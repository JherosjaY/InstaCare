package com.example.instacare;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.view.LayoutInflater;
import android.widget.TextView;
import android.widget.Toast;

import com.example.instacare.data.local.AppDatabase;
import com.example.instacare.data.local.Endorsement;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class MyEndorsementsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private EndorsementAdapter adapter;
    private AppDatabase db;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_endorsements);

        // Adaptive status bar
        boolean isDark = (getResources().getConfiguration().uiMode &
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES;
        androidx.core.view.WindowInsetsControllerCompat controller =
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(!isDark);
        }

        db = AppDatabase.getDatabase(this);
        SharedPreferences prefs = getSharedPreferences("InstaCarePrefs", Context.MODE_PRIVATE);
        userEmail = prefs.getString("USER_USERNAME", ""); // Refactored: using username as unique ID internally

        recyclerView = findViewById(R.id.endorsementsList);
        emptyState = findViewById(R.id.emptyState);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EndorsementAdapter(new ArrayList<>(), this::showDetailDialog);
        recyclerView.setAdapter(adapter);

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // New request button
        findViewById(R.id.btnNewRequest).setOnClickListener(v -> {
            EndorsementRequestSheet sheet = new EndorsementRequestSheet();
            sheet.setOnEndorsementSubmittedListener(this::loadEndorsements);
            sheet.show(getSupportFragmentManager(), "endorsement_request");
        });

        loadEndorsements();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEndorsements();
    }

    private void loadEndorsements() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Endorsement> endorsements = db.endorsementDao().getAllByUsername(userEmail);
            runOnUiThread(() -> {
                if (endorsements.isEmpty()) {
                    emptyState.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyState.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    adapter.updateData(endorsements);
                }
            });
        });
    }

    private void showDetailDialog(Endorsement endorsement) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_endorsement_review, null);
        dialog.setContentView(dialogView);

        // Update Title for User View
        TextView tvTitle = dialogView.findViewById(R.id.tvReviewTitle);
        if (tvTitle != null) {
            tvTitle.setText("Endorsement Details");
        }

        // Populate detail fields
        ((TextView) dialogView.findViewById(R.id.tvPatientName)).setText(endorsement.patientName);
        ((TextView) dialogView.findViewById(R.id.tvPurpose)).setText(endorsement.purpose);
        ((TextView) dialogView.findViewById(R.id.tvHospital)).setText(endorsement.hospitalName);
        ((TextView) dialogView.findViewById(R.id.tvAddress)).setText(
                endorsement.address != null && !endorsement.address.isEmpty() ? endorsement.address : "Not provided");
        ((TextView) dialogView.findViewById(R.id.tvReason)).setText(endorsement.reason);

        TextView tvStatus = dialogView.findViewById(R.id.tvStatus);
        tvStatus.setText(endorsement.status);

        // Admin-only actions should be hidden
        dialogView.findViewById(R.id.actionButtons).setVisibility(View.GONE);
        
        // Handle Status Coloring and Admin Remarks
        View remarksLayout = dialogView.findViewById(R.id.remarksLayout);
        TextView editRemarks = dialogView.findViewById(R.id.editRemarks);

        switch (endorsement.status) {
            case "PENDING":
                tvStatus.setTextColor(getResources().getColor(R.color.warning_orange, null));
                break;
            case "APPROVED":
                tvStatus.setTextColor(getResources().getColor(R.color.success_green, null));
                break;
            case "DECLINED":
                tvStatus.setTextColor(getResources().getColor(R.color.emergency_red, null));
                // Show admin remarks if declined
                if (endorsement.adminRemarks != null && !endorsement.adminRemarks.isEmpty()) {
                    remarksLayout.setVisibility(View.VISIBLE);
                    editRemarks.setText(endorsement.adminRemarks);
                    editRemarks.setEnabled(false);
                }
                break;
        }

        dialog.show();
    }
}
