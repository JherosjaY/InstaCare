package com.example.instacare;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class MapRouteActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_route);

        // Status bar matching header
        android.view.Window window = getWindow();
        window.setStatusBarColor(android.graphics.Color.parseColor("#121212"));
        window.getDecorView().setSystemUiVisibility(0);

        com.google.android.material.card.MaterialCardView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        android.widget.TextView tvDest = findViewById(R.id.tvRouteDestination);
        android.widget.TextView tvDist = findViewById(R.id.tvRouteDistance);
        android.widget.TextView tvEta = findViewById(R.id.tvRouteETA);

        String targetHospital = getIntent().getStringExtra("TARGET_HOSPITAL_NAME");
        if (targetHospital != null) {
            tvDest.setText(targetHospital);
        }

        // 🛰️ Listen for Route Updates from Fragment
        getSupportFragmentManager().setFragmentResultListener("route_update", this, (requestKey, result) -> {
            double length = result.getDouble("DISTANCE", 0.0);
            double duration = result.getDouble("DURATION", 0.0);
            String dest = result.getString("DESTINATION", targetHospital);

            // Distance formatted via centralized utility
            String distStr = com.example.instacare.utils.DistanceUtils.formatDistance(length);
            tvDist.setText(distStr);

            // Duration in minutes
            int mins = (int) (duration / 60.0);
            if (mins < 1) tvEta.setText("< 1 min");
            else if (mins > 60) tvEta.setText((mins / 60) + "h " + (mins % 60) + "m");
            else tvEta.setText(mins + " mins");

            if (dest != null) tvDest.setText(dest);
        });

        if (savedInstanceState == null) {
            HospitalsFragment hf = new HospitalsFragment();
            if (targetHospital != null) {
                Bundle args = new Bundle();
                args.putString("TARGET_HOSPITAL_NAME", targetHospital);
                args.putBoolean("IS_ROUTE_ONLY", true);
                hf.setArguments(args);
            }
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, hf)
                .commit();
        }
    }
}
