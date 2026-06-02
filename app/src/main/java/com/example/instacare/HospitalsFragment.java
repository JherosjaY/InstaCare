package com.example.instacare;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.databinding.FragmentHospitalsBinding; // Removed
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Looper;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import org.osmdroid.views.overlay.Polyline;
import java.util.Collections;
import java.util.Comparator;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.ArrayList;
import java.util.List;

public class HospitalsFragment extends Fragment implements SensorEventListener {

    private boolean isImmersiveMode = false;
    private boolean isRouteOnly = false;

    // Views
    private RecyclerView hospitalsRecyclerView;
    private MaterialCardView bottomSheet;
    private FrameLayout mapContainer;
    private org.osmdroid.views.MapView mapView; // Real Map
    private ConstraintLayout topBar;
    private MaterialCardView navFabContainer;
    private ImageView navFabIcon;
    private View loadingState;
    
    // Detailed Compass Views
    private View compassOverlay;
    private View compassDial;
    
    // Compass Sensors
    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private final float[] rotationMatrix = new float[9];
    private final float[] orientationValues = new float[3];
    private float currentAzimuth = 0f;
    
    // Navigation & Routing
    private android.app.AlertDialog gpsDialog;
    private LocationManager locationManager;
    private org.osmdroid.util.GeoPoint currentUserLocation;
    private Polyline currentRoute;
    private List<com.example.instacare.data.local.Hospital> currentHospitals = new ArrayList<>();
    private List<com.example.instacare.data.local.EvacuationCenter> currentEvacCenters = new ArrayList<>();
    private String currentMode = "hospitals"; // "hospitals" or "evacuation"
    private String autoRouteTarget = null;
    private ActivityResultLauncher<String[]> locationPermissionRequest;
    private LocationListener locationListener;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // Info Window (Removed static members, now dynamic)

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        locationPermissionRequest = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
                if (fineLocationGranted != null && fineLocationGranted) {
                    startLocationTracking();
                }
            }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Initialize Osmdroid Configuration
        org.osmdroid.config.Configuration.getInstance().setUserAgentValue(requireContext().getPackageName());
        return inflater.inflate(R.layout.fragment_hospitals, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        if (getArguments() != null) {
            autoRouteTarget = getArguments().getString("TARGET_HOSPITAL_NAME");
            isRouteOnly = getArguments().getBoolean("IS_ROUTE_ONLY", false);
        }
        
        hospitalsRecyclerView = view.findViewById(R.id.hospitalsRecyclerView);
        bottomSheet = view.findViewById(R.id.bottomSheet);
        mapContainer = view.findViewById(R.id.mapContainer);
        mapView = view.findViewById(R.id.mapView);
        topBar = view.findViewById(R.id.topBar);
        
        if (isRouteOnly) {
            if (topBar != null) topBar.setVisibility(View.GONE);
            if (bottomSheet != null) bottomSheet.setVisibility(View.GONE);
        }
        
        navFabContainer = view.findViewById(R.id.navFabContainer);
        navFabIcon = view.findViewById(R.id.navFabIcon);
        compassOverlay = view.findViewById(R.id.compassOverlay);
        compassDial = view.findViewById(R.id.compassDial);
        loadingState = view.findViewById(R.id.loadingStateHospitals);
        FrameLayout refreshCard = view.findViewById(R.id.refreshCard);
        
        // Edge-to-Edge inset handling for the top bar
        if (topBar != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, windowInsets) -> {
                androidx.core.graphics.Insets insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
                int padding20 = (int)(20 * getResources().getDisplayMetrics().density);
                v.setPadding(padding20, insets.top + padding20, padding20, padding20);
                return windowInsets;
            });
        }
        
        setupRecyclerView();
        setupMap();
        setupImmersiveMode();
        setupCompassSensors();
        
        // 🔙 Intercept System Back Button for Map Immersive Mode
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (isImmersiveMode) {
                    // Restore UI from full-screen map mode
                    toggleImmersiveMode();
                } else {
                    // Normal back button behavior (return to Dashboard)
                    setEnabled(false);
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                    // Re-enable so it works again if they reopen the section
                    setEnabled(true);
                }
            }
        });
        
        // Dynamic Header Accent
        TextView tvTitle = view.findViewById(R.id.tvTitleHospitals);
        if (tvTitle != null) {
            String fullText = "Nearby Facilities";
            android.text.SpannableStringBuilder spannable = new android.text.SpannableStringBuilder(fullText);
            int start = fullText.indexOf("Facilities");
            if (start != -1) {
                int color = androidx.core.content.ContextCompat.getColor(view.getContext(), R.color.emergency_red);
                spannable.setSpan(
                    new android.text.style.ForegroundColorSpan(color),
                    start, 
                    start + "Facilities".length(), 
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
            tvTitle.setText(spannable);
        }

        // Chip filter listener
        com.google.android.material.chip.ChipGroup chipGroup = view.findViewById(R.id.chipGroupFilter);
        final View disasterScroll = view.findViewById(R.id.scrollDisasterFilters);
        com.google.android.material.chip.ChipGroup disasterGroup = view.findViewById(R.id.chipGroupDisasterFilter);

        if (chipGroup != null) {
            chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.contains(R.id.chipHospitals)) {
                    currentMode = "hospitals";
                    if (disasterScroll != null) disasterScroll.setVisibility(View.GONE);
                    showLoading(true);
                    hospitalsRecyclerView.setVisibility(View.VISIBLE);
                    setupRecyclerView();
                    loadHospitalData(false);
                } else if (checkedIds.contains(R.id.chipEvacuation)) {
                    currentMode = "evacuation";
                    if (disasterScroll != null) disasterScroll.setVisibility(View.VISIBLE);
                    showLoading(true);
                    setupEvacuationRecyclerView();
                    loadEvacuationData(false);
                }
            });
        }

        if (disasterGroup != null) {
            disasterGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
                String disaster = "all";
                if (checkedIds.contains(R.id.chipDisasterFlood)) disaster = "flood";
                else if (checkedIds.contains(R.id.chipDisasterEarthquake)) disaster = "earthquake";
                else if (checkedIds.contains(R.id.chipDisasterFire)) disaster = "fire";
                else if (checkedIds.contains(R.id.chipDisasterTyphoon)) disaster = "typhoon";
                else if (checkedIds.contains(R.id.chipDisasterVolcano)) disaster = "volcano";
                
                filterEvacuationByDisaster(disaster);
            });
        }
        
        if (isRouteOnly) {
            if (navFabContainer != null) navFabContainer.setVisibility(View.GONE);
        }
        
        locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        checkAndRequestLocationPermission();
        
        navFabContainer.setOnClickListener(v -> {
            toggleDetailedCompass();
        });
        
        compassOverlay.setOnClickListener(v -> {
            toggleDetailedCompass();
        });

        // Add resetting map rotation on dial click
        compassDial.setOnClickListener(v -> {
            if (mapView != null) {
                mapView.setMapOrientation(0f);
            }
        });
        
        refreshCard.setOnClickListener(v -> {
            // Animate refresh icon rotation
            v.animate().rotationBy(360f).setDuration(500).start();
            showLoading(true);
            
            if (currentUserLocation != null) {
                if ("evacuation".equals(currentMode)) {
                    loadEvacuationData(true);
                } else {
                    loadHospitalData(true);
                }
                if (mapView != null) {
                    mapView.getController().animateTo(currentUserLocation);
                    mapView.getController().setZoom(15.0);
                }
            }
        });
        
        // 🚀 Instant Persistence: Load cache immediately on launch
        loadHospitalData(false);
        loadEvacuationData(false);

        // HANDLE REDIRECT: Auto-switch to Evacuation tab if requested
        if (getArguments() != null) {
            if (getArguments().getBoolean("SELECT_EVACUATION", false) || getArguments().getBoolean("SELECT_CHECKIN", false)) {
                com.google.android.material.chip.Chip chipEvac = view.findViewById(R.id.chipEvacuation);
                if (chipEvac != null) chipEvac.setChecked(true);
            }
        }
    }

    private void fetchHospitalsViaOverpassAPI(org.osmdroid.util.GeoPoint location) {
        if (location == null) return;
        
        executor.execute(() -> {
            try {
                // Construct Overpass QL query: Fetch NWR (nodes, ways, relations) for hospitals within 50km
                String query = "[out:json];(nwr[\"amenity\"=\"hospital\"](around:50000," + location.getLatitude() + "," + location.getLongitude() + ");nwr[\"healthcare\"=\"hospital\"](around:50000," + location.getLatitude() + "," + location.getLongitude() + "););out center;";
                String urlString = "https://overpass-api.de/api/interpreter?data=" + URLEncoder.encode(query, "UTF-8");
                
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                
                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    JSONObject json = new JSONObject(response.toString());
                    JSONArray elements = json.getJSONArray("elements");
                    
                    List<com.example.instacare.data.local.Hospital> fetchedHospitals = new ArrayList<>();
                    
                    for (int i = 0; i < elements.length(); i++) {
                        JSONObject element = elements.getJSONObject(i);
                        
                        double lat = 0;
                        double lon = 0;
                        if (element.has("center")) {
                            JSONObject center = element.getJSONObject("center");
                            lat = center.getDouble("lat");
                            lon = center.getDouble("lon");
                        } else if (element.has("lat") && element.has("lon")) {
                            lat = element.getDouble("lat");
                            lon = element.getDouble("lon");
                        } else {
                            continue; // skip if no coords
                        }
                        
                        String name = "General Hospital";
                        String addr = "Local Hospital";
                        String phone = "N/A";
                        if (element.has("tags")) {
                            JSONObject tags = element.getJSONObject("tags");
                            
                            // Extensive fallback for real name
                            if (tags.has("name")) {
                                name = tags.getString("name");
                            } else if (tags.has("official_name")) {
                                name = tags.getString("official_name");
                            } else if (tags.has("alt_name")) {
                                name = tags.getString("alt_name");
                            } else if (tags.has("short_name")) {
                                name = tags.getString("short_name");
                            } else if (tags.has("operator")) {
                                name = tags.getString("operator") + " Hospital";
                            } else if (tags.has("brand")) {
                                String brand = tags.getString("brand");
                                name = brand.toLowerCase().contains("hospital") ? brand : brand + " Hospital";
                            }
                            
                            // Address parsing
                            if (tags.has("addr:street")) {
                                addr = tags.getString("addr:street");
                                if (tags.has("addr:housenumber")) {
                                    addr = tags.getString("addr:housenumber") + " " + addr;
                                }
                                if (tags.has("addr:city")) {
                                    addr += ", " + tags.getString("addr:city");
                                }
                            }
                            
                            // Phone parsing
                            if (tags.has("phone")) {
                                phone = tags.getString("phone");
                            } else if (tags.has("contact:phone")) {
                                phone = tags.getString("contact:phone");
                            }
                        }
                        
                        org.osmdroid.util.GeoPoint hospLoc = new org.osmdroid.util.GeoPoint(lat, lon);
                        double distanceKm = location.distanceToAsDouble(hospLoc) / 1000.0;
                        String distStr = com.example.instacare.utils.DistanceUtils.formatDistance(distanceKm);

                        // Create Hospital object locally dynamically using the required constructor
                        com.example.instacare.data.local.Hospital h = new com.example.instacare.data.local.Hospital(
                            String.valueOf(i + 1), // id
                            name,                  // name
                            distStr,               // distance
                            addr,                  // address
                            phone,                 // phone
                            true,                  // isOpen
                            "Hospital",            // type
                            "Emergency",           // servicesJson
                            lat,                   // latitude
                            lon,                   // longitude
                            "hospital_temp_bg",    // imageUrl
                            "Available"            // capacityStatus
                        );
                        
                        fetchedHospitals.add(h);
                    }
                    
                    // Sort nearest to farthest
                    Collections.sort(fetchedHospitals, new Comparator<com.example.instacare.data.local.Hospital>() {
                        @Override
                        public int compare(com.example.instacare.data.local.Hospital h1, com.example.instacare.data.local.Hospital h2) {
                            org.osmdroid.util.GeoPoint p1 = new org.osmdroid.util.GeoPoint(h1.latitude, h1.longitude);
                            org.osmdroid.util.GeoPoint p2 = new org.osmdroid.util.GeoPoint(h2.latitude, h2.longitude);
                            return Double.compare(location.distanceToAsDouble(p1), location.distanceToAsDouble(p2));
                        }
                    });
                    
                    // Save to Room DB cache
                    com.example.instacare.data.local.AppDatabase db = com.example.instacare.data.local.AppDatabase.getDatabase(requireContext());
                    db.hospitalDao().deleteAll();
                    db.hospitalDao().insertAll(fetchedHospitals);
                    
                    // Update UI on Main Thread
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> displayHospitalResults(fetchedHospitals));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void loadHospitalData(boolean forceRefresh) {
        if (!isAdded()) return;
        com.example.instacare.data.local.AppDatabase db = com.example.instacare.data.local.AppDatabase.getDatabase(requireContext());
        
        executor.execute(() -> {
            List<com.example.instacare.data.local.Hospital> cached = db.hospitalDao().getAllHospitalsDirect();
            if (!forceRefresh && cached != null && !cached.isEmpty()) {
                if (currentUserLocation != null) {
                    for(com.example.instacare.data.local.Hospital h : cached) {
                        org.osmdroid.util.GeoPoint p = new org.osmdroid.util.GeoPoint(h.latitude, h.longitude);
                        double d = currentUserLocation.distanceToAsDouble(p) / 1000.0;
                        h.distance = com.example.instacare.utils.DistanceUtils.formatDistance(d);
                    }
                    Collections.sort(cached, (a, b) -> {
                        org.osmdroid.util.GeoPoint pa = new org.osmdroid.util.GeoPoint(a.latitude, a.longitude);
                        org.osmdroid.util.GeoPoint pb = new org.osmdroid.util.GeoPoint(b.latitude, b.longitude);
                        return Double.compare(currentUserLocation.distanceToAsDouble(pa), currentUserLocation.distanceToAsDouble(pb));
                    });
                }
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Purge any existing dummy data if detected
                        boolean hasDummy = false;
                        java.util.Iterator<com.example.instacare.data.local.Hospital> iterator = cached.iterator();
                        while(iterator.hasNext()){
                            com.example.instacare.data.local.Hospital h = iterator.next();
                            if(h.id.startsWith("v") || h.id.startsWith("m") || h.id.startsWith("c") || 
                               h.id.startsWith("d") || h.id.startsWith("b") || h.id.startsWith("z")){
                                hasDummy = true;
                                iterator.remove();
                                db.hospitalDao().delete(h);
                            }
                        }
                        displayHospitalResults(cached);
                    });
                }
            } else {
                if (currentUserLocation != null) {
                    fetchHospitalsViaOverpassAPI(currentUserLocation);
                } else if (!forceRefresh && cached != null && !cached.isEmpty()) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> displayHospitalResults(cached));
                    }
                }
            }
        });
    }

    private void displayHospitalResults(List<com.example.instacare.data.local.Hospital> hospitals) {
        if (!isAdded()) return;
        currentHospitals = hospitals;
        if (hospitalsRecyclerView.getAdapter() instanceof HospitalAdapter) {
            ((HospitalAdapter) hospitalsRecyclerView.getAdapter()).setHospitals(currentHospitals);
        }
        if ("hospitals".equals(currentMode)) {
            updateMapMarkers(currentHospitals);
        }
        
        if (autoRouteTarget != null && currentHospitals != null) {
            for (com.example.instacare.data.local.Hospital h : currentHospitals) {
                if (h.name != null && h.name.equalsIgnoreCase(autoRouteTarget)) {
                    drawRouteToHospital(h);
                    mapView.getController().animateTo(new org.osmdroid.util.GeoPoint(h.latitude, h.longitude));
                    mapView.getController().setZoom(16.0);
                    for (org.osmdroid.views.overlay.Overlay overlay : mapView.getOverlays()) {
                        if (overlay instanceof org.osmdroid.views.overlay.Marker) {
                            org.osmdroid.views.overlay.Marker m = (org.osmdroid.views.overlay.Marker) overlay;
                            if (autoRouteTarget.equalsIgnoreCase(m.getTitle())) {
                                m.showInfoWindow();
                                break;
                            }
                        }
                    }
                    if (!isRouteOnly) {
                        autoRouteTarget = null;
                    }
                    break;
                }
            }
        }
        
        View emptyState = getView() != null ? getView().findViewById(R.id.emptyStateHospitals) : null;
        if (currentHospitals == null || currentHospitals.isEmpty()) {
            if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
            hospitalsRecyclerView.setVisibility(View.GONE);
        } else {
            if (emptyState != null) emptyState.setVisibility(View.GONE);
            hospitalsRecyclerView.setVisibility(View.VISIBLE);
        }
        showLoading(false);
    }

    private void showLoading(boolean show) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            if (loadingState != null) {
                loadingState.setVisibility(show ? View.VISIBLE : View.GONE);
            }
            if (show) {
                hospitalsRecyclerView.setVisibility(View.GONE);
                View empty = getView() != null ? getView().findViewById(R.id.emptyStateHospitals) : null;
                if (empty != null) empty.setVisibility(View.GONE);
            }
        });
    }

    private void setupRecyclerView() {
        HospitalAdapter adapter = new HospitalAdapter(new ArrayList<>(), new HospitalAdapter.OnHospitalClickListener() {
            @Override
            public void onHospitalClick(com.example.instacare.data.local.Hospital hospital) {
                redirectMapToHospital(hospital);
            }
        });
        hospitalsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        hospitalsRecyclerView.setAdapter(adapter);

        // We no longer query the local DB. The UI is populated when fetchHospitalsViaOverpassAPI completes.
        View emptyState = getView() != null ? getView().findViewById(R.id.emptyStateHospitals) : null;
        if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
        hospitalsRecyclerView.setVisibility(View.GONE);
    }

    private void setupEvacuationRecyclerView() {
        EvacuationAdapter adapter = new EvacuationAdapter(new ArrayList<>(), center -> {
            redirectMapToEvacuation(center);
        });
        hospitalsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        hospitalsRecyclerView.setAdapter(adapter);

        View emptyState = getView() != null ? getView().findViewById(R.id.emptyStateHospitals) : null;
        if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
        hospitalsRecyclerView.setVisibility(View.GONE);
    }

    private void loadEvacuationData(boolean forceRefresh) {
        if (!isAdded()) return;
        com.example.instacare.data.local.AppDatabase db = com.example.instacare.data.local.AppDatabase.getDatabase(requireContext());

        executor.execute(() -> {
            List<com.example.instacare.data.local.EvacuationCenter> localCenters = db.evacuationCenterDao().getActiveCenters();
            if (localCenters == null) localCenters = new ArrayList<>();

            boolean hasApiCache = false;
            for (com.example.instacare.data.local.EvacuationCenter c : localCenters) {
                if ("api".equals(c.source)) { hasApiCache = true; break; }
            }

            if (currentUserLocation != null) {
                for (com.example.instacare.data.local.EvacuationCenter c : localCenters) {
                    org.osmdroid.util.GeoPoint pt = new org.osmdroid.util.GeoPoint(c.latitude, c.longitude);
                    double distKm = currentUserLocation.distanceToAsDouble(pt) / 1000.0;
                    c.distance = com.example.instacare.utils.DistanceUtils.formatDistance(distKm);
                }
            }

            final List<com.example.instacare.data.local.EvacuationCenter> merged = new ArrayList<>(localCenters);

            if (!forceRefresh && hasApiCache) {
                if (currentUserLocation != null) {
                    Collections.sort(merged, (a, b) -> {
                        org.osmdroid.util.GeoPoint pa = new org.osmdroid.util.GeoPoint(a.latitude, a.longitude);
                        org.osmdroid.util.GeoPoint pb = new org.osmdroid.util.GeoPoint(b.latitude, b.longitude);
                        return Double.compare(currentUserLocation.distanceToAsDouble(pa), currentUserLocation.distanceToAsDouble(pb));
                    });
                }
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        displayEvacuationResults(merged);
                    });
                }
            } else {
                if (currentUserLocation != null) {
                    List<com.example.instacare.data.local.EvacuationCenter> existingLocalOnly = new ArrayList<>();
                    for (com.example.instacare.data.local.EvacuationCenter c : localCenters) {
                        if (!"api".equals(c.source)) existingLocalOnly.add(c);
                    }
                    fetchEvacuationViaOverpassAPI(currentUserLocation, existingLocalOnly);
                } else {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> displayEvacuationResults(merged));
                    }
                }
            }
        });
    }

    private void fetchEvacuationViaOverpassAPI(org.osmdroid.util.GeoPoint location, List<com.example.instacare.data.local.EvacuationCenter> existingLocal) {
        if (location == null || !isAdded()) return;

        try {
            String query = "[out:json];("
                + "nwr[\"amenity\"=\"shelter\"](around:50000," + location.getLatitude() + "," + location.getLongitude() + ");"
                + "nwr[\"social_facility\"=\"shelter\"](around:50000," + location.getLatitude() + "," + location.getLongitude() + ");"
                + "nwr[\"emergency\"=\"assembly_point\"](around:50000," + location.getLatitude() + "," + location.getLongitude() + ");"
                + "nwr[\"amenity\"=\"community_centre\"](around:50000," + location.getLatitude() + "," + location.getLongitude() + ");"
                + ");out center;";
            String urlString = "https://overpass-api.de/api/interpreter?data=" + URLEncoder.encode(query, "UTF-8");

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject json = new JSONObject(response.toString());
                JSONArray elements = json.getJSONArray("elements");

                List<com.example.instacare.data.local.EvacuationCenter> apiFetched = new ArrayList<>();

                for (int i = 0; i < elements.length(); i++) {
                    JSONObject element = elements.getJSONObject(i);

                    double lat = 0, lon = 0;
                    if (element.has("center")) {
                        JSONObject center = element.getJSONObject("center");
                        lat = center.getDouble("lat");
                        lon = center.getDouble("lon");
                    } else if (element.has("lat") && element.has("lon")) {
                        lat = element.getDouble("lat");
                        lon = element.getDouble("lon");
                    } else {
                        continue;
                    }

                    String name = "Evacuation Center";
                    String addr = "";
                    String cType = "Shelter";
                    if (element.has("tags")) {
                        JSONObject tags = element.getJSONObject("tags");
                        
                        // Extensive fallback for finding a real name
                        if (tags.has("name")) {
                            name = tags.getString("name");
                        } else if (tags.has("official_name")) {
                            name = tags.getString("official_name");
                        } else if (tags.has("alt_name")) {
                            name = tags.getString("alt_name");
                        } else if (tags.has("short_name")) {
                            name = tags.getString("short_name");
                        } else if (tags.has("operator")) {
                            name = tags.getString("operator") + " Evacuation Center";
                        } else if (tags.has("brand")) {
                            name = tags.getString("brand") + " Center";
                        } else if (tags.has("description")) {
                            name = tags.getString("description");
                        }
                        
                        if (tags.has("addr:street")) addr = tags.optString("addr:street", "");
                        if (tags.has("amenity")) cType = capitalizeStr(tags.getString("amenity"));
                        if (tags.has("building")) cType = capitalizeStr(tags.getString("building"));
                    }

                    org.osmdroid.util.GeoPoint pt = new org.osmdroid.util.GeoPoint(lat, lon);
                    double distKm = location.distanceToAsDouble(pt) / 1000.0;
                    String distStr = com.example.instacare.utils.DistanceUtils.formatDistance(distKm);

                    boolean isDuplicate = false;
                    for (com.example.instacare.data.local.EvacuationCenter local : existingLocal) {
                        org.osmdroid.util.GeoPoint lp = new org.osmdroid.util.GeoPoint(local.latitude, local.longitude);
                        if (pt.distanceToAsDouble(lp) < 100) { isDuplicate = true; break; }
                    }
                    if (isDuplicate) continue;

                    com.example.instacare.data.local.EvacuationCenter c = new com.example.instacare.data.local.EvacuationCenter(
                        "api_" + i, name, addr, lat, lon, cType, "Open"
                    );
                    c.distance = distStr;
                    c.source = "api";
                    c.centerType = cType;
                    apiFetched.add(c);
                }

                com.example.instacare.data.local.AppDatabase db = com.example.instacare.data.local.AppDatabase.getDatabase(requireContext());
                db.evacuationCenterDao().deleteApiCenters();
                db.evacuationCenterDao().insertAll(apiFetched);

                List<com.example.instacare.data.local.EvacuationCenter> allCenters = new ArrayList<>(existingLocal);
                allCenters.addAll(apiFetched);

                Collections.sort(allCenters, (a, b) -> {
                    org.osmdroid.util.GeoPoint pa = new org.osmdroid.util.GeoPoint(a.latitude, a.longitude);
                    org.osmdroid.util.GeoPoint pb = new org.osmdroid.util.GeoPoint(b.latitude, b.longitude);
                    return Double.compare(location.distanceToAsDouble(pa), location.distanceToAsDouble(pb));
                });

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> displayEvacuationResults(allCenters));
                }
            } else {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> displayEvacuationResults(existingLocal));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    displayEvacuationResults(existingLocal);
                });
            }
        }
    }

    private void displayEvacuationResults(List<com.example.instacare.data.local.EvacuationCenter> centers) {
        if (!isAdded()) return;
        currentEvacCenters = centers;

        if (hospitalsRecyclerView.getAdapter() instanceof EvacuationAdapter) {
            ((EvacuationAdapter) hospitalsRecyclerView.getAdapter()).setCenters(currentEvacCenters);
        }
        if ("evacuation".equals(currentMode)) {
            updateMapMarkersEvacuation(currentEvacCenters);
        }

        View emptyState = getView() != null ? getView().findViewById(R.id.emptyStateHospitals) : null;
        if (currentEvacCenters.isEmpty()) {
            if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
            hospitalsRecyclerView.setVisibility(View.GONE);
        } else {
            if (emptyState != null) emptyState.setVisibility(View.GONE);
            hospitalsRecyclerView.setVisibility(View.VISIBLE);
        }

        if ("checkin".equals(currentMode)) {
            updateMapMarkersCheckIn(currentEvacCenters);
        }

        showLoading(false);
    }

    private void updateMapMarkersEvacuation(List<com.example.instacare.data.local.EvacuationCenter> centers) {
        if (mapView == null || !isAdded()) return;

        mapView.getOverlays().clear();

        org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer clusterer =
            new org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer(requireContext());
        clusterer.setName("Evacuation");

        org.osmdroid.util.GeoPoint startPoint = currentUserLocation != null ? currentUserLocation : new org.osmdroid.util.GeoPoint(12.8797, 121.7740);
        addMarkerToMap(startPoint, "Me", "My Current Location", R.drawable.user_location_dot, null, null);

        if (centers != null) {
            for (com.example.instacare.data.local.EvacuationCenter c : centers) {
                if (c == null) continue;
                
                // 🚦 Route Isolation: Hide other markers if viewing a specific route
                if (isRouteOnly && autoRouteTarget != null && !c.name.equalsIgnoreCase(autoRouteTarget)) {
                    continue; 
                }
                
                org.osmdroid.util.GeoPoint p = new org.osmdroid.util.GeoPoint(c.latitude, c.longitude);
                addEvacMarkerToMap(p, c.name, c.type != null ? c.type : "Shelter", R.drawable.ic_marker_evac, c, clusterer);
            }
        }

        mapView.getOverlays().add(clusterer);

        if (currentRoute != null && !mapView.getOverlays().contains(currentRoute)) {
            mapView.getOverlays().add(0, currentRoute);
        }

        org.osmdroid.views.overlay.MapEventsOverlay mapEventsOverlay = new org.osmdroid.views.overlay.MapEventsOverlay(new org.osmdroid.events.MapEventsReceiver() {
            @Override public boolean singleTapConfirmedHelper(org.osmdroid.util.GeoPoint p) { toggleImmersiveMode(); return true; }
            @Override public boolean longPressHelper(org.osmdroid.util.GeoPoint p) { return false; }
        });
        mapView.getOverlays().add(0, mapEventsOverlay);
        mapView.invalidate();
    }

    private void addEvacMarkerToMap(org.osmdroid.util.GeoPoint point, String title, String snippet, int iconRes,
                                     com.example.instacare.data.local.EvacuationCenter center,
                                     org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer clusterer) {
        if (mapView == null || !isAdded()) return;

        org.osmdroid.views.overlay.Marker marker = new org.osmdroid.views.overlay.Marker(mapView);
        marker.setPosition(point);
        try {
            android.graphics.drawable.Drawable icon = androidx.core.content.ContextCompat.getDrawable(requireContext(), iconRes);
            if (icon != null) marker.setIcon(icon);
        } catch (Exception e) { /* fallback */ }
        marker.setTitle(title);
        marker.setSnippet(snippet);
        marker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM);

        org.osmdroid.views.overlay.infowindow.MarkerInfoWindow infoWindow =
            new org.osmdroid.views.overlay.infowindow.MarkerInfoWindow(R.layout.info_window, mapView) {
            @Override
            public void onOpen(Object item) {
                View view = getView();
                if (view == null) return;
                view.setVisibility(View.VISIBLE);
                view.setAlpha(1.0f);

                TextView nameTv = view.findViewById(R.id.infoWindowName);
                TextView detailsTv = view.findViewById(R.id.infoWindowDetails);
                TextView capacityTv = view.findViewById(R.id.infoWindowCapacity);
                com.google.android.material.button.MaterialButton routeBtn = view.findViewById(R.id.infoWindowRouteBtn);

                if (routeBtn != null && center != null) {
                    routeBtn.setOnClickListener(v -> { drawRouteToEvacuation(center); close(); });
                }
                if (nameTv != null) nameTv.setText(title);
                if (detailsTv != null) detailsTv.setText(snippet);
                if (capacityTv != null) {
                    String cap = center != null && center.status != null ? center.status : "Open";
                    capacityTv.setText(cap);
                    capacityTv.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                        cap.equalsIgnoreCase("Full") || cap.equalsIgnoreCase("Closed") ? 0xFFE53935 : 0xFF2563EB));
                }
            }
        };
        marker.setInfoWindow(infoWindow);

        if (clusterer != null) { clusterer.add(marker); } else { mapView.getOverlays().add(marker); }
    }

    private void redirectMapToEvacuation(com.example.instacare.data.local.EvacuationCenter center) {
        if (mapView == null) return;

        org.osmdroid.util.GeoPoint loc = new org.osmdroid.util.GeoPoint(center.latitude, center.longitude);
        mapView.getController().setZoom(17.0);
        mapView.getController().animateTo(loc);

        for (org.osmdroid.views.overlay.Overlay overlay : mapView.getOverlays()) {
            if (overlay instanceof org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer) {
                org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer clusterer = (org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer) overlay;
                for (org.osmdroid.views.overlay.Marker marker : clusterer.getItems()) {
                    if (marker.getPosition().getLatitude() == center.latitude &&
                        marker.getPosition().getLongitude() == center.longitude) {
                        for (org.osmdroid.views.overlay.Marker m : clusterer.getItems()) m.closeInfoWindow();
                        marker.showInfoWindow();
                        break;
                    }
                }
            }
        }
        toggleImmersiveMode();
    }

    private void drawRouteToEvacuation(com.example.instacare.data.local.EvacuationCenter center) {
        if (mapView == null || currentUserLocation == null) return;

        executor.execute(() -> {
            try {
                org.osmdroid.bonuspack.routing.RoadManager roadManager = new org.osmdroid.bonuspack.routing.OSRMRoadManager(requireContext(), "InstaCare-Agent");
                ArrayList<org.osmdroid.util.GeoPoint> waypoints = new ArrayList<>();
                waypoints.add(currentUserLocation);
                waypoints.add(new org.osmdroid.util.GeoPoint(center.latitude, center.longitude));
                org.osmdroid.bonuspack.routing.Road road = roadManager.getRoad(waypoints);

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (currentRoute != null) mapView.getOverlays().remove(currentRoute);
                        if (road.mStatus == org.osmdroid.bonuspack.routing.Road.STATUS_OK) {
                            currentRoute = org.osmdroid.bonuspack.routing.RoadManager.buildRoadOverlay(road);
                            currentRoute.setColor(android.graphics.Color.parseColor("#2563EB")); // Blue for evacuation
                            currentRoute.setWidth(12.0f);
                            mapView.getOverlays().add(0, currentRoute);
                            mapView.invalidate();

                            // 🛰️ Update Parent (MapRouteActivity) with stats
                            String roadDistStr = com.example.instacare.utils.DistanceUtils.formatDistance(road.mLength);
                            android.os.Bundle res = new android.os.Bundle();
                            res.putDouble("DISTANCE", road.mLength); // usually km
                            res.putDouble("DURATION", road.mDuration); // usually seconds
                            res.putString("DESTINATION", center.name);
                            getParentFragmentManager().setFragmentResult("route_update", res);

                            // 🔄 Sync back to list/marker data
                            center.distance = roadDistStr;
                            if (hospitalsRecyclerView.getAdapter() != null) {
                                hospitalsRecyclerView.getAdapter().notifyDataSetChanged();
                            }
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                    });
                }
            }
        });
    }

    private String capitalizeStr(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase() + s.substring(1).replace("_", " ");
    }

    private void drawRouteToHospital(com.example.instacare.data.local.Hospital hospital) {
        if (mapView == null || currentUserLocation == null) return;
        
        executor.execute(() -> {
            try {
                org.osmdroid.bonuspack.routing.RoadManager roadManager = new org.osmdroid.bonuspack.routing.OSRMRoadManager(requireContext(), "InstaCare-Agent");
                
                ArrayList<org.osmdroid.util.GeoPoint> waypoints = new ArrayList<org.osmdroid.util.GeoPoint>();
                waypoints.add(currentUserLocation);
                waypoints.add(new org.osmdroid.util.GeoPoint(hospital.latitude, hospital.longitude));
                
                org.osmdroid.bonuspack.routing.Road road = roadManager.getRoad(waypoints);
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (currentRoute != null) {
                            mapView.getOverlays().remove(currentRoute);
                        }
                        if (road.mStatus == org.osmdroid.bonuspack.routing.Road.STATUS_OK) {
                            currentRoute = org.osmdroid.bonuspack.routing.RoadManager.buildRoadOverlay(road);
                            currentRoute.setColor(android.graphics.Color.parseColor("#EF4444")); // Red for hospitals
                            currentRoute.setWidth(12.0f);
                            mapView.getOverlays().add(0, currentRoute); 
                            mapView.invalidate();

                            // 🛰️ Update Parent (MapRouteActivity) with stats
                            String roadDistStr = com.example.instacare.utils.DistanceUtils.formatDistance(road.mLength);
                            android.os.Bundle res = new android.os.Bundle();
                            res.putDouble("DISTANCE", road.mLength); // usually km
                            res.putDouble("DURATION", road.mDuration); // usually seconds
                            res.putString("DESTINATION", hospital.name);
                            getParentFragmentManager().setFragmentResult("route_update", res);

                            // 🔄 Sync back to list/marker data
                            hospital.distance = roadDistStr;
                            if (hospitalsRecyclerView.getAdapter() != null) {
                                hospitalsRecyclerView.getAdapter().notifyDataSetChanged();
                            }
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                    });
                }
            }
        });
    }

    private void redirectMapToHospital(com.example.instacare.data.local.Hospital hospital) {
        if (mapView == null) return;
        
        org.osmdroid.util.GeoPoint hospLoc = new org.osmdroid.util.GeoPoint(hospital.latitude, hospital.longitude);

        // 1. Zoom and Center
        mapView.getController().setZoom(17.0);
        mapView.getController().animateTo(hospLoc);
        
        // 2. Find marker and show InfoWindow
        for (org.osmdroid.views.overlay.Overlay overlay : mapView.getOverlays()) {
            if (overlay instanceof org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer) {
                org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer clusterer = (org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer) overlay;
                for (org.osmdroid.views.overlay.Marker marker : clusterer.getItems()) {
                    if (marker.getPosition().getLatitude() == hospital.latitude && 
                        marker.getPosition().getLongitude() == hospital.longitude) {
                        
                        // Close other windows first
                        for (org.osmdroid.views.overlay.Marker m : clusterer.getItems()) m.closeInfoWindow();
                        
                        marker.showInfoWindow();
                        break;
                    }
                }
            }
        }
        
        // 3. Close Bottom Sheet (Optional: immersive view)
        toggleImmersiveMode();
    }

    private void updateMapMarkers(List<com.example.instacare.data.local.Hospital> hospitals) {
        if (mapView == null || !isAdded()) return;
        
        mapView.getOverlays().clear();
        
        // 1. Create Clusterer with default styling for stability
        org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer clusterer = 
            new org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer(requireContext());
        clusterer.setName("Hospitals");
        
        // 2. Add "Me" marker (Always outside clusterer)
        org.osmdroid.util.GeoPoint startPoint = currentUserLocation != null ? currentUserLocation : new org.osmdroid.util.GeoPoint(12.8797, 121.7740);
        addMarkerToMap(startPoint, "Me", "My Current Location", R.drawable.user_location_dot, null, null);

        // 3. Add Hospital Markers to Clusterer
        if (hospitals != null) {
            for (com.example.instacare.data.local.Hospital h : hospitals) {
                if (h == null) continue;
                
                // 🚦 Route Isolation: Hide other markers if viewing a specific route
                if (isRouteOnly && autoRouteTarget != null && !h.name.equalsIgnoreCase(autoRouteTarget)) {
                    continue; 
                }
                
                org.osmdroid.util.GeoPoint p = new org.osmdroid.util.GeoPoint(h.latitude, h.longitude);
                addMarkerToMap(p, h.name, h.type, R.drawable.ic_marker_hospital_cool, h, clusterer); 
            }
        }
        
        mapView.getOverlays().add(clusterer);
        
        if (currentRoute != null && !mapView.getOverlays().contains(currentRoute)) {
            mapView.getOverlays().add(0, currentRoute);
        }
        
        // 4. Add Map Events Overlay
        org.osmdroid.views.overlay.MapEventsOverlay mapEventsOverlay = new org.osmdroid.views.overlay.MapEventsOverlay(new org.osmdroid.events.MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(org.osmdroid.util.GeoPoint p) {
                toggleImmersiveMode();
                return true;
            }

            @Override
            public boolean longPressHelper(org.osmdroid.util.GeoPoint p) {
                return false;
            }
        });
        mapView.getOverlays().add(0, mapEventsOverlay);
        mapView.invalidate();
    }

    private void setupMap() {
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(6.0); 
        
        org.osmdroid.util.GeoPoint phCenter = new org.osmdroid.util.GeoPoint(12.8797, 121.7740);
        mapView.getController().setCenter(phCenter);

        org.osmdroid.views.overlay.MapEventsOverlay mapEventsOverlay = new org.osmdroid.views.overlay.MapEventsOverlay(new org.osmdroid.events.MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(org.osmdroid.util.GeoPoint p) {
                toggleImmersiveMode();
                return true;
            }

            @Override
            public boolean longPressHelper(org.osmdroid.util.GeoPoint p) {
                return false;
            }
        });
        mapView.getOverlays().add(0, mapEventsOverlay);
    }

    private void addMarkerToMap(org.osmdroid.util.GeoPoint point, String title, String snippet, int iconRes, 
                             com.example.instacare.data.local.Hospital hospital, org.osmdroid.bonuspack.clustering.RadiusMarkerClusterer clusterer) {
        if (mapView == null || !isAdded()) return;
        
        boolean isUser = "Me".equals(title);
        
        org.osmdroid.views.overlay.Marker marker = new org.osmdroid.views.overlay.Marker(mapView);
        marker.setPosition(point);
        try {
            android.graphics.drawable.Drawable icon = androidx.core.content.ContextCompat.getDrawable(requireContext(), iconRes);
            if (icon != null) marker.setIcon(icon);
        } catch (Exception e) {
            // Fallback
        }
        marker.setTitle(title);
        marker.setSnippet(snippet);
        marker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM);
        
        if (isUser) {
             marker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER, org.osmdroid.views.overlay.Marker.ANCHOR_CENTER);
        } else {
            org.osmdroid.views.overlay.infowindow.MarkerInfoWindow infoWindow = 
                new org.osmdroid.views.overlay.infowindow.MarkerInfoWindow(R.layout.info_window, mapView) {
                @Override
                public void onOpen(Object item) {
                    View view = getView();
                    if (view == null) return;
                    
                    view.setVisibility(View.VISIBLE);
                    view.setAlpha(1.0f);
                    
                    TextView nameTv = view.findViewById(R.id.infoWindowName);
                    TextView detailsTv = view.findViewById(R.id.infoWindowDetails);
                    ImageView imageIv = view.findViewById(R.id.infoWindowImage);
                    TextView dismissTv = view.findViewById(R.id.infoWindowDismiss);
                    TextView capacityTv = view.findViewById(R.id.infoWindowCapacity);
                    View infoCard = view.findViewById(R.id.infoWindowCard);
                    com.google.android.material.button.MaterialButton routeBtn = view.findViewById(R.id.infoWindowRouteBtn);
                    
                    if (routeBtn != null && hospital != null) {
                        routeBtn.setOnClickListener(v -> {
                            drawRouteToHospital(hospital);
                            close();
                        });
                    }
                    
                    if (nameTv != null) nameTv.setText(title);
                    if (detailsTv != null) detailsTv.setText(snippet);
                    
                    if (capacityTv != null && hospital != null) {
                        String cap = hospital.capacityStatus != null ? hospital.capacityStatus : "Available";
                        capacityTv.setText(cap);
                        if (cap.equalsIgnoreCase("Full")) {
                            capacityTv.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE53935)); // Red
                        } else if (cap.equalsIgnoreCase("Moderate")) {
                            capacityTv.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFB8C00)); // Orange
                        } else {
                            capacityTv.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF43A047)); // Green
                        }
                    }
                    
                    // Load actual hospital image with Glide
                    if (imageIv != null && hospital != null && hospital.imageUrl != null) {
                        int resId = imageIv.getContext().getResources().getIdentifier(hospital.imageUrl, "drawable", imageIv.getContext().getPackageName());
                        Object loadTarget = resId != 0 ? resId : hospital.imageUrl;
                        com.bumptech.glide.Glide.with(imageIv.getContext())
                            .load(loadTarget)
                            .placeholder(R.drawable.bg_header_rounded)
                            .error(R.drawable.bg_header_rounded)
                            .into(imageIv);
                    }
                    
                    if (dismissTv != null) dismissTv.setOnClickListener(v -> close());
                    
                    if (infoCard != null && hospital != null) {
                        // Override default osmdroid behavior which closes InfoWindow on touch by default
                        infoCard.setOnTouchListener((v, event) -> {
                            if (event.getAction() == android.view.MotionEvent.ACTION_UP) {
                                HospitalDetailsBottomSheet bottomSheet = HospitalDetailsBottomSheet.newInstance(
                                    hospital.name, hospital.imageUrl, hospital.address,
                                    hospital.distance, hospital.phone, hospital.type,
                                    hospital.capacityStatus, hospital.servicesJson
                                );
                                bottomSheet.show(getChildFragmentManager(), "HospitalDetails");
                            }
                            return true;
                        });
                    }
                }
            };
            marker.setInfoWindow(infoWindow);
            
            marker.setOnMarkerClickListener((m, mv) -> {
                if (m == null || mv == null) return true;
                // Close others in clusterer
                if (clusterer != null) {
                    for (org.osmdroid.views.overlay.Marker cm : clusterer.getItems()) cm.closeInfoWindow();
                }
                m.showInfoWindow();
                mv.getController().animateTo(m.getPosition());
                return true;
            });
        }
        
        if (clusterer != null) {
            clusterer.add(marker);
        } else {
            mapView.getOverlays().add(marker);
        }
    }

    private void setupImmersiveMode() {
        BottomSheetBehavior<MaterialCardView> behavior = 
            BottomSheetBehavior.from(bottomSheet);

        behavior.setHideable(false);
        navFabContainer.setVisibility(View.GONE);
        
        behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                // Ensure map is ALWAYS interactive
                if (mapView != null) {
                    mapView.setOnTouchListener(null);
                    mapView.setClickable(true);
                    mapView.setFocusable(true);
                }
            }
            @Override public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
        });
    }

    private void toggleImmersiveMode() {
        if (isRouteOnly) return;
        isImmersiveMode = !isImmersiveMode;
        BottomSheetBehavior<MaterialCardView> behavior = BottomSheetBehavior.from(bottomSheet);
            
        if (isImmersiveMode) {
            behavior.setHideable(true);
            behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            
            // Slide Animations
            topBar.animate().translationY(-topBar.getHeight()).setDuration(300).start();
            bottomSheet.animate().translationY(bottomSheet.getHeight()).setDuration(300).start(); // Slide down matching top bar
            
            navFabContainer.setVisibility(View.VISIBLE);
            navFabContainer.setAlpha(0f);
            navFabContainer.animate().alpha(1f).setDuration(300).start();
            
            if (getActivity() instanceof UserDashboardActivity) ((UserDashboardActivity) getActivity()).hideBottomNav();
        } else {
            topBar.animate().translationY(0).setDuration(300).start();
            bottomSheet.animate().translationY(0).setDuration(300).start();
            
            navFabContainer.setVisibility(View.GONE);
            behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            behavior.setHideable(false);
            if (getActivity() instanceof UserDashboardActivity) ((UserDashboardActivity) getActivity()).showBottomNav();
        }
    }
    
    private void toggleDetailedCompass() {
        if (compassOverlay == null) return;
        
        if (compassOverlay.getVisibility() == View.VISIBLE) {
            compassOverlay.animate().alpha(0f).setDuration(300).withEndAction(() -> {
                compassOverlay.setVisibility(View.GONE);
            }).start();
        } else {
            compassOverlay.setAlpha(0f);
            compassOverlay.setVisibility(View.VISIBLE);
            compassOverlay.animate().alpha(1f).setDuration(300).start();
        }
    }

    private void setupCompassSensors() {
        if (getContext() != null) {
            sensorManager = (SensorManager) getContext().getSystemService(Context.SENSOR_SERVICE);
            if (sensorManager != null) {
                rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            }
        }
    }

    private void checkAndRequestLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationTracking();
        } else {
            locationPermissionRequest.launch(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void startLocationTracking() {
        try {
            boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            
            if (!isGpsEnabled && !isNetworkEnabled) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // PREVENT DUPLICATES: Check if a dialog is already showing
                        if (gpsDialog != null && gpsDialog.isShowing()) return;

                        gpsDialog = new android.app.AlertDialog.Builder(requireContext())
                            .setTitle("GPS is Disabled")
                            .setMessage("Please turn on your Location (GPS) in your phone's Settings to find nearby hospitals.")
                            .setCancelable(false)
                            .setPositiveButton("Open Settings", (dialog, which) -> {
                                startActivity(new android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                                gpsDialog = null; // Reset pointer
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> {
                                gpsDialog = null; // Reset pointer
                            })
                            .create();

                        // --- PREMIUM AESTHETIC: Deep Dim + Frosted Glass Blur ---
                        com.example.instacare.utils.BlurUtils.applyBlur(gpsDialog);

                        gpsDialog.show();
                    });
                }
                return; // Stop tracking attempts until enabled
            }

            if (locationListener == null) {
                locationListener = new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull android.location.Location location) {
                        updateUserLocation(new org.osmdroid.util.GeoPoint(location.getLatitude(), location.getLongitude()));
                    }
                };
            }

            if (isGpsEnabled) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10f, locationListener, Looper.getMainLooper());
            }
            if (isNetworkEnabled) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 10f, locationListener, Looper.getMainLooper());
            }
            
            android.location.Location lastLoc = null;
            if (isGpsEnabled) lastLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastLoc == null && isNetworkEnabled) lastLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            
            if (lastLoc != null) {
                updateUserLocation(new org.osmdroid.util.GeoPoint(lastLoc.getLatitude(), lastLoc.getLongitude()));
                if (mapView != null) {
                    mapView.getController().setCenter(currentUserLocation);
                    mapView.getController().setZoom(15.0);
                }
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    private void updateUserLocation(org.osmdroid.util.GeoPoint newLocation) {
        if (getActivity() == null || !isAdded()) return;
        
        boolean isFirstLock = (currentUserLocation == null);
        currentUserLocation = newLocation;
        
        if (isFirstLock) {
            // First time we got a GPS lock, fetch real hospitals from Overpass API
            if (getActivity() != null) {
                loadHospitalData(false);
                loadEvacuationData(false);
            }
        }
        
        // 🏥 Update Hospital Distances & Sorting
        if (currentHospitals != null && !currentHospitals.isEmpty()) {
            Collections.sort(currentHospitals, new Comparator<com.example.instacare.data.local.Hospital>() {
                @Override
                public int compare(com.example.instacare.data.local.Hospital h1, com.example.instacare.data.local.Hospital h2) {
                    org.osmdroid.util.GeoPoint p1 = new org.osmdroid.util.GeoPoint(h1.latitude, h1.longitude);
                    org.osmdroid.util.GeoPoint p2 = new org.osmdroid.util.GeoPoint(h2.latitude, h2.longitude);
                    return Double.compare(currentUserLocation.distanceToAsDouble(p1), currentUserLocation.distanceToAsDouble(p2));
                }
            });
            for (com.example.instacare.data.local.Hospital h : currentHospitals) {
                org.osmdroid.util.GeoPoint hospLoc = new org.osmdroid.util.GeoPoint(h.latitude, h.longitude);
                double distanceKm = currentUserLocation.distanceToAsDouble(hospLoc) / 1000.0;
                h.distance = com.example.instacare.utils.DistanceUtils.formatDistance(distanceKm);
            }
            if ("hospitals".equals(currentMode) && hospitalsRecyclerView.getAdapter() instanceof HospitalAdapter) {
                ((HospitalAdapter) hospitalsRecyclerView.getAdapter()).setHospitals(currentHospitals);
            }
        }
        
        // ⛺ Update Evacuation Distances & Sorting
        if (currentEvacCenters != null && !currentEvacCenters.isEmpty()) {
            Collections.sort(currentEvacCenters, (a, b) -> {
                org.osmdroid.util.GeoPoint pa = new org.osmdroid.util.GeoPoint(a.latitude, a.longitude);
                org.osmdroid.util.GeoPoint pb = new org.osmdroid.util.GeoPoint(b.latitude, b.longitude);
                return Double.compare(currentUserLocation.distanceToAsDouble(pa), currentUserLocation.distanceToAsDouble(pb));
            });
            for (com.example.instacare.data.local.EvacuationCenter c : currentEvacCenters) {
                org.osmdroid.util.GeoPoint pt = new org.osmdroid.util.GeoPoint(c.latitude, c.longitude);
                double distKm = currentUserLocation.distanceToAsDouble(pt) / 1000.0;
                c.distance = com.example.instacare.utils.DistanceUtils.formatDistance(distKm);
            }
            if ("evacuation".equals(currentMode) && hospitalsRecyclerView.getAdapter() instanceof EvacuationAdapter) {
                ((EvacuationAdapter) hospitalsRecyclerView.getAdapter()).setCenters(currentEvacCenters);
            }
        }
        
        // 🗺️ Refresh Active Markers
        if ("evacuation".equals(currentMode)) {
            updateMapMarkersEvacuation(currentEvacCenters);
        } else if ("checkin".equals(currentMode)) {
            loadCheckInData(false); // Refresh check-in markers
        } else {
            updateMapMarkers(currentHospitals);
        }
        
        if (isFirstLock && mapView != null) {
            mapView.getController().animateTo(currentUserLocation);
            mapView.getController().setZoom(15.0);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sensorManager != null && rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI);
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationTracking();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        if (locationManager != null && locationListener != null) {
            locationManager.removeUpdates(locationListener);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            SensorManager.getOrientation(rotationMatrix, orientationValues);
            
            // Azimuth is in orientationValues[0], convert to degrees
            float azimuth = (float) Math.toDegrees(orientationValues[0]);
            
            // Smoothen the rotation slightly if needed, but for now direct set
            if (navFabIcon != null) {
                // Invert the rotation so the arrow points North
                navFabIcon.setRotation(-azimuth);
            }
            
            if (compassDial != null && compassOverlay.getVisibility() == View.VISIBLE) {
                // Rotate the entire dial
                compassDial.setRotation(-azimuth);
            }
        }
    }

    private void setupCheckInRecyclerView() {
        setupEvacuationRecyclerView();
    }

    private void loadCheckInData(boolean forceRefresh) {
        if (!isAdded()) return;
        // Reuse evacuation centers for check-in spots
        currentMode = "checkin";
        setupEvacuationRecyclerView();
        loadEvacuationData(forceRefresh);
    }

    private void updateMapMarkersCheckIn(List<com.example.instacare.data.local.EvacuationCenter> centers) {
        if (mapView == null || !isAdded()) return;
        mapView.getOverlays().clear();
        
        org.osmdroid.util.GeoPoint startPoint = currentUserLocation != null ? currentUserLocation : new org.osmdroid.util.GeoPoint(12.8797, 121.7740);
        addMarkerToMap(startPoint, "Me", "My Current Location", R.drawable.user_location_dot, null, null);

        if (centers != null) {
            for (com.example.instacare.data.local.EvacuationCenter c : centers) {
                org.osmdroid.util.GeoPoint p = new org.osmdroid.util.GeoPoint(c.latitude, c.longitude);
                org.osmdroid.views.overlay.Marker marker = new org.osmdroid.views.overlay.Marker(mapView);
                marker.setPosition(p);
                marker.setIcon(androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.ic_check_circle));
                marker.setTitle(c.name);
                marker.setSnippet("Check in here for safety");
                mapView.getOverlays().add(marker);
            }
        }
        mapView.invalidate();
    }

    private void filterEvacuationByDisaster(String disaster) {
        if (!isAdded() || currentEvacCenters == null) return;
        
        if ("all".equalsIgnoreCase(disaster)) {
            displayEvacuationResults(currentEvacCenters);
            return;
        }

        List<com.example.instacare.data.local.EvacuationCenter> filtered = new ArrayList<>();
        for (com.example.instacare.data.local.EvacuationCenter c : currentEvacCenters) {
            if (c.disasterTypes != null && c.disasterTypes.toLowerCase().contains(disaster.toLowerCase())) {
                filtered.add(c);
            }
        }
        
        if (hospitalsRecyclerView.getAdapter() instanceof EvacuationAdapter) {
            ((EvacuationAdapter) hospitalsRecyclerView.getAdapter()).setCenters(filtered);
        }
        updateMapMarkersEvacuation(filtered);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed
    }
}

