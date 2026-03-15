package com.example.instacare;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.data.local.ActivityLog;
import com.example.instacare.data.local.AppDatabase;
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.List;

public class AdminMonitoringFragment extends Fragment {

    private RecyclerView rvLogs;
    private AdminLogsAdapter adapter;
    private TextView tvTotalLogs, tvSosLogs, tvLogCount;
    private TextView tvRankPolice, tvRankFire, tvRankMedical, tvRankDisaster;
    private android.widget.LinearLayout userRankingContainer;
    private TextView tvNoUserActivity;
    private View emptyState;
    private String currentLogFilter = "ALL";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_monitoring, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        tvTotalLogs = view.findViewById(R.id.tvTotalLogs);
        tvSosLogs = view.findViewById(R.id.tvSosLogs);
        tvLogCount = view.findViewById(R.id.tvLogCount);
        
        tvRankPolice = view.findViewById(R.id.tvRankCountPolice);
        tvRankFire = view.findViewById(R.id.tvRankCountFire);
        tvRankMedical = view.findViewById(R.id.tvRankCountMedical);
        tvRankDisaster = view.findViewById(R.id.tvRankCountDisaster);
        
        userRankingContainer = view.findViewById(R.id.userRankingContainer);
        tvNoUserActivity = view.findViewById(R.id.tvNoUserActivity);
        
        rvLogs = view.findViewById(R.id.rvLogs);
        emptyState = view.findViewById(R.id.emptyStateLogs);

        adapter = new AdminLogsAdapter(new ArrayList<>());
        rvLogs.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvLogs.setAdapter(adapter);

        // Chip group filtering for audit chronology
        ChipGroup chipGroup = view.findViewById(R.id.chipGroupLogs);
        
        // Apply role-based colors
        int activeColor;
        boolean isBarangay = getActivity() instanceof BarangayDashboardActivity;
        if (isBarangay) {
            activeColor = android.graphics.Color.parseColor("#2563EB"); // Blue
        } else {
            activeColor = android.graphics.Color.parseColor("#DC2626"); // Red
        }
        
