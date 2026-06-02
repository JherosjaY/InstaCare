package com.example.instacare;

import android.content.Intent;
import android.net.Uri;
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

    @Override
    public void onResume() {
        super.onResume();
        loadEndorsements();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_barangay_endorsements, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = AppDatabase.getDatabase(requireContext());

        recyclerView = view.findViewById(R.id.endorsementsList);
        emptyState = view.findViewById(R.id.emptyState);

        SessionManager sessionManager = SessionManager.getInstance(requireContext());
        int currentUserId = sessionManager.getCurrentUserUid();
        String currentUserRole = sessionManager.getString("USER_ROLE", "BARANGAY");

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new EndorsementAdapter(new ArrayList<>(), currentUserId, currentUserRole, this::showReviewDialog, this::openChat);
        recyclerView.setAdapter(adapter);

        // Chip group filtering
        ChipGroup chipGroup = view.findViewById(R.id.chipGroup);
        
        // Handle Initial Filter from Activity
        TextView tvEmptyTitle = view.findViewById(R.id.tvEmptyTitle);

        if (getArguments() != null) {
            String initialFilter = getArguments().getString("FILTER");
            if ("PENDING".equals(initialFilter)) {
                currentFilter = "PENDING";
                // Update activity toolbar title
                if (getActivity() instanceof BarangayDashboardActivity) {
                    ((BarangayDashboardActivity) getActivity()).getSupportActionBar().setTitle("Pending Requests");
                }
                if (tvEmptyTitle != null) tvEmptyTitle.setText("No pending requests");
                view.findViewById(R.id.chipPending).performClick();
            } else if ("HISTORY".equals(initialFilter)) {
                currentFilter = "HISTORY";
                // Update activity toolbar title
                if (getActivity() instanceof BarangayDashboardActivity) {
                    ((BarangayDashboardActivity) getActivity()).getSupportActionBar().setTitle("Processed Requests");
                }
                if (tvEmptyTitle != null) tvEmptyTitle.setText("No request history");
                view.findViewById(R.id.chipAll).setVisibility(View.GONE);
                view.findViewById(R.id.chipPending).setVisibility(View.GONE);
                
                com.google.android.material.chip.Chip chipApproved = view.findViewById(R.id.chipApproved);
                chipApproved.setChecked(true);
                currentFilter = "REVIEWED";
                loadEndorsements();
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
            else if (checkedIds.contains(R.id.chipApproved)) currentFilter = "REVIEWED"; // Use 'Approved' chip for active ones
            else if (checkedIds.contains(R.id.chipDeclined)) currentFilter = "DECLINED";
            loadEndorsements();
        });

        TextInputEditText etSearch = view.findViewById(R.id.etSearch);
        etSearch.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadEndorsements(s.toString());
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        // Open Drawer from internal header
        View btnMenu = view.findViewById(R.id.btnMenu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                if (getActivity() instanceof BarangayDashboardActivity) {
                    ((BarangayDashboardActivity) getActivity()).openDrawer();
                }
            });
        }

        // Only manual load if not already triggered by filter arguments
        if (getArguments() == null || !getArguments().containsKey("FILTER")) {
            loadEndorsements("");
        }
    }

    private void loadEndorsements() {
        TextInputEditText etSearch = getView() != null ? getView().findViewById(R.id.etSearch) : null;
        loadEndorsements(etSearch != null ? etSearch.getText().toString() : "");
    }

    private void loadEndorsements(String query) {
        SessionManager sessionManager = SessionManager.getInstance(requireContext());
        String selectedCommunity = sessionManager.getString("SELECTED_COMMUNITY", null);

        Executors.newSingleThreadExecutor().execute(() -> {
            List<Endorsement> endorsements;
            if (selectedCommunity != null) {
                if ("ALL".equals(currentFilter)) {
                    endorsements = db.endorsementDao().getAllManualByBarangay(selectedCommunity);
                } else if ("HISTORY".equals(currentFilter)) {
                    endorsements = db.endorsementDao().getAllByHistoryByBarangay(selectedCommunity);
                } else if ("REVIEWED".equals(currentFilter)) {
                    endorsements = db.endorsementDao().getAllProcessedByBarangay(selectedCommunity);
                } else {
                    endorsements = db.endorsementDao().getAllManualByStatusByBarangay(currentFilter, selectedCommunity);
                }
            } else {
                // Global view (Admin fallback or if no community selected)
                if ("ALL".equals(currentFilter)) {
                    endorsements = db.endorsementDao().getAllManual();
                } else if ("HISTORY".equals(currentFilter)) {
                    endorsements = db.endorsementDao().getAllByHistory();
                } else if ("REVIEWED".equals(currentFilter)) {
                    endorsements = db.endorsementDao().getAllProcessed();
                } else {
                    endorsements = db.endorsementDao().getAllManualByStatus(currentFilter);
                }
            }

            // Filter by search query
            List<Endorsement> result = endorsements;
            if (query != null && !query.isEmpty()) {
                String lowerQuery = query.toLowerCase();
                List<Endorsement> filtered = new ArrayList<>();
                for (Endorsement e : endorsements) {
                    if ((e.caseRef != null && e.caseRef.toLowerCase().contains(lowerQuery)) || 
                        (e.patientName != null && e.patientName.toLowerCase().contains(lowerQuery))) {
                        filtered.add(e);
                    }
                }
                result = filtered;
            }

            final List<Endorsement> finalResult = result;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (finalResult.isEmpty()) {
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
                        adapter.updateData(finalResult);
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

        TextView tvCaseRef = dialogView.findViewById(R.id.tvCaseRef);
        tvCaseRef.setText(endorsement.caseRef);

        TextView tvStatus = dialogView.findViewById(R.id.tvStatus);
        tvStatus.setText(endorsement.status);

        LinearLayout actionButtons = dialogView.findViewById(R.id.actionButtons);
        TextInputLayout remarksLayout = dialogView.findViewById(R.id.remarksLayout);
        TextInputEditText editRemarks = dialogView.findViewById(R.id.editRemarks);
        View resolvedContainer = dialogView.findViewById(R.id.resolvedContainer);

        // Dynamic Theme Colors for Barangay
        int brgyPrimary = getResources().getColor(R.color.barangay_primary);
        int brgyLight = getResources().getColor(R.color.secondary_light); // Assuming blueish light color
        
        tvCaseRef.setTextColor(brgyPrimary);
        tvCaseRef.setBackgroundTintList(android.content.res.ColorStateList.valueOf(brgyLight));
        
        com.google.android.material.button.MaterialButton btnViewMap = dialogView.findViewById(R.id.btnViewMap);
        brgyPrimary = getResources().getColor(R.color.barangay_primary, null);
        brgyLight = android.graphics.Color.parseColor("#152563EB"); // Very light blue for tonal background
        
        btnViewMap.setBackgroundTintList(android.content.res.ColorStateList.valueOf(brgyLight));
        btnViewMap.setIconTint(android.content.res.ColorStateList.valueOf(brgyPrimary));
        btnViewMap.setTextColor(brgyPrimary);

        switch (endorsement.status) {
            case "PENDING":
                tvStatus.setTextColor(getResources().getColor(R.color.warning_orange, null));
                actionButtons.setVisibility(View.VISIBLE);
                resolvedContainer.setVisibility(View.GONE);
                break;
            case "REVIEWED":
            case "FORWARDED":
                tvStatus.setTextColor(getResources().getColor(R.color.barangay_primary, null));
                actionButtons.setVisibility(View.VISIBLE);
                resolvedContainer.setVisibility(View.GONE);
                break;
            case "RESOLVED":
                tvStatus.setTextColor(getResources().getColor(R.color.success_green, null));
                actionButtons.setVisibility(View.GONE);
                resolvedContainer.setVisibility(View.VISIBLE);
                break;
            case "DECLINED":
                tvStatus.setTextColor(getResources().getColor(R.color.emergency_red, null));
                actionButtons.setVisibility(View.GONE);
                resolvedContainer.setVisibility(View.GONE);
                if (endorsement.adminRemarks != null && !endorsement.adminRemarks.isEmpty()) {
                    remarksLayout.setVisibility(View.VISIBLE);
                    editRemarks.setText(endorsement.adminRemarks);
                    editRemarks.setEnabled(false);
                }
                break;
        }

        // Map Button
        btnViewMap.setOnClickListener(v -> {
            String hospitalName = endorsement.hospitalName;
            if (hospitalName == null || hospitalName.isEmpty()) return;
            Intent mapIntent = new Intent(v.getContext(), MapRouteActivity.class);
            mapIntent.putExtra("TARGET_HOSPITAL_NAME", hospitalName);
            v.getContext().startActivity(mapIntent);
        });

        // Dynamic Actions based on Proposal Lifecycle
        com.google.android.material.button.MaterialButton btnMainAction = dialogView.findViewById(R.id.btnApprove);
        com.google.android.material.button.MaterialButton btnDecline = dialogView.findViewById(R.id.btnDecline);

        // Apply Blue Theme to Buttons
        btnDecline.setStrokeColor(android.content.res.ColorStateList.valueOf(brgyPrimary));
        btnDecline.setTextColor(brgyPrimary);
        btnMainAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(brgyPrimary));

        if ("PENDING".equals(endorsement.status)) {
            btnMainAction.setText("Acknowledge");
            btnMainAction.setOnClickListener(v -> updateEndorsementStatus(endorsement, "REVIEWED", "Acknowledged by Staff", dialog));
        } else if ("REVIEWED".equals(endorsement.status)) {
            btnMainAction.setText("Forward Case");
            btnMainAction.setOnClickListener(v -> updateEndorsementStatus(endorsement, "FORWARDED", "Forwarded to " + endorsement.hospitalName, dialog));
        } else if ("FORWARDED".equals(endorsement.status)) {
            btnMainAction.setText("Resolve Case");
            btnMainAction.setOnClickListener(v -> updateEndorsementStatus(endorsement, "RESOLVED", "Emergency case closed", dialog));
        }

        btnDecline.setOnClickListener(v -> {
            if (remarksLayout.getVisibility() == View.GONE) {
                remarksLayout.setVisibility(View.VISIBLE);
                editRemarks.setEnabled(true);
                editRemarks.requestFocus();
                return;
            }
            String remarks = editRemarks.getText().toString().trim();
            if (remarks.isEmpty()) {
                Toast.makeText(requireContext(), "Reason required", Toast.LENGTH_SHORT).show();
                return;
            }
            updateEndorsementStatus(endorsement, "DECLINED", remarks, dialog);
        });

        dialog.show();
    }

    private void updateEndorsementStatus(Endorsement endorsement, String newStatus, String remarks, BottomSheetDialog dialog) {
        long now = System.currentTimeMillis();
        Executors.newSingleThreadExecutor().execute(() -> {
            db.endorsementDao().updateStatus(endorsement.id, newStatus, remarks, now);
            
            // Record in Status Log (Proposal Requirement)
            db.endorsementStatusLogDao().insert(new com.example.instacare.data.local.EndorsementStatusLog(
                endorsement.id, endorsement.status, newStatus, "Staff", now
            ));

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Status updated to " + newStatus, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadEndorsements();
                });
            }
        });
    }
    private void openChat(Endorsement endorsement) {
        Intent intent = new Intent(requireContext(), ChatActivity.class);
        intent.putExtra("ENDORSEMENT_ID", endorsement.id);
        intent.putExtra("OTHER_USER_ID", endorsement.userId);
        intent.putExtra("CASE_REF", endorsement.caseRef);
        intent.putExtra("CONVERSATION_ROLE", "BARANGAY");
        startActivity(intent);
    }
}
