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
            else if (checkedIds.contains(R.id.chipLogPolice)) currentLogFilter = "Police";
            else if (checkedIds.contains(R.id.chipLogFire)) currentLogFilter = "Fire Department";
            else if (checkedIds.contains(R.id.chipLogMedical)) currentLogFilter = "Medical";
            else if (checkedIds.contains(R.id.chipLogDisaster)) currentLogFilter = "Disaster Response";
            loadFilteredLogs();
        });

        // Open Drawer from internal header
        View btnMenu = view.findViewById(R.id.btnMenu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                if (getActivity() instanceof AdminDashboardActivity) {
                    ((AdminDashboardActivity) getActivity()).openDrawer();
                } else if (getActivity() instanceof BarangayDashboardActivity) {
                    ((BarangayDashboardActivity) getActivity()).openDrawer();
                }
            });
        }

        loadLogs();
    }

    private void loadLogs() {
        AppDatabase db = AppDatabase.getDatabase(requireContext());
        new Thread(() -> {
            List<ActivityLog> allLogs = db.activityLogDao().getAllLogsSync();
            List<ActivityLog> sosLogs = db.activityLogDao().getLogsByTypeSync("SOS");
            
            if (allLogs == null) allLogs = new ArrayList<>();
            if (sosLogs == null) sosLogs = new ArrayList<>();

            // Aggregation (using SOS logs for category rankings)
            int sosCount = sosLogs.size();
            java.util.Map<String, Integer> categoryCounts = new java.util.HashMap<>();
            java.util.Map<Integer, Integer> userCounts = new java.util.HashMap<>();
            
            for (ActivityLog log : sosLogs) {
                if (log.category != null) {
                    categoryCounts.put(log.category, categoryCounts.getOrDefault(log.category, 0) + 1);
                }
                
                if (log.userId > 0) {
                    userCounts.put(log.userId, userCounts.getOrDefault(log.userId, 0) + 1);
                }
            }

            // Sort Users by frequency (Ranking)
            List<java.util.Map.Entry<Integer, Integer>> sortedUsers = new ArrayList<>(userCounts.entrySet());
            sortedUsers.sort((a, b) -> b.getValue().compareTo(a.getValue()));

            final int finalTotal = allLogs.size();
            final int finalSos = sosCount;
            final List<ActivityLog> finalLogs = allLogs;
            final java.util.Map<String, Integer> finalCategories = categoryCounts;
            final List<java.util.Map.Entry<Integer, Integer>> finalUserRanking = sortedUsers;

            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                // Synchronize Total Logs with SOS Alerts for better monitoring focus
                tvTotalLogs.setText(String.valueOf(finalSos));
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
            List<ActivityLog> allSosLogs = db.activityLogDao().getLogsByTypeSync("SOS");
            if (allSosLogs == null) allSosLogs = new ArrayList<>();
            
            List<ActivityLog> logs = new ArrayList<>();
            if ("ALL".equals(currentLogFilter)) {
                logs.addAll(allSosLogs);
            } else {
                for (ActivityLog log : allSosLogs) {
                    if (currentLogFilter.equals(log.category)) {
                        logs.add(log);
                    }
                }
            }

            if (getActivity() == null) return;
            final List<ActivityLog> finalLogs = logs;
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

    private void updateUserRankings(List<java.util.Map.Entry<Integer, Integer>> ranking) {
        userRankingContainer.removeAllViews();
        if (ranking.isEmpty()) {
            userRankingContainer.addView(tvNoUserActivity);
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(requireContext());
        int maxItems = Math.min(ranking.size(), 5); // Show top 5
        
        for (int i = 0; i < maxItems; i++) {
            java.util.Map.Entry<Integer, Integer> entry = ranking.get(i);
            View row = inflater.inflate(android.R.layout.simple_list_item_2, userRankingContainer, false);
            TextView tvName = row.findViewById(android.R.id.text1);
            TextView tvSub = row.findViewById(android.R.id.text2);
            
            tvName.setText((i + 1) + ". User #" + entry.getKey());
            tvName.setTextColor(getResources().getColor(R.color.text_primary, null));
            tvSub.setText(entry.getValue() + " activities logged");
            tvSub.setTextColor(getResources().getColor(R.color.text_secondary, null));
            
            // Add a separator margin
            android.widget.LinearLayout.LayoutParams params = (android.widget.LinearLayout.LayoutParams) row.getLayoutParams();
            params.setMargins(16, 8, 16, 8);
            row.setLayoutParams(params);

            // Make clickable — show user audit trail
            final int userId = entry.getKey();
            row.setOnClickListener(v -> showUserAuditTrail(userId));
            
            userRankingContainer.addView(row);
        }
    }

    private void showUserAuditTrail(int userId) {
        if (userId <= 0) return;

        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
            new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme);
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_user_audit_trail, null);
        dialog.setContentView(dialogView);

        TextView tvSubtitle = dialogView.findViewById(R.id.tvAuditUserEmail);
        tvSubtitle.setText("User ID: " + userId);

        RecyclerView rvUserLogs = dialogView.findViewById(R.id.rvUserLogs);
        rvUserLogs.setLayoutManager(new LinearLayoutManager(requireContext()));

        AdminLogsAdapter userAdapter = new AdminLogsAdapter(new ArrayList<>());
        rvUserLogs.setAdapter(userAdapter);

        AppDatabase db = AppDatabase.getDatabase(requireContext());
        new Thread(() -> {
            List<ActivityLog> userLogs = db.activityLogDao().getLogsByUserSync(userId);
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
