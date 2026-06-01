package com.example.instacare;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.instacare.data.local.AppDatabase;
import com.example.instacare.databinding.ActivitySosBinding;
import com.example.instacare.utils.CaseRefGenerator;
import com.example.instacare.utils.ZoneLookupHelper;

public class SOSActivity extends AppCompatActivity {

    private ActivitySosBinding binding;
    private String currentNumberToCall = null;
    private static final int CALL_PERMISSION_REQUEST_CODE = 2;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 3;

    private String emergencyLabel = "GENERAL SOS";
    private String emergencyCategory = "General";
    private boolean isResolvingGPS = false; 
    private boolean isSOSTriggered = false; // Prevent multiple logs per session


    @Override
    protected void onResume() {
        super.onResume();
        // Automatically check and fetch if user just returned from settings
        // Only if we're not currently waiting for a dialog result
        if (!isResolvingGPS && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            checkGPSStatusAndFetch();
        }
    }

    private void checkGPSStatusAndFetch() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // Use Google Play Services Settings API for "Automatic/One-tap" Enable
        com.google.android.gms.location.LocationRequest locationRequest = com.google.android.gms.location.LocationRequest.create()
                .setPriority(com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY);

        com.google.android.gms.location.LocationSettingsRequest.Builder builder = new com.google.android.gms.location.LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        com.google.android.gms.location.SettingsClient client = com.google.android.gms.location.LocationServices.getSettingsClient(this);
        com.google.android.gms.tasks.Task<com.google.android.gms.location.LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(this, locationSettingsResponse -> {
            // All location settings are satisfied. The client can initialize location requests here.
            fetchLocationAndTrigger();
        });

