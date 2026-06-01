package com.example.instacare;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.net.Uri;
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

import android.graphics.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class MyEndorsementsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private EndorsementAdapter adapter;
    private AppDatabase db;
    private int userId;

    private String selectedFilter = "ALL";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_endorsements);

        // Edge-to-Edge
        boolean isDark = (getResources().getConfiguration().uiMode &
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES;
        androidx.core.view.WindowInsetsControllerCompat controller =
                androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(!isDark);
        }

        db = AppDatabase.getDatabase(this);
        SessionManager sessionManager = SessionManager.getInstance(this);
        userId = sessionManager.getCurrentUserUid();
        String userRole = sessionManager.getString("USER_ROLE", "USER");

        recyclerView = findViewById(R.id.endorsementsList);
        emptyState = findViewById(R.id.emptyState);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EndorsementAdapter(new ArrayList<>(), userId, userRole, this::showDetailDialog, this::openChat);
        recyclerView.setAdapter(adapter);

        // --- Filters Logic ---
        com.google.android.material.chip.ChipGroup filterGroup = findViewById(R.id.chipGroupEndorsementFilter);
        filterGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipFilterMedical) selectedFilter = "MEDICAL";
            else if (id == R.id.chipFilterEvac) selectedFilter = "EVAC";
            else selectedFilter = "ALL";
            loadEndorsements();
        });

        // --- Premium Branding ---
        TextView tvHeader = findViewById(R.id.tvHeaderTitle);
        if (tvHeader != null) {
            String headerText = "My <font color='#E53935'>Endorsements</font>";
            tvHeader.setText(android.text.Html.fromHtml(headerText, android.text.Html.FROM_HTML_MODE_LEGACY));
        }

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // New request button
        // New request button
        findViewById(R.id.btnNewRequest).setOnClickListener(v -> {
            EndorsementRequestSheet sheet = new EndorsementRequestSheet();
            sheet.setOnEndorsementSubmittedListener(this::loadEndorsements);
            sheet.show(getSupportFragmentManager(), "endorsement_request");
        });

        // --- Initial Filter State ---
        String startFilter = getIntent().getStringExtra("START_FILTER");
        if ("EVAC".equals(startFilter)) {
            selectedFilter = "EVAC";
            filterGroup.check(R.id.chipFilterEvac);
        } else if ("MEDICAL".equals(startFilter)) {
            selectedFilter = "MEDICAL";
            filterGroup.check(R.id.chipFilterMedical);
        }

        loadEndorsements();
    }

    private void loadEndorsements() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Endorsement> medicalList = new ArrayList<>();
            List<com.example.instacare.data.local.EvacuationEndorsement> evacList = new ArrayList<>();

            if (selectedFilter.equals("ALL") || selectedFilter.equals("MEDICAL")) {
                medicalList = db.endorsementDao().getAllManualByUserId(userId);
            }
            if (selectedFilter.equals("ALL") || selectedFilter.equals("EVAC")) {
                evacList = db.evacuationEndorsementDao().getAllByUserId(userId);
            }

            // Convert and Merge
            List<Endorsement> unifiedList = new ArrayList<>(medicalList);
            for (com.example.instacare.data.local.EvacuationEndorsement evac : evacList) {
                // Map Evacuation fields to Endorsement fields for display
                Endorsement pseudo = new Endorsement(
                    evac.userId, evac.patientName, evac.address,
                    evac.disasterType, evac.assignedCenterName != null ? evac.assignedCenterName : "Pending Assignment",
                    evac.reason, evac.status, evac.caseRef, evac.barangayZone,
                    0, evac.createdAt, evac.updatedAt
                );
                pseudo.id = evac.id;
                // Add a marker for evacuation type (e.g. via reason prefix or a new transient field if I could, but I'll use purpose)
                // purpose was already set to evac.disasterType
                unifiedList.add(pseudo);
            }

            // Sort by createdAt DESC
            java.util.Collections.sort(unifiedList, (a, b) -> Long.compare(b.createdAt, a.createdAt));

            List<Endorsement> finalUnifiedList = unifiedList;
            runOnUiThread(() -> {
                if (finalUnifiedList.isEmpty()) {
                    emptyState.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyState.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    adapter.updateData(finalUnifiedList);
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
        
        // Proposal Alignment: Case Ref & Zone
        ((TextView) dialogView.findViewById(R.id.tvCaseRef)).setText(endorsement.caseRef != null ? endorsement.caseRef : "N/A");
        ((TextView) dialogView.findViewById(R.id.tvZoneLabel)).setText(endorsement.barangayZone != null ? endorsement.barangayZone : "Pending Zone");

        TextView tvStatus = dialogView.findViewById(R.id.tvStatus);
        tvStatus.setText(endorsement.status);

        // Admin-only actions should be hidden
        dialogView.findViewById(R.id.actionButtons).setVisibility(View.GONE);
        
        // Map Button
        dialogView.findViewById(R.id.btnViewMap).setOnClickListener(v -> {
            String hospitalName = endorsement.hospitalName;
            if (hospitalName == null || hospitalName.isEmpty()) return;
            Intent mapIntent = new Intent(v.getContext(), MapRouteActivity.class);
            mapIntent.putExtra("TARGET_HOSPITAL_NAME", hospitalName);
            v.getContext().startActivity(mapIntent);
        });
        
        // Handle Status Coloring and Admin Remarks
        View remarksLayout = dialogView.findViewById(R.id.remarksLayout);
        TextView editRemarks = dialogView.findViewById(R.id.editRemarks);
        
        // Role-Based Button Coloring (Red for User)
        com.google.android.material.button.MaterialButton btnViewMap = dialogView.findViewById(R.id.btnViewMap);
        int primaryColor = getResources().getColor(R.color.primary, null);
        int primaryLight = Color.parseColor("#15D32F2F"); // Very light red for tonal button background
        
        btnViewMap.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primaryLight));
        btnViewMap.setIconTint(android.content.res.ColorStateList.valueOf(primaryColor));
        btnViewMap.setTextColor(primaryColor);

        switch (endorsement.status) {
            case "PENDING":
                tvStatus.setTextColor(getResources().getColor(R.color.warning_orange, null));
                break;
            case "REVIEWED":
            case "FORWARDED":
                tvStatus.setTextColor(android.graphics.Color.parseColor("#2563EB")); // Blue for in-progress
                break;
            case "RESOLVED":
                tvStatus.setTextColor(getResources().getColor(R.color.success_green, null));
                break;
            case "DECLINED":
                tvStatus.setTextColor(getResources().getColor(R.color.emergency_red, null));
                if (endorsement.adminRemarks != null && !endorsement.adminRemarks.isEmpty()) {
                    remarksLayout.setVisibility(View.VISIBLE);
                    editRemarks.setText(endorsement.adminRemarks);
                    editRemarks.setEnabled(false);
                }
                break;
        }

        dialog.show();
    }
    private void openChat(Endorsement endorsement) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("ENDORSEMENT_ID", endorsement.id);
        intent.putExtra("OTHER_USER_ID", 1000); // Default receiver (Barangay Staff)
        intent.putExtra("CASE_REF", endorsement.caseRef);
        intent.putExtra("CONVERSATION_ROLE", "BARANGAY");
        startActivity(intent);
    }
}
