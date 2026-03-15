package com.example.instacare;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.instacare.data.local.AppDatabase;
import com.example.instacare.data.local.Endorsement;
import com.example.instacare.data.local.ActivityLog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.AdminLogsAdapter;
import java.util.ArrayList;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class BarangayOverviewFragment extends Fragment {

    private TextView tvPending, tvApproved, tvDate;
    private RecyclerView rvLogs;
    private AdminLogsAdapter logsAdapter;
    private View emptyStateRecentLogs, cardRecentLogs;
    private AppDatabase db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_barangay_overview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = AppDatabase.getDatabase(requireContext());

        tvPending = view.findViewById(R.id.tvPendingCount);
        tvApproved = view.findViewById(R.id.tvApprovedCount);
        tvDate = view.findViewById(R.id.tvDate);

        String today = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(new Date());
        tvDate.setText(today);

        view.findViewById(R.id.btnMenu).setOnClickListener(v -> {
            if (getActivity() instanceof BarangayDashboardActivity) {
                ((BarangayDashboardActivity) getActivity()).openDrawer();
            }
        });

        view.findViewById(R.id.btnReviewEndorsements).setOnClickListener(v -> {
            if (getActivity() instanceof BarangayDashboardActivity) {
                ((BarangayDashboardActivity) getActivity()).navigateToSection(R.id.nav_brgy_endorsements);
            }
        });

        view.findViewById(R.id.btnViewLogs).setOnClickListener(v -> {
            if (getActivity() instanceof BarangayDashboardActivity) {
                ((BarangayDashboardActivity) getActivity()).navigateToSection(R.id.nav_brgy_logs);
            }
        });

        // Initialize Recent Activity
        rvLogs = view.findViewById(R.id.rvRecentLogs);
        emptyStateRecentLogs = view.findViewById(R.id.emptyStateRecentLogs);
        cardRecentLogs = view.findViewById(R.id.cardRecentLogs);
        
        logsAdapter = new AdminLogsAdapter(new ArrayList<>());
        rvLogs.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvLogs.setAdapter(logsAdapter);

        view.findViewById(R.id.tvSeeAllLogs).setOnClickListener(v -> {
            if (getActivity() instanceof BarangayDashboardActivity) {
                ((BarangayDashboardActivity) getActivity()).navigateToSection(R.id.nav_brgy_logs);
            }
        });

        loadData();
    }

    private void loadData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Endorsement> pending = db.endorsementDao().getAllByStatus("PENDING");
            List<Endorsement> approved = db.endorsementDao().getAllByStatus("APPROVED");

            // Load last 5 logs for 'barangay'
            List<ActivityLog> allBrgyLogs = db.activityLogDao().getLogsByUserSync("barangay");
            final List<ActivityLog> recentLogs = (allBrgyLogs != null && allBrgyLogs.size() > 5) ? 
                    allBrgyLogs.subList(0, 5) : allBrgyLogs;

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    tvPending.setText(String.valueOf(pending.size()));
                    tvApproved.setText(String.valueOf(approved.size()));
                    
                    if (recentLogs == null || recentLogs.isEmpty()) {
                        cardRecentLogs.setVisibility(View.GONE);
                        emptyStateRecentLogs.setVisibility(View.VISIBLE);
                    } else {
                        cardRecentLogs.setVisibility(View.VISIBLE);
                        emptyStateRecentLogs.setVisibility(View.GONE);
                        logsAdapter.updateList(recentLogs);
                    }
                });
            }
        });
    }
}