        task.addOnFailureListener(this, e -> {
            if (e instanceof com.google.android.gms.common.api.ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed by showing the user a dialog.
                try {
                    isResolvingGPS = true; // Set flag before starting resolution
                    // Show the dialog by calling startResolutionForResult(), and check the result in onActivityResult().
                    com.google.android.gms.common.api.ResolvableApiException resolvable = (com.google.android.gms.common.api.ResolvableApiException) e;
                    resolvable.startResolutionForResult(SOSActivity.this, 1001);
                } catch (android.content.IntentSender.SendIntentException sendEx) {
                    isResolvingGPS = false;
                }
            } else {
                // Location settings are not satisfied, and no way to fix it.
                finish();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001) {
            isResolvingGPS = false; // Reset flag after user returns from dialog
            if (resultCode == RESULT_OK) {
                // User agreed to make required location settings changes.
                fetchLocationAndTrigger();
            } else {
                // User chose not to make required location settings changes.
                finish();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Immersive UI
        getWindow().setStatusBarColor(android.graphics.Color.parseColor("#EF4444"));
        getWindow().setNavigationBarColor(ContextCompat.getColor(this, R.color.sos_background));
        // Ensure status bar icons are white on the red background
        getWindow().getDecorView().setSystemUiVisibility(0);

        // Get Extracted Category
        if (getIntent().hasExtra("EMERGENCY_LABEL")) {
            emergencyLabel = getIntent().getStringExtra("EMERGENCY_LABEL");
        }
        if (getIntent().hasExtra("EMERGENCY_CATEGORY")) {
            emergencyCategory = getIntent().getStringExtra("EMERGENCY_CATEGORY");
        }

        setupUI();

        binding.btnImSafe.setOnClickListener(v -> finish());
        
        populateHotlines();

        // Check Location Permission and Trigger SOS
        checkGPSStatusAndFetch();
    }
    
    private void setupUI() {
        binding.tvSosHeader.setText("SOS ACTIVE");
        binding.chipEmergencyType.setText(emergencyLabel);
        
        // Dynamic Icon/Color for Chip based on selection
        int iconRes = R.drawable.ic_baseline_warning_24;
        
        if (emergencyCategory != null) {
            switch (emergencyCategory) {
                case "Police":
                    iconRes = R.drawable.ic_shield;
                    break;
                case "Fire Department":
                    iconRes = R.drawable.ic_fire;
                    break;
                case "Medical":
                    iconRes = R.drawable.ic_heart_pulse;
                    break;
                case "Disaster Response":
                    iconRes = R.drawable.ic_storm;
                    break;
            }
        }
        
        binding.chipEmergencyType.setIconResource(iconRes);
        
        // Define exact fallback colors matching the UI design specs (Natural colors)
        int resolvedColor = android.graphics.Color.parseColor("#1E293B"); // Default Dark
        if ("Police".equals(emergencyCategory)) resolvedColor = android.graphics.Color.parseColor("#3B82F6"); // Blue-500 for text/icon visibility
        if ("Fire Department".equals(emergencyCategory)) resolvedColor = android.graphics.Color.parseColor("#F97316"); // Orange
        if ("Medical".equals(emergencyCategory)) resolvedColor = android.graphics.Color.parseColor("#EF4444"); // Red 
        if ("Disaster Response".equals(emergencyCategory)) resolvedColor = android.graphics.Color.parseColor("#64748B"); // Slate/Gray

        binding.chipEmergencyType.setIconTint(android.content.res.ColorStateList.valueOf(resolvedColor));
        binding.chipEmergencyType.setTextColor(resolvedColor);
    }

    @SuppressLint("MissingPermission")
    private void fetchLocationAndTrigger() {
        if (isSOSTriggered) return; // Exit if already fetching or triggered
        
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            triggerSOSFlow(7.9120, 125.0950);
            return;
        }

        final boolean[] locationFound = {false};
        android.os.Handler timeoutHandler = new android.os.Handler(android.os.Looper.getMainLooper());

        android.location.LocationListener listener = new android.location.LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                if (!locationFound[0]) {
                    locationFound[0] = true;
                    locationManager.removeUpdates(this);
                    timeoutHandler.removeCallbacksAndMessages(null);
                    triggerSOSFlow(location.getLatitude(), location.getLongitude());
                }
            }
            @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
            @Override public void onProviderEnabled(@NonNull String provider) {}
            @Override public void onProviderDisabled(@NonNull String provider) {}
        };

        // Attempt GPS (High Accuracy) and Network (Fast Fallback)
        // Removed Toast for cleaner UI
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, listener);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, listener);

        // 5-Second Timeout for GPS Fix (Fail-safe for rapid response)
        timeoutHandler.postDelayed(() -> {
            if (!locationFound[0]) {
                locationFound[0] = true;
                locationManager.removeUpdates(listener);
                Location lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnown == null) lastKnown = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                
                if (lastKnown != null) {
                    triggerSOSFlow(lastKnown.getLatitude(), lastKnown.getLongitude());
                } else {
                    // Critical Fallback to city center if all else fails
                    triggerSOSFlow(7.9120, 125.0950);
                }
            }
        }, 5000);
    }

    private void triggerSOSFlow(double lat, double lon) {
        if (isSOSTriggered) return;
        isSOSTriggered = true;

        SessionManager sessionManager = SessionManager.getInstance(this);
        int userId = sessionManager.getCurrentUserUid();
        String username = sessionManager.getString("USER_USERNAME", "");

        if (userId == 0 && username.isEmpty()) return;
        
        new Thread(() -> {
            AppDatabase db = AppDatabase.getDatabase(this);
            
            // Deduplication Logic: Check if an alert was already triggered for this user in the last 15 seconds
            com.example.instacare.data.local.EmergencyAlert latest = db.emergencyAlertDao().getLatestAlertForUser(userId);
            if (latest != null) {
                long diff = System.currentTimeMillis() - latest.timestamp;
                if (diff < 15000) { // 15 seconds threshold
                    return; 
                }
            }
            
            com.example.instacare.data.local.User userProfile = db.userDao().getUserByUsername(username);
            String patientName = (userProfile != null) ? userProfile.fullName : "User (" + username + ")";

            // Lookup Zone based on real GPS
            java.util.List<com.example.instacare.data.local.BarangayZone> zones = db.barangayZoneDao().getAllZones();
            String matchedZone = ZoneLookupHelper.findZone(lat, lon, zones);

            // Generate Case Reference Number
            String caseRef = CaseRefGenerator.generate();

            // Create Emergency Alert (Admin/Responder ONLY)
            com.example.instacare.data.local.EmergencyAlert alert = new com.example.instacare.data.local.EmergencyAlert(
                userId, patientName, emergencyCategory, lat, lon, matchedZone, caseRef, System.currentTimeMillis(), "TRIGGERED"
            );
            long alertId = db.emergencyAlertDao().insert(alert);

            // Notify Contacts (Log notification)
            db.alertNotificationDao().insert(new com.example.instacare.data.local.AlertNotification(
                (int)alertId, "Emergency Contacts", "SENT", System.currentTimeMillis()
            ));

            // LOG ACTIVITY for Monitoring & Audit (Correct Connection)
            db.activityLogDao().insert(new com.example.instacare.data.local.ActivityLog(
                "SOS",
                "SOS triggered by " + patientName + " (Ref: " + caseRef + ")",
                userId,
                emergencyCategory,
                matchedZone,
                System.currentTimeMillis()
            ));

            runOnUiThread(() -> {
                binding.tvSosCaseRef.setText("Case: " + caseRef);
            });
        }).start();
    }
    
    private void populateHotlines() {
        android.widget.LinearLayout container = binding.hotlinesContainer;
        if (container == null) return;
        
        // Only populate the requested category. showHeaders=false to prevent redundant labels
        HotlineHelper.populateCategorized(this, container, emergencyCategory, false, number -> makeCall(number));
    }

    private void makeCall(String number) {
        currentNumberToCall = number;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, CALL_PERMISSION_REQUEST_CODE);
        } else {
            performCall(number);
        }
    }

    private void performCall(String number) {
        if (number == null || number.isEmpty()) return;
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + number));
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CALL_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            performCall(currentNumberToCall);
        } else if (requestCode == CALL_PERMISSION_REQUEST_CODE) {
            Toast.makeText(this, "Permission denied. Cannot make calls.", Toast.LENGTH_SHORT).show();
        } else if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            fetchLocationAndTrigger();
        }
    }
}
