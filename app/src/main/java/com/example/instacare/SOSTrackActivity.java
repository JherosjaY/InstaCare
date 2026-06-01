package com.example.instacare;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import com.google.android.material.appbar.MaterialToolbar;
import org.osmdroid.bonuspack.routing.OSRMRoadManager;
import org.osmdroid.bonuspack.routing.Road;
import org.osmdroid.bonuspack.routing.RoadManager;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;
import java.util.ArrayList;

public class SOSTrackActivity extends AppCompatActivity {

    private MapView mapView;
    private MyLocationNewOverlay myLocationOverlay;
    private Polyline roadOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Force Light Theme locally for this Activity — Monitoring is excluded from global user theme preferences
        getDelegate().setLocalNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        
        super.onCreate(savedInstanceState);
        
        // OsmDroid Configuration
        Configuration.getInstance().load(this, getPreferences(MODE_PRIVATE));
        Configuration.getInstance().setUserAgentValue(getPackageName());
        
        setContentView(R.layout.activity_sos_track);

        // Immersive status bar with dark icons (visible on light map)
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        androidx.core.view.WindowInsetsControllerCompat windowInsetsController = 
            androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (windowInsetsController != null) {
            windowInsetsController.setAppearanceLightStatusBars(true); // Black icons
        }

        mapView = findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(18.0);

        // Enable Responder Location Tracking (Blue Dot)
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView);
        myLocationOverlay.enableMyLocation();
        mapView.getOverlays().add(myLocationOverlay);

        findViewById(R.id.btnBackFloating).setOnClickListener(v -> finish());

        // Get Data from Intent
        String patientName = getIntent().getStringExtra("PATIENT_NAME");
        String emergencyType = getIntent().getStringExtra("EMERGENCY_TYPE");
        String caseRef = getIntent().getStringExtra("CASE_REF");
        String locationZone = getIntent().getStringExtra("LOCATION_ZONE");
        double lat = getIntent().getDoubleExtra("LATITUDE", 0.0);
        double lon = getIntent().getDoubleExtra("LONGITUDE", 0.0);

        // Update UI
        TextView tvType = findViewById(R.id.tvEmergencyType);
        TextView tvName = findViewById(R.id.tvPatientName);
        TextView tvCase = findViewById(R.id.tvCaseRef);
        TextView tvLoc = findViewById(R.id.tvLocationDetails);
        ImageView ivIcon = findViewById(R.id.ivTypeIcon);

        tvType.setText(emergencyType != null ? emergencyType : "Emergency");
        tvName.setText(patientName != null ? patientName : "Unknown Patient");
        tvCase.setText("Case: " + (caseRef != null ? caseRef : "N/A"));
        tvLoc.setText((locationZone != null ? locationZone : "Unknown Zone") + ", Valencia City");

        // Set Icon and Color based on type
        int iconColor = android.graphics.Color.parseColor("#EF4444");
        if ("Crime Emergency".equals(emergencyType)) iconColor = android.graphics.Color.parseColor("#3B82F6");
        if ("Disaster Response".equals(emergencyType)) iconColor = android.graphics.Color.parseColor("#64748B");
        if ("Medical Emergency".equals(emergencyType)) iconColor = android.graphics.Color.parseColor("#10B981");

        tvType.setTextColor(iconColor);
        ivIcon.setColorFilter(iconColor);

        // System Navigation Bar Polish (Dark to match the card)
        getWindow().setNavigationBarColor(android.graphics.Color.parseColor("#1A1212"));
        androidx.core.view.WindowInsetsControllerCompat controller = 
            androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightNavigationBars(false); // Light icons for dark bar
        }

        // Add Victim Marker
        GeoPoint victimPoint = new GeoPoint(lat, lon);
        Marker victimMarker = new Marker(mapView);
        victimMarker.setPosition(victimPoint);
        victimMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        victimMarker.setTitle(patientName);
        victimMarker.setSnippet(emergencyType);
        victimMarker.setIcon(getResources().getDrawable(R.drawable.vec_location_pin));
        
        // Pulsing / Blinking Effect for Marker
        android.animation.ValueAnimator pulseAnimator = android.animation.ValueAnimator.ofFloat(0.4f, 1.0f);
        pulseAnimator.setDuration(1000);
        pulseAnimator.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        pulseAnimator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        pulseAnimator.addUpdateListener(animation -> {
            float alpha = (float) animation.getAnimatedValue();
            victimMarker.setAlpha(alpha);
            mapView.invalidate();
        });
        pulseAnimator.start();

        mapView.getOverlays().add(victimMarker);
        mapView.getController().setCenter(victimPoint);

        // Define Valencia City Response Stations (Coordinates for Routing)
        GeoPoint policeStation = new GeoPoint(7.9064, 125.0934);
        GeoPoint fireStation = new GeoPoint(7.9082, 125.0921);
        GeoPoint medicalStation = new GeoPoint(7.9095, 125.0955);
        GeoPoint disasterStation = new GeoPoint(7.9070, 125.0945);
        
        GeoPoint activeStation = policeStation; // Default
        String stationTitle = "Valencia PNP Station";

        if ("Fire Department".equals(emergencyType)) {
            activeStation = fireStation;
            stationTitle = "Valencia Fire Station";
        } else if ("Medical".equals(emergencyType)) {
            activeStation = medicalStation;
            stationTitle = "Valencia City Health";
        } else if ("Disaster Response".equals(emergencyType)) {
            activeStation = disasterStation;
            stationTitle = "Valencia Disaster Res";
        }

        // Add Station Marker
        Marker stationMarker = new Marker(mapView);
        stationMarker.setPosition(activeStation);
        stationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        stationMarker.setTitle(stationTitle);
        stationMarker.setIcon(getResources().getDrawable(R.drawable.ic_shield));
        stationMarker.getIcon().setColorFilter(android.graphics.Color.parseColor("#3B82F6"), android.graphics.PorterDuff.Mode.SRC_IN);
        mapView.getOverlays().add(stationMarker);

        final GeoPoint finalStation = activeStation;

        findViewById(R.id.btnNavigate).setOnClickListener(v -> {
            android.widget.Toast.makeText(this, "Calculating Route from Station...", android.widget.Toast.LENGTH_SHORT).show();

            // Background thread for OSRM routing (From Station to Victim)
            new Thread(() -> {
                RoadManager roadManager = new OSRMRoadManager(this, getPackageName());
                ArrayList<GeoPoint> waypoints = new ArrayList<>();
                waypoints.add(finalStation); // START: Station
                waypoints.add(victimPoint); // END: Victim LIVE GPS

                Road road = roadManager.getRoad(waypoints);
                
                runOnUiThread(() -> {
                    if (road.mStatus != Road.STATUS_OK) {
                        android.widget.Toast.makeText(this, "Routing failed. Check connection.", android.widget.Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Remove old road if exists
                    if (roadOverlay != null) mapView.getOverlays().remove(roadOverlay);

                    roadOverlay = RoadManager.buildRoadOverlay(road);
                    roadOverlay.setColor(android.graphics.Color.parseColor("#2563EB")); // Primary Blue
                    roadOverlay.setWidth(14.0f);
                    
                    mapView.getOverlays().add(roadOverlay);
                    mapView.invalidate();
                    
                    android.widget.Toast.makeText(this, "Station Route Active (" + 
                        String.format("%.1f", road.mLength) + " km)", android.widget.Toast.LENGTH_SHORT).show();
                });
            }).start();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        myLocationOverlay.enableMyLocation();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        myLocationOverlay.disableMyLocation();
    }
}
