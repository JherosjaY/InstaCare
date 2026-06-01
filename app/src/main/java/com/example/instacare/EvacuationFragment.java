package com.example.instacare;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.data.local.AppDatabase;
import com.example.instacare.data.local.EvacuationCenter;
import com.example.instacare.utils.DistanceUtils;
import com.google.android.material.chip.ChipGroup;
import org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * EvacuationFragment — Tier 1 + Tier 2 Features
 * Dedicated standalone evacuation screen.
 * Tier 1: Status badge, capacity/contact popup, disaster filter chips.
 * Tier 2 Feature A+B: Opens CenterDetailBottomSheet with resources + check-in on card tap.
 * Tier 2 Feature D: Top-3 nearest Open centers shown with color-coded routes on map.
 */
public class EvacuationFragment extends Fragment {

    private RecyclerView recyclerView;
    private View loadingState, emptyState;
    private MapView mapView;
    private GeoPoint userLocation;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private Polyline currentRoute;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private List<EvacuationCenter> allCenters = new ArrayList<>();
    private String activeDisasterFilter = "all"; // "all","flood","earthquake","fire","typhoon"

    private ActivityResultLauncher<String[]> locationPermissionRequest;

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        locationPermissionRequest = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                Boolean fine = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                if (Boolean.TRUE.equals(fine)) startLocationTracking();
            }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        org.osmdroid.config.Configuration.getInstance()
            .setUserAgentValue(requireContext().getPackageName());
        return inflater.inflate(R.layout.fragment_evacuation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.evacRecyclerView);
        loadingState = view.findViewById(R.id.evacLoadingState);
        emptyState   = view.findViewById(R.id.evacEmptyState);
        mapView      = view.findViewById(R.id.evacMapView);

        // Edge-to-edge top bar padding
        View topBar = view.findViewById(R.id.evacTopBar);
        if (topBar != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, wi) -> {
                androidx.core.graphics.Insets ins = wi.getInsets(
                    androidx.core.view.WindowInsetsCompat.Type.systemBars());
                int p = (int)(20 * getResources().getDisplayMetrics().density);
                v.setPadding(p, ins.top + p, p, p);
                return wi;
            });
        }

        // Colored title "Evacuation Centers"
        TextView tvTitle = view.findViewById(R.id.tvTitleEvacuation);
        if (tvTitle != null) {
            android.text.SpannableStringBuilder sb = new android.text.SpannableStringBuilder("Evacuation Centers");
            sb.setSpan(new android.text.style.ForegroundColorSpan(
                ContextCompat.getColor(requireContext(), R.color.warning_orange)),
                0, "Evacuation".length(),
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            tvTitle.setText(sb);
        }

        setupMap();
        setupRecyclerView();
        setupDisasterFilterChips(view);

        // Refresh button
        view.findViewById(R.id.evacRefreshCard).setOnClickListener(v -> {
            v.animate().rotationBy(360f).setDuration(500).start();
            showLoading(true);
            loadEvacuationData(true);
        });

        // Compass
        view.findViewById(R.id.evacNavFabContainer).setOnClickListener(v ->
            toggleCompass(view));
        view.findViewById(R.id.evacCompassOverlay).setOnClickListener(v ->
            toggleCompass(view));
        view.findViewById(R.id.evacCompassDial).setOnClickListener(v -> {
            if (mapView != null) mapView.setMapOrientation(0f);
        });

        locationManager = (LocationManager) requireContext()
            .getSystemService(Context.LOCATION_SERVICE);
        checkAndRequestLocation();

        showLoading(true);
        loadEvacuationData(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) mapView.onPause();
        stopLocationTracking();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdownNow();
    }

    // ── Setup ────────────────────────────────────────────────────────────────

    private void setupMap() {
        if (mapView == null) return;
        mapView.setTileSource(org.osmdroid.tileprovider.tilesource.TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(14.0);
        mapView.getController().setCenter(new GeoPoint(12.8797, 121.7740));
    }

    private void setupRecyclerView() {
        EvacuationAdapter adapter = new EvacuationAdapter(new ArrayList<>(), this::onCenterClicked);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    /**
     * Feature 4 — Disaster Type Filter Chips
     * Filters the already-loaded list client-side. No new network call needed.
     */
    private void setupDisasterFilterChips(View view) {
        ChipGroup chipGroup = view.findViewById(R.id.chipGroupDisasterType);
        if (chipGroup == null) return;

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if      (id == R.id.chipDisasterAll)        activeDisasterFilter = "all";
            else if (id == R.id.chipDisasterFlood)      activeDisasterFilter = "flood";
            else if (id == R.id.chipDisasterEarthquake) activeDisasterFilter = "earthquake";
            else if (id == R.id.chipDisasterFire)       activeDisasterFilter = "fire";
            else if (id == R.id.chipDisasterTyphoon)    activeDisasterFilter = "typhoon";
            applyDisasterFilter();
        });
    }

    /**
     * Filters `allCenters` by `activeDisasterFilter` and refreshes the adapter + map.
     * Feature 4 — no re-fetch, instant client-side filter.
     */
    private void applyDisasterFilter() {
        if (!isAdded()) return;
        List<EvacuationCenter> filtered = new ArrayList<>();
        for (EvacuationCenter c : allCenters) {
            if ("all".equals(activeDisasterFilter)) {
                filtered.add(c);
            } else if (c.disasterTypes != null &&
                       c.disasterTypes.toLowerCase().contains(activeDisasterFilter)) {
                filtered.add(c);
            }
        }
        updateUI(filtered);

        // Update empty message to reflect filter context
        View empView = getView() != null ? getView().findViewById(R.id.evacEmptyState) : null;
        TextView empMsg = empView != null ? empView.findViewById(R.id.tvEvacEmptyMessage) : null;
        if (empMsg != null) {
            empMsg.setText("all".equals(activeDisasterFilter)
                ? "No evacuation centers found nearby."
                : "No centers tagged for " + activeDisasterFilter + " disasters.");
        }
    }

    // ── Data Loading ─────────────────────────────────────────────────────────

    private void loadEvacuationData(boolean forceRefresh) {
        if (!isAdded()) return;
        AppDatabase db = AppDatabase.getDatabase(requireContext());

        executor.execute(() -> {
            List<EvacuationCenter> local = db.evacuationCenterDao().getActiveCenters();
            if (local == null) local = new ArrayList<>();

            boolean hasApi = false;
            for (EvacuationCenter c : local) {
                if ("api".equals(c.source)) { hasApi = true; break; }
            }

            if (userLocation != null) {
                for (EvacuationCenter c : local) {
                    GeoPoint pt = new GeoPoint(c.latitude, c.longitude);
                    c.distance = DistanceUtils.formatDistance(
                        userLocation.distanceToAsDouble(pt) / 1000.0);
                }
                sortByDistance(local);
            }

            final List<EvacuationCenter> merged = new ArrayList<>(local);

            if (!forceRefresh && hasApi) {
                postDisplay(merged);
            } else if (userLocation != null) {
                List<EvacuationCenter> localOnly = new ArrayList<>();
                for (EvacuationCenter c : local) {
                    if (!"api".equals(c.source)) localOnly.add(c);
                }
                fetchFromOverpass(userLocation, localOnly);
            } else {
                postDisplay(merged);
            }
        });
    }

    private void fetchFromOverpass(GeoPoint loc, List<EvacuationCenter> existingLocal) {
        try {
            String q = "[out:json];("
                + "nwr[\"amenity\"=\"shelter\"](around:50000," + loc.getLatitude() + "," + loc.getLongitude() + ");"
                + "nwr[\"social_facility\"=\"shelter\"](around:50000," + loc.getLatitude() + "," + loc.getLongitude() + ");"
                + "nwr[\"emergency\"=\"assembly_point\"](around:50000," + loc.getLatitude() + "," + loc.getLongitude() + ");"
                + "nwr[\"amenity\"=\"community_centre\"](around:50000," + loc.getLatitude() + "," + loc.getLongitude() + ");"
                + ");out center;";
            URL url = new URL("https://overpass-api.de/api/interpreter?data=" + URLEncoder.encode(q, "UTF-8"));
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                StringBuilder sb = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONArray elements = new JSONObject(sb.toString()).getJSONArray("elements");
                List<EvacuationCenter> fetched = new ArrayList<>();
                for (int i = 0; i < elements.length(); i++) {
                    JSONObject el = elements.getJSONObject(i);
                    double lat = 0, lon = 0;
                    if (el.has("center")) {
                        lat = el.getJSONObject("center").getDouble("lat");
                        lon = el.getJSONObject("center").getDouble("lon");
                    } else if (el.has("lat") && el.has("lon")) {
                        lat = el.getDouble("lat");
                        lon = el.getDouble("lon");
                    } else continue;

                    String name = "Evacuation Center", addr = "", cType = "Shelter";
                    if (el.has("tags")) {
                        JSONObject tags = el.getJSONObject("tags");
                        if (tags.has("name"))           name = tags.getString("name");
                        else if (tags.has("operator"))  name = tags.getString("operator") + " Center";
                        if (tags.has("addr:street"))    addr = tags.optString("addr:street", "");
                        if (tags.has("amenity"))        cType = capitalize(tags.getString("amenity"));
                    }

                    GeoPoint pt = new GeoPoint(lat, lon);
                    boolean dup = false;
                    for (EvacuationCenter lc : existingLocal) {
                        if (pt.distanceToAsDouble(new GeoPoint(lc.latitude, lc.longitude)) < 100) {
                            dup = true; break;
                        }
                    }
                    if (dup) continue;

                    EvacuationCenter c = new EvacuationCenter(
                        "api_" + i, name, addr, lat, lon, cType, "Open");
                    c.distance = DistanceUtils.formatDistance(
                        loc.distanceToAsDouble(pt) / 1000.0);
                    c.source = "api";
                    fetched.add(c);
                }

                AppDatabase db = AppDatabase.getDatabase(requireContext());
                db.evacuationCenterDao().deleteApiCenters();
                db.evacuationCenterDao().insertAll(fetched);

                List<EvacuationCenter> all = new ArrayList<>(existingLocal);
                all.addAll(fetched);
                sortByDistance(all);
                postDisplay(all);
            } else {
                postDisplay(existingLocal);
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (isAdded()) postDisplay(existingLocal);
        }
    }

    private void postDisplay(List<EvacuationCenter> centers) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            allCenters = centers;
            applyDisasterFilter(); // apply any active filter before showing
            showLoading(false);
        });
    }

    private void updateUI(List<EvacuationCenter> centers) {
        if (!isAdded()) return;
        if (recyclerView.getAdapter() instanceof EvacuationAdapter) {
            ((EvacuationAdapter) recyclerView.getAdapter()).setCenters(centers);
        }
        updateMapMarkers(centers);
        boolean empty = centers == null || centers.isEmpty();
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    // ── Map ──────────────────────────────────────────────────────────────────

    private void updateMapMarkers(List<EvacuationCenter> centers) {
        if (mapView == null || !isAdded()) return;
        mapView.getOverlays().clear();

        RadiusMarkerClusterer clusterer = new RadiusMarkerClusterer(requireContext());

        GeoPoint start = userLocation != null ? userLocation : new GeoPoint(12.8797, 121.7740);
        addUserDot(start);

        if (centers != null) {
            for (EvacuationCenter c : centers) {
                if (c == null) continue;
                addEvacMarker(new GeoPoint(c.latitude, c.longitude), c, clusterer);
            }
        }

        mapView.getOverlays().add(clusterer);
        if (currentRoute != null) mapView.getOverlays().add(0, currentRoute);

        // Feature D — draw top-3 nearest open centers as color-coded route previews
        if (userLocation != null && centers != null && centers.size() > 1) {
            drawTop3Routes(centers);
        }

        org.osmdroid.views.overlay.MapEventsOverlay eventsOverlay =
            new org.osmdroid.views.overlay.MapEventsOverlay(
                new org.osmdroid.events.MapEventsReceiver() {
                    @Override public boolean singleTapConfirmedHelper(GeoPoint p) { return true; }
                    @Override public boolean longPressHelper(GeoPoint p) { return false; }
                });
        mapView.getOverlays().add(0, eventsOverlay);
        mapView.invalidate();
    }

    private void addUserDot(GeoPoint point) {
        Marker m = new Marker(mapView);
        m.setPosition(point);
        m.setTitle("You");
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
        try {
            m.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.user_location_dot));
        } catch (Exception ignored) {}
        mapView.getOverlays().add(m);
    }

    private void addEvacMarker(GeoPoint point, EvacuationCenter center,
                                RadiusMarkerClusterer clusterer) {
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setTitle(center.name);
        marker.setSnippet(center.type != null ? center.type : "Shelter");
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        try {
            marker.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_marker_evac));
        } catch (Exception ignored) {}

        // Feature 3 — Capacity & Contact in info window
        org.osmdroid.views.overlay.infowindow.MarkerInfoWindow infoWindow =
            new org.osmdroid.views.overlay.infowindow.MarkerInfoWindow(
                R.layout.info_window_evac, mapView) {
                @Override
                public void onOpen(Object item) {
                    View v = getView();
                    if (v == null) return;
                    v.setVisibility(View.VISIBLE);

                    TextView tvName    = v.findViewById(R.id.evacInfoWindowName);
                    TextView tvType    = v.findViewById(R.id.evacInfoWindowType);
                    TextView tvStatus  = v.findViewById(R.id.evacInfoWindowStatus);
                    TextView tvCap     = v.findViewById(R.id.evacInfoWindowCapacity);
                    View     contactRow= v.findViewById(R.id.evacInfoContactRow);
                    TextView tvContact = v.findViewById(R.id.evacInfoWindowContact);
                    com.google.android.material.button.MaterialButton btnRoute =
                        v.findViewById(R.id.evacInfoWindowRouteBtn);

                    if (tvName != null)   tvName.setText(center.name);
                    if (tvType != null) {
                        String typeAddr = center.type != null ? center.type : "Shelter";
                        if (center.address != null && !center.address.isEmpty())
                            typeAddr += " · " + center.address;
                        tvType.setText(typeAddr);
                    }

                    // Status badge in popup
                    if (tvStatus != null) {
                        String s = center.status != null ? center.status : "Open";
                        tvStatus.setText("● " + s);
                        int col;
                        switch (s.toLowerCase()) {
                            case "full":   col = 0xFFEF4444; break;
                            case "closed": col = 0xFF6B7280; break;
                            default:       col = 0xFF10B981; break;
                        }
                        tvStatus.getBackground().setTint(col);
                    }

                    // Feature 3 — Capacity
                    if (tvCap != null) {
                        tvCap.setText(center.capacity > 0
                            ? "Cap: " + center.capacity : "Cap: —");
                    }

                    // Feature 3 — Contact
                    if (contactRow != null && tvContact != null) {
                        if (center.contact != null && !center.contact.isEmpty()) {
                            contactRow.setVisibility(View.VISIBLE);
                            tvContact.setText(center.contact);
                        } else {
                            contactRow.setVisibility(View.GONE);
                        }
                    }

                    if (btnRoute != null) {
                        btnRoute.setOnClickListener(bv -> {
                            drawRouteTo(center);
                            close();
                        });
                    }
                }
            };
        marker.setInfoWindow(infoWindow);
        clusterer.add(marker);
    }

    private void drawRouteTo(EvacuationCenter center) {
        if (userLocation == null || !isAdded()) return;
        GeoPoint dest = new GeoPoint(center.latitude, center.longitude);
        mapView.getController().animateTo(dest);
        mapView.getController().setZoom(15.0);

        executor.execute(() -> {
            try {
                String url = "https://router.project-osrm.org/route/v1/driving/"
                    + userLocation.getLongitude() + "," + userLocation.getLatitude() + ";"
                    + dest.getLongitude() + "," + dest.getLatitude()
                    + "?overview=full&geometries=geojson";
                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                conn.setConnectTimeout(8000); conn.setReadTimeout(8000);
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    StringBuilder sb = new StringBuilder();
                    BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                    r.close();
                    JSONArray coords = new JSONObject(sb.toString())
                        .getJSONArray("routes").getJSONObject(0)
                        .getJSONObject("geometry").getJSONArray("coordinates");

                    List<GeoPoint> pts = new ArrayList<>();
                    for (int i = 0; i < coords.length(); i++) {
                        JSONArray pt = coords.getJSONArray(i);
                        pts.add(new GeoPoint(pt.getDouble(1), pt.getDouble(0)));
                    }
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (currentRoute != null) mapView.getOverlays().remove(currentRoute);
                            currentRoute = new Polyline();
                            currentRoute.setPoints(pts);
                            currentRoute.getOutlinePaint().setColor(0xFFFF6D00);
                            currentRoute.getOutlinePaint().setStrokeWidth(10f);
                            mapView.getOverlays().add(0, currentRoute);
                            mapView.invalidate();
                        });
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    // ── Location ─────────────────────────────────────────────────────────────

    private void checkAndRequestLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationTracking();
        } else {
            locationPermissionRequest.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void startLocationTracking() {
        if (!isAdded()) return;
        try {
            locationListener = loc -> {
                userLocation = new GeoPoint(loc.getLatitude(), loc.getLongitude());
                mapView.getController().animateTo(userLocation);
                mapView.getController().setZoom(14.0);
                loadEvacuationData(false);
            };
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 5000, 10, locationListener);
        } catch (SecurityException ignored) {}
    }

    private void stopLocationTracking() {
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Feature A + B: Open CenterDetailBottomSheet showing resources, live count, and check-in.
     * Feature D: Also pan map to center.
     */
    private void onCenterClicked(EvacuationCenter center) {
        // Pan map to tapped center
        if (mapView != null) {
            GeoPoint pt = new GeoPoint(center.latitude, center.longitude);
            mapView.getController().animateTo(pt);
            mapView.getController().setZoom(16.0);
        }
        // Open detail bottom sheet (Feature A + B)
        if (isAdded() && getParentFragmentManager() != null) {
            CenterDetailBottomSheet sheet = CenterDetailBottomSheet.newInstance(center);
            sheet.show(getParentFragmentManager(), "CenterDetail");
        }
    }

    private void showLoading(boolean show) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            if (loadingState != null) loadingState.setVisibility(show ? View.VISIBLE : View.GONE);
            if (show) {
                recyclerView.setVisibility(View.GONE);
                emptyState.setVisibility(View.GONE);
            }
        });
    }

    private void toggleCompass(View root) {
        View overlay = root.findViewById(R.id.evacCompassOverlay);
        if (overlay != null) {
            overlay.setVisibility(overlay.getVisibility() == View.VISIBLE
                ? View.GONE : View.VISIBLE);
        }
    }

    private void sortByDistance(List<EvacuationCenter> list) {
        if (userLocation == null) return;
        Collections.sort(list, (a, b) -> Double.compare(
            userLocation.distanceToAsDouble(new GeoPoint(a.latitude, a.longitude)),
            userLocation.distanceToAsDouble(new GeoPoint(b.latitude, b.longitude))));
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    /**
     * Feature D — Multi-stop Route Planner
     * Shows straight-line bearing lines from userLocation to the top-3 nearest "Open" centers.
     * Color-coded: 1st=Gold, 2nd=Teal, 3rd=Purple. Fetches actual road routes asynchronously.
     */
    private void drawTop3Routes(List<EvacuationCenter> centers) {
        if (userLocation == null || mapView == null || !isAdded()) return;

        // Pick top-3 nearest OPEN centers
        int[] routeColors = {0xFFFFD700, 0xFF00BCD4, 0xFF9C27B0}; // gold, teal, purple
        int count = 0;
        for (EvacuationCenter c : centers) {
            if (count >= 3) break;
            if ("closed".equalsIgnoreCase(c.status)) continue;
            final int colorIndex = count;
            final EvacuationCenter ec = c;
            count++;

            executor.execute(() -> {
                try {
                    GeoPoint dest = new GeoPoint(ec.latitude, ec.longitude);
                    String url = "https://router.project-osrm.org/route/v1/driving/"
                        + userLocation.getLongitude() + "," + userLocation.getLatitude() + ";"
                        + dest.getLongitude() + "," + dest.getLatitude()
                        + "?overview=simplified&geometries=geojson";
                    HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                    conn.setConnectTimeout(6000);
                    conn.setReadTimeout(6000);

                    if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        StringBuilder sb = new StringBuilder();
                        BufferedReader r = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String line;
                        while ((line = r.readLine()) != null) sb.append(line);
                        r.close();

                        JSONArray coords = new JSONObject(sb.toString())
                            .getJSONArray("routes").getJSONObject(0)
                            .getJSONObject("geometry").getJSONArray("coordinates");

                        List<GeoPoint> pts = new ArrayList<>();
                        for (int i = 0; i < coords.length(); i++) {
                            JSONArray pt = coords.getJSONArray(i);
                            pts.add(new GeoPoint(pt.getDouble(1), pt.getDouble(0)));
                        }

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (!isAdded() || mapView == null) return;
                                Polyline route = new Polyline();
                                route.setPoints(pts);
                                route.getOutlinePaint().setColor(routeColors[colorIndex]);
                                route.getOutlinePaint().setStrokeWidth(6f);
                                route.getOutlinePaint().setAlpha(180);
                                // Dashed effect for non-primary routes
                                if (colorIndex > 0) {
                                    route.getOutlinePaint().setPathEffect(
                                        new android.graphics.DashPathEffect(
                                            new float[]{20, 10}, 0));
                                }
                                mapView.getOverlays().add(0, route);
                                mapView.invalidate();
                            });
                        }
                    }
                } catch (Exception e) { /* silent fail — route preview is optional */ }
            });
        }
    }
}