        int rippleColor = isBarangay ? android.graphics.Color.parseColor("#302563EB") : android.graphics.Color.parseColor("#30DC2626");
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            com.google.android.material.chip.Chip chip = (com.google.android.material.chip.Chip) chipGroup.getChildAt(i);
            int[][] chipStates = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{}
            };
            int[] chipColors = new int[]{activeColor, android.graphics.Color.parseColor("#F1F5F9")};
            chip.setChipBackgroundColor(new android.content.res.ColorStateList(chipStates, chipColors));
            
            // Ripple effect matching role
            chip.setRippleColor(android.content.res.ColorStateList.valueOf(rippleColor));
            
            int[] chipTextColors = new int[]{android.graphics.Color.WHITE, android.graphics.Color.parseColor("#64748B")};
            chip.setTextColor(new android.content.res.ColorStateList(chipStates, chipTextColors));
        }

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipLogAll)) currentLogFilter = "ALL";
            else if (checkedIds.contains(R.id.chipLogLogin)) currentLogFilter = "LOGIN";
            else if (checkedIds.contains(R.id.chipLogRegister)) currentLogFilter = "REGISTER";
            else if (checkedIds.contains(R.id.chipLogSos)) currentLogFilter = "SOS";
            else if (checkedIds.contains(R.id.chipLogBroadcast)) currentLogFilter = "BROADCAST";
            loadFilteredLogs();
        });

        loadLogs();
    }

    private void loadLogs() {
        AppDatabase db = AppDatabase.getDatabase(requireContext());
        new Thread(() -> {
            List<ActivityLog> logs = db.activityLogDao().getAllLogsSync();
            if (logs == null) logs = new ArrayList<>();

            // Aggregation
            int sosCount = 0;
            java.util.Map<String, Integer> categoryCounts = new java.util.HashMap<>();
            java.util.Map<String, Integer> userCounts = new java.util.HashMap<>();
            
            for (ActivityLog log : logs) {
                if ("SOS".equals(log.type)) {
                    sosCount++;
                    if (log.category != null) {
                        categoryCounts.put(log.category, categoryCounts.getOrDefault(log.category, 0) + 1);
                    }
                }
                
                if (log.userEmail != null && !log.userEmail.isEmpty()) {
                    userCounts.put(log.userEmail, userCounts.getOrDefault(log.userEmail, 0) + 1);
                }
            }

            // Sort Users by frequency (Ranking)
            List<java.util.Map.Entry<String, Integer>> sortedUsers = new ArrayList<>(userCounts.entrySet());
            sortedUsers.sort((a, b) -> b.getValue().compareTo(a.getValue()));

            final int finalSos = sosCount;
            final List<ActivityLog> finalLogs = logs;
            final java.util.Map<String, Integer> finalCategories = categoryCounts;
            final List<java.util.Map.Entry<String, Integer>> finalUserRanking = sortedUsers;

            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                tvTotalLogs.setText(String.valueOf(finalLogs.size()));
                tvSosLogs.setText(String.valueOf(finalSos));
                
                // Update Category Rankings
                tvRankPolice.setText(String.valueOf(finalCategories.getOrDefault("Police", 0)));
                tvRankFire.setText(String.valueOf(finalCategories.getOrDefault("Fire Department", 0)));
                tvRankMedical.setText(String.valueOf(finalCategories.getOrDefault("Medical", 0)));
                tvRankDisaster.setText(String.valueOf(finalCategories.getOrDefault("Disaster Response", 0)));
                
                // Update User Rankings UI
                updateUserRankings(finalUserRanking);
                
                // Show filtered logs in audit chronology
                loadFilteredLogs();
            });
        }).start();
    }

    private void loadFilteredLogs() {
        AppDatabase db = AppDatabase.getDatabase(requireContext());
        new Thread(() -> {
            List<ActivityLog> logs;
            if ("ALL".equals(currentLogFilter)) {
                logs = db.activityLogDao().getAllLogsSync();
            } else {
                logs = db.activityLogDao().getLogsByTypeSync(currentLogFilter);
            }
            if (logs == null) logs = new ArrayList<>();

            final List<ActivityLog> finalLogs = logs;
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                tvLogCount.setText(finalLogs.size() + " entries");
                adapter.updateList(finalLogs);
                if (finalLogs.isEmpty()) {
                    rvLogs.setVisibility(View.GONE);
                    emptyState.setVisibility(View.VISIBLE);
                } else {
                    rvLogs.setVisibility(View.VISIBLE);
                    emptyState.setVisibility(View.GONE);
                }
            });
        }).start();
    }

    private void updateUserRankings(List<java.util.Map.Entry<String, Integer>> ranking) {
        userRankingContainer.removeAllViews();
        if (ranking.isEmpty()) {
            userRankingContainer.addView(tvNoUserActivity);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int maxItems = Math.min(ranking.size(), 5); // Show top 5
        
        for (int i = 0; i < maxItems; i++) {
            java.util.Map.Entry<String, Integer> entry = ranking.get(i);
            View row = inflater.inflate(android.R.layout.simple_list_item_2, userRankingContainer, false);
            TextView tvName = row.findViewById(android.R.id.text1);
            TextView tvSub = row.findViewById(android.R.id.text2);
            
            tvName.setText((i + 1) + ". " + entry.getKey());
            tvName.setTextColor(getResources().getColor(R.color.text_primary, null));
            tvSub.setText(entry.getValue() + " activities logged");
            tvSub.setTextColor(getResources().getColor(R.color.text_secondary, null));
            
            // Add a separator margin
            android.widget.LinearLayout.LayoutParams params = (android.widget.LinearLayout.LayoutParams) row.getLayoutParams();
            params.setMargins(16, 8, 16, 8);
            row.setLayoutParams(params);

            // Make clickable — show user audit trail
            final String userEmail = entry.getKey();
            row.setOnClickListener(v -> showUserAuditTrail(userEmail));
            
            userRankingContainer.addView(row);
        }
    }

    private void showUserAuditTrail(String userEmail) {
        if (userEmail == null || userEmail.isEmpty()) return;

        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
            new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme);
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_user_audit_trail, null);
        dialog.setContentView(dialogView);

        TextView tvSubtitle = dialogView.findViewById(R.id.tvAuditUserEmail);
        tvSubtitle.setText(userEmail);

        RecyclerView rvUserLogs = dialogView.findViewById(R.id.rvUserLogs);
        rvUserLogs.setLayoutManager(new LinearLayoutManager(requireContext()));

        AdminLogsAdapter userAdapter = new AdminLogsAdapter(new ArrayList<>());
        rvUserLogs.setAdapter(userAdapter);

        AppDatabase db = AppDatabase.getDatabase(requireContext());
        new Thread(() -> {
            List<ActivityLog> userLogs = db.activityLogDao().getLogsByUserSync(userEmail);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    userAdapter.updateList(userLogs);
                });
            }
        }).start();

        dialogView.findViewById(R.id.btnCloseAudit).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}
