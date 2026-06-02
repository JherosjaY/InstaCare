package com.example.instacare;

import android.content.Context;
import android.content.SharedPreferences;
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
import com.example.instacare.data.local.Endorsement;
import com.example.instacare.data.local.EvacuationCenter;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class BarangayMonitoringFragment extends Fragment {

    private RecyclerView rvLogs;
    private AdminLogsAdapter adapter;
    private TextView tvEvacuationCount, tvEndorsementCount, tvSosCount, tvLogCount;
    private View emptyState;
    private String currentLogFilter = "ALL";
    private String selectedCommunity;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_barangay_monitoring, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        tvEvacuationCount = view.findViewById(R.id.tvEvacuationCount);
        tvEndorsementCount = view.findViewById(R.id.tvEndorsementCount);
        tvSosCount = view.findViewById(R.id.tvSosCount);
        tvLogCount = view.findViewById(R.id.tvLogCount);
        rvLogs = view.findViewById(R.id.rvLogs);
        emptyState = view.findViewById(R.id.emptyStateLogs);

        adapter = new AdminLogsAdapter(new ArrayList<>());
        rvLogs.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvLogs.setAdapter(adapter);

        SessionManager sessionManager = SessionManager.getInstance(requireContext());
        selectedCommunity = sessionManager.getString("SELECTED_COMMUNITY", "");

        ChipGroup chipGroup = view.findViewById(R.id.chipGroupLogs);
        
        // Consistent active color for Barangay (Blue)
        int activeColor = android.graphics.Color.parseColor("#2563EB");
        int rippleColor = android.graphics.Color.parseColor("#302563EB");
        
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            com.google.android.material.chip.Chip chip = (com.google.android.material.chip.Chip) chipGroup.getChildAt(i);
            int[][] chipStates = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{}
            };
            int[] chipColors = new int[]{activeColor, android.graphics.Color.parseColor("#F1F5F9")};
            chip.setChipBackgroundColor(new android.content.res.ColorStateList(chipStates, chipColors));
            chip.setRippleColor(android.content.res.ColorStateList.valueOf(rippleColor));
            
            int[] chipTextColors = new int[]{android.graphics.Color.WHITE, android.graphics.Color.parseColor("#64748B")};
            chip.setTextColor(new android.content.res.ColorStateList(chipStates, chipTextColors));
        }

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.contains(R.id.chipLogAll)) currentLogFilter = "ALL";
            else if (checkedIds.contains(R.id.chipLogEvacuation)) currentLogFilter = "EVACUATION";
            else if (checkedIds.contains(R.id.chipLogEndorsement)) currentLogFilter = "ENDORSEMENT";
            else if (checkedIds.contains(R.id.chipLogSos)) currentLogFilter = "SOS";
            loadLogs();
        });

        loadLogs();
    }

    private void loadLogs() {
        AppDatabase db = AppDatabase.getDatabase(requireContext());
        new Thread(() -> {
            List<EvacuationCenter> evacs = db.evacuationCenterDao().getCentersByBarangaySync(selectedCommunity);
            if (evacs == null) evacs = new ArrayList<>();
            
            List<Endorsement> endorsements = db.endorsementDao().getAllManualByBarangay(selectedCommunity);
            if (endorsements == null) endorsements = new ArrayList<>();

            List<ActivityLog> mappedLogs = new ArrayList<>();

            // Map Evacuations into Logs (simulate timestamp to place them at bottom mostly, or top, just map them)
            long simulatedTime = System.currentTimeMillis();
            if ("ALL".equals(currentLogFilter) || "EVACUATION".equals(currentLogFilter)) {
                for (EvacuationCenter ec : evacs) {
                    mappedLogs.add(new ActivityLog("EVACUATION", 
                        "Assigned Evacuation Center: " + ec.name, 
                        0, "Evacuation", selectedCommunity, simulatedTime--));
                }
            }

            if ("ALL".equals(currentLogFilter) || "ENDORSEMENT".equals(currentLogFilter)) {
                for (Endorsement e : endorsements) {
                    String desc = "Reviewed Endorsement from User #" + e.userId;
                    if ("PENDING".equals(e.status)) {
                        desc = "Pending Endorsement from User #" + e.userId;
                    }
                    mappedLogs.add(new ActivityLog("ENDORSE", 
                        desc, e.userId, e.status, selectedCommunity, e.createdAt));
                }
            }

            // Real SOS logs from DB (Zone Isolated)
            List<ActivityLog> dbSosLogs = db.activityLogDao().getLogsByZoneAndTypeSync(selectedCommunity, "SOS");
            if (dbSosLogs == null) dbSosLogs = new ArrayList<>();
            
            if ("ALL".equals(currentLogFilter) || "SOS".equals(currentLogFilter)) {
                mappedLogs.addAll(dbSosLogs);
            }

            // Sort purely by timestamp DESC
            mappedLogs.sort((a, b) -> Long.compare(b.timestamp, a.timestamp));

            final int evacCount = evacs.size();
            final int endCount = endorsements.size();
            final int sosCount = dbSosLogs.size();
            final List<ActivityLog> finalLogs = mappedLogs;

            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                tvEvacuationCount.setText(String.valueOf(evacCount));
                tvEndorsementCount.setText(String.valueOf(endCount));
                tvSosCount.setText(String.valueOf(sosCount));
                tvLogCount.setText(finalLogs.size() + " entries");
                
                if (finalLogs.isEmpty()) {
                    rvLogs.setVisibility(View.GONE);
                    emptyState.setVisibility(View.VISIBLE);
                } else {
                    rvLogs.setVisibility(View.VISIBLE);
                    emptyState.setVisibility(View.GONE);
                    adapter.updateList(finalLogs);
                }
            });
        }).start();
    }
}
