package com.example.instacare;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import com.example.instacare.data.local.AppDatabase;
import com.example.instacare.data.local.Endorsement;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class BarangayEndorsementsFragment extends Fragment {

    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private EndorsementAdapter adapter;
    private AppDatabase db;
    private String currentFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_endorsements, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = AppDatabase.getDatabase(requireContext());

        recyclerView = view.findViewById(R.id.endorsementsList);
        emptyState = view.findViewById(R.id.emptyState);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new EndorsementAdapter(new ArrayList<>(), this::showReviewDialog);
        recyclerView.setAdapter(adapter);

        // Chip group filtering
        ChipGroup chipGroup = view.findViewById(R.id.chipGroup);
        
        // Handle Initial Filter from Activity
        TextView tvTitle = view.findViewById(R.id.tvHeaderTitle);
        TextView tvSubtitle = view.findViewById(R.id.tvHeaderSubtitle);
        TextView tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle);

        if (getArguments() != null) {
            String initialFilter = getArguments().getString("FILTER");
            if ("PENDING".equals(initialFilter)) {
                currentFilter = "PENDING";
                tvTitle.setText("Pending Service Requests");
                tvSubtitle.setText("Process community service requests");
                tvEmptyTitle.setText("No pending requests");
                view.findViewById(R.id.chipPending).performClick();
            } else if ("HISTORY".equals(initialFilter)) {
                currentFilter = "HISTORY";
                tvTitle.setText("Processed Requests");
                tvSubtitle.setText("View processed service requests");
                tvEmptyTitle.setText("No request history");
                view.findViewById(R.id.chipAll).setVisibility(View.GONE);
                view.findViewById(R.id.chipPending).setVisibility(View.GONE);
                view.findViewById(R.id.chipApproved).performClick();
            }
        }

        // Set blue tint for Barangay empty icon
        android.widget.ImageView ivEmpty = view.findViewById(R.id.ivEmptyIcon);
        if (ivEmpty != null) {
            ivEmpty.setImageTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.secondary_light, null)));
        }
        
        // Apply blue role colors and ripples to chips
        int activeColor = android.graphics.Color.parseColor("#2563EB");
        int inactiveColor = android.graphics.Color.parseColor("#F1F5F9");
        int rippleColor = android.graphics.Color.parseColor("#302563EB"); // Semi-transparent blue
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            com.google.android.material.chip.Chip chip = (com.google.android.material.chip.Chip) chipGroup.getChildAt(i);
            int[][] states = new int[][]{new int[]{android.R.attr.state_checked}, new int[]{}};
            
            // Background
            int[] colors = new int[]{activeColor, inactiveColor};
            chip.setChipBackgroundColor(new android.content.res.ColorStateList(states, colors));
            
            // Stroke
            chip.setChipStrokeColor(new android.content.res.ColorStateList(states, colors));
            
            // Ripple (Fix red long-press)
            chip.setRippleColor(android.content.res.ColorStateList.valueOf(rippleColor));
            
            // Text
            int[] textColors = new int[]{android.graphics.Color.WHITE, android.graphics.Color.parseColor("#64748B")};
            chip.setTextColor(new android.content.res.ColorStateList(states, textColors));
        }

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipAll)) currentFilter = "ALL";
            else if (checkedIds.contains(R.id.chipPending)) currentFilter = "PENDING";
            else if (checkedIds.contains(R.id.chipApproved)) currentFilter = "APPROVED";
            else if (checkedIds.contains(R.id.chipDeclined)) currentFilter = "DECLINED";
            loadEndorsements();
        });

        loadEndorsements();
    }

    private void loadEndorsements() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Endorsement> endorsements;
            if ("ALL".equals(currentFilter)) {
                endorsements = db.endorsementDao().getAll();
            } else if ("HISTORY".equals(currentFilter)) {
                // Show both Approved and Declined
                endorsements = db.endorsementDao().getAllByHistory(); 
            } else {
                endorsements = db.endorsementDao().getAllByStatus(currentFilter);
            }

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (endorsements.isEmpty()) {
                        emptyState.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                        
                        // Update empty text based on filter
                        TextView tvEmpty = emptyState.findViewById(R.id.emptyStateSubtitle);
                        if ("PENDING".equals(currentFilter)) {
                            tvEmpty.setText("No pending requests to review");
                        } else {
                            tvEmpty.setText("No requests match the current filter");
                        }
                    } else {
                        emptyState.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        adapter.updateData(endorsements);
                    }
                });
            }
        });
    }

    private void showReviewDialog(Endorsement endorsement) {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_endorsement_review, null);
        dialog.setContentView(dialogView);

        // Populate detail fields
        ((TextView) dialogView.findViewById(R.id.tvPatientName)).setText(endorsement.patientName);
        ((TextView) dialogView.findViewById(R.id.tvPurpose)).setText(endorsement.purpose);
        ((TextView) dialogView.findViewById(R.id.tvHospital)).setText(endorsement.hospitalName);
        ((TextView) dialogView.findViewById(R.id.tvAddress)).setText(
                endorsement.address != null && !endorsement.address.isEmpty() ? endorsement.address : "Not provided");
        ((TextView) dialogView.findViewById(R.id.tvReason)).setText(endorsement.reason);

        TextView tvStatus = dialogView.findViewById(R.id.tvStatus);
        tvStatus.setText(endorsement.status);

        LinearLayout actionButtons = dialogView.findViewById(R.id.actionButtons);
        TextInputLayout remarksLayout = dialogView.findViewById(R.id.remarksLayout);
        TextInputEditText editRemarks = dialogView.findViewById(R.id.editRemarks);

        switch (endorsement.status) {
            case "PENDING":
                tvStatus.setTextColor(getResources().getColor(R.color.warning_orange, null));
                actionButtons.setVisibility(View.VISIBLE);
                break;
            case "APPROVED":
                tvStatus.setTextColor(getResources().getColor(R.color.success_green, null));
                actionButtons.setVisibility(View.GONE);
                break;
            case "DECLINED":
                tvStatus.setTextColor(getResources().getColor(R.color.emergency_red, null));
                actionButtons.setVisibility(View.GONE);
                if (endorsement.adminRemarks != null && !endorsement.adminRemarks.isEmpty()) {
                    remarksLayout.setVisibility(View.VISIBLE);
                    editRemarks.setText(endorsement.adminRemarks);
                    editRemarks.setEnabled(false);
                }
                break;
        }

        // Approve button
        dialogView.findViewById(R.id.btnApprove).setOnClickListener(v -> {
            long now = System.currentTimeMillis();
            Executors.newSingleThreadExecutor().execute(() -> {
                // Track that it was approved by BARANGAY staff
                db.endorsementDao().updateStatus(endorsement.id, "APPROVED", "Approved by Barangay Staff", now);
                
                // Also add an Activity Log
                db.activityLogDao().insert(new com.example.instacare.data.local.ActivityLog(
                        "ENDORSEMENT_ACTION",
                        "Barangay Staff approved endorsement for " + endorsement.patientName,
                        "staff@barangay.gov.ph",
                        "Barangay",
                        now
                ));

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Endorsement approved ✓", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadEndorsements();
                    });
                }
            });
        });

        // Decline button
        dialogView.findViewById(R.id.btnDecline).setOnClickListener(v -> {
            if (remarksLayout.getVisibility() == View.GONE) {
                remarksLayout.setVisibility(View.VISIBLE);
                editRemarks.setEnabled(true);
                editRemarks.requestFocus();
                return;
            }

            String remarks = editRemarks.getText() != null ? editRemarks.getText().toString().trim() : "";
            if (remarks.isEmpty()) {
                Toast.makeText(requireContext(), "Please provide a reason for declining", Toast.LENGTH_SHORT).show();
                return;
            }

            long now = System.currentTimeMillis();
            Executors.newSingleThreadExecutor().execute(() -> {
                db.endorsementDao().updateStatus(endorsement.id, "DECLINED", "(Barangay Staff): " + remarks, now);
                
                db.activityLogDao().insert(new com.example.instacare.data.local.ActivityLog(
                        "ENDORSEMENT_ACTION",
                        "Barangay Staff declined endorsement for " + endorsement.patientName,
                        "staff@barangay.gov.ph",
                        "Barangay",
                        now
                ));

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Endorsement declined", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                        loadEndorsements();
                    });
                }
            });
        });

        dialog.show();
    }
}
