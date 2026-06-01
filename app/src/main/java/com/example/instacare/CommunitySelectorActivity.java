package com.example.instacare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.data.local.AppDatabase;
import com.example.instacare.data.local.BarangayZone;
import java.util.List;
import java.util.concurrent.Executors;

public class CommunitySelectorActivity extends AppCompatActivity implements BarangayAdapter.OnBarangayClickListener {

    private RecyclerView recyclerView;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Force Light Theme locally for this Activity
        getDelegate().setLocalNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_selector);

        // Immersive status bar
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

        db = AppDatabase.getDatabase(this);
        recyclerView = findViewById(R.id.communityRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnLogout).setOnClickListener(v -> handleLogout());

        loadBarangays();
    }

    private void handleLogout() {
        SessionManager.getInstance(this).endSession();
        
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void loadBarangays() {
        View emptyState = findViewById(R.id.emptyState);
        Executors.newSingleThreadExecutor().execute(() -> {
            List<BarangayZone> barangays = db.barangayZoneDao().getAllZones();
            runOnUiThread(() -> {
                if (barangays == null || barangays.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    if (emptyState != null) emptyState.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    if (emptyState != null) emptyState.setVisibility(View.GONE);
                    recyclerView.setAdapter(new BarangayAdapter(barangays, this));
                }
            });
        });
    }

    @Override
    public void onBarangayClick(BarangayZone barangay) {
        showPinDialog(barangay);
    }

    private void showPinDialog(BarangayZone barangay) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_pin_entry, null);
        EditText pinEditText = dialogView.findViewById(R.id.pinEditText);
        
        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomAlertDialog)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        dialogView.findViewById(R.id.btnVerifyPin).setOnClickListener(v -> {
            String enteredPin = pinEditText.getText().toString();
            if (enteredPin.equals(barangay.accessPin)) {
                saveCommunityAndProceed(barangay);
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Incorrect PIN. Access Denied.", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void saveCommunityAndProceed(BarangayZone barangay) {
        SessionManager sessionManager = SessionManager.getInstance(this);
        sessionManager.putString("SELECTED_COMMUNITY", barangay.name);
        sessionManager.putBoolean("IS_COMMUNITY_AUTHED", true);

        // Log the community entry for isolated audit trail
        Executors.newSingleThreadExecutor().execute(() -> {
            int userId = SessionManager.getInstance(this).getCurrentUserUid();
            com.example.instacare.data.local.ActivityLog log = new com.example.instacare.data.local.ActivityLog(
                    "COMMUNITY_AUTH",
                    "Staff authenticated for community: " + barangay.name,
                    userId,
                    null,
                    barangay.name,
                    System.currentTimeMillis()
            );
            db.activityLogDao().insert(log);
        });

        Intent intent = new Intent(this, BarangayDashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
