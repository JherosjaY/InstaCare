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

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.ArrayList;
import java.util.List;

public class HospitalsFragment extends Fragment implements SensorEventListener {

    private boolean isImmersiveMode = false;

    // Views
    private RecyclerView hospitalsRecyclerView;
    private MaterialCardView bottomSheet;
    private FrameLayout mapContainer;
    private org.osmdroid.views.MapView mapView; // Real Map
    private ConstraintLayout topBar;
    private MaterialCardView navFabContainer;
    private ImageView navFabIcon;
    
    // Detailed Compass Views
    private View compassOverlay;
    private View compassDial;
    
    // Compass Sensors
    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private final float[] rotationMatrix = new float[9];
    private final float[] orientationValues = new float[3];
    private float currentAzimuth = 0f;
    
    // Info Window (Removed static members, now dynamic)

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
        
        hospitalsRecyclerView = view.findViewById(R.id.hospitalsRecyclerView);
        bottomSheet = view.findViewById(R.id.bottomSheet);
        mapContainer = view.findViewById(R.id.mapContainer);
        mapView = view.findViewById(R.id.mapView);
        topBar = view.findViewById(R.id.topBar);
        navFabContainer = view.findViewById(R.id.navFabContainer);
        navFabIcon = view.findViewById(R.id.navFabIcon);
        compassOverlay = view.findViewById(R.id.compassOverlay);
        compassDial = view.findViewById(R.id.compassDial);
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
                android.widget.Toast.makeText(requireContext(), "Map reset to North", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
        
        refreshCard.setOnClickListener(v -> {
            // Animate refresh icon rotation
            v.animate().rotationBy(360f).setDuration(500).start();
            
            // Re-fetch hospitals data & redraw map markers
            com.example.instacare.data.local.AppDatabase.getDatabase(requireContext().getApplicationContext())
                .hospitalDao().getAllHospitals().observe(getViewLifecycleOwner(), hospitals -> {
                    if (hospitals != null) updateMapMarkers(hospitals);
                });
                
            // Back to default center
            org.osmdroid.util.GeoPoint phCenter = new org.osmdroid.util.GeoPoint(12.8797, 121.7740);
            mapView.getController().animateTo(phCenter);
            mapView.getController().setZoom(6.0);
            
            android.widget.Toast.makeText(requireContext(), "Map Refreshed", android.widget.Toast.LENGTH_SHORT).show();
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

        // Observe LiveData from DB
        com.example.instacare.data.local.AppDatabase.getDatabase(requireContext().getApplicationContext())
            .hospitalDao().getAllHospitals().observe(getViewLifecycleOwner(), hospitals -> {
                if (hospitals != null) {
                    // Calculate Real Distances
                    org.osmdroid.util.GeoPoint userLoc = new org.osmdroid.util.GeoPoint(7.9097, 125.0933); // Mock user location
                    for (com.example.instacare.data.local.Hospital h : hospitals) {
                        org.osmdroid.util.GeoPoint hospLoc = new org.osmdroid.util.GeoPoint(h.latitude, h.longitude);
                        double distanceKm = userLoc.distanceToAsDouble(hospLoc) / 1000.0;
                        if (distanceKm > 100) {
                            h.distance = String.format("%.0f km", distanceKm);
                        } else {
                            h.distance = String.format("%.1f km", distanceKm);
                        }
                    }
                    
                    adapter.setHospitals(hospitals);
                    updateMapMarkers(hospitals);
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
        org.osmdroid.util.GeoPoint startPoint = new org.osmdroid.util.GeoPoint(7.9097, 125.0933);
        addMarkerToMap(startPoint, "Me", "My Current Location", R.drawable.user_location_dot, null, null);

        // 3. Add Hospital Markers to Clusterer
        if (hospitals != null) {
            for (com.example.instacare.data.local.Hospital h : hospitals) {
                if (h == null) continue;
                org.osmdroid.util.GeoPoint p = new org.osmdroid.util.GeoPoint(h.latitude, h.longitude);
                addMarkerToMap(p, h.name, h.type, R.drawable.ic_marker_hospital_cool, h, clusterer); 
            }
        }
        
        mapView.getOverlays().add(clusterer);
        
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
        BottomSheetBehavior<androidx.cardview.widget.CardView> behavior = 
            BottomSheetBehavior.from((androidx.cardview.widget.CardView) bottomSheet);

        behavior.setHideable(false);
        navFabContainer.setVisibility(View.GONE);
    }

    private void toggleImmersiveMode() {
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

    @Override
    public void onResume() {
        super.onResume();
        if (sensorManager != null && rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
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

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed
    }
}

