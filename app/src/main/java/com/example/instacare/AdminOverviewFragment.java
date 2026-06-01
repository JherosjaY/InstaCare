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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminOverviewFragment extends Fragment {

    private TextView tvUsers, tvGuides, tvHospitals, tvLogs;
    private RecyclerView rvLogs;
    private AdminLogsAdapter logsAdapter;
    private View emptyStateRecentLogs, cardRecentLogs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_overview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Stat count views
        tvUsers = view.findViewById(R.id.tvStatUsers);
        tvGuides = view.findViewById(R.id.tvStatGuides);
        tvHospitals = view.findViewById(R.id.tvStatHospitals);
        tvLogs = view.findViewById(R.id.tvStatLogs);

        // Live date in header
        TextView tvDate = view.findViewById(R.id.tvHeaderDate);
        String today = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(new Date());
        tvDate.setText(today);

        // Recent logs list
        rvLogs = view.findViewById(R.id.rvRecentLogs);
        emptyStateRecentLogs = view.findViewById(R.id.emptyStateRecentLogs);
        cardRecentLogs = view.findViewById(R.id.cardRecentLogs);
        logsAdapter = new AdminLogsAdapter(new ArrayList<>(), this::showUserAuditTrail);
        rvLogs.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvLogs.setAdapter(logsAdapter);

        // Quick action buttons — navigate via parent AdminDashboardActivity
        view.findViewById(R.id.quickAddGuide).setOnClickListener(v -> navigateTo(R.id.nav_content));
        view.findViewById(R.id.quickAddHospital).setOnClickListener(v -> navigateTo(R.id.nav_hospitals));
        view.findViewById(R.id.quickViewLogs).setOnClickListener(v -> navigateTo(R.id.nav_monitoring));
        view.findViewById(R.id.quickBroadcast).setOnClickListener(v -> showBroadcastDialog());

        // Open Drawer from inline menu
        View btnMenu = view.findViewById(R.id.btnAdminMenu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                if (getActivity() instanceof AdminDashboardActivity) {
                    ((AdminDashboardActivity) getActivity()).openDrawer();
                }
            });
        }

        // See All → Monitoring
        view.findViewById(R.id.tvSeeAllLogs).setOnClickListener(v -> navigateTo(R.id.nav_monitoring));

        loadStats();
    }

    private void navigateTo(int navItemId) {
        if (getActivity() instanceof AdminDashboardActivity) {
            ((AdminDashboardActivity) getActivity()).navigateToSection(navItemId);
        }
    }

    private void loadStats() {
        AppDatabase db = AppDatabase.getDatabase(requireContext());
        new Thread(() -> {
            int userCount = db.userDao().getUserCount();
            List<com.example.instacare.data.local.Guide> guides = db.guideDao().getAllGuidesDirect();
            int guideCount = guides != null ? guides.size() : 0;
            int hospitalCount = db.hospitalDao().getCount();
            
            // Total SOS count for status card
            List<ActivityLog> sosLogs = db.activityLogDao().getLogsByTypeSync("SOS");
            int logCount = sosLogs != null ? sosLogs.size() : 0;
            
            // Dashboard activity: Only SOS alerts (matches new Monitoring logic)
            List<ActivityLog> recentSos = (sosLogs != null && sosLogs.size() > 5) ? 
                    sosLogs.subList(0, 5) : sosLogs;
            
            final List<ActivityLog> finalLogs = recentSos != null ? recentSos : new ArrayList<>();

            requireActivity().runOnUiThread(() -> {
                tvUsers.setText(String.valueOf(userCount));
                tvGuides.setText(String.valueOf(guideCount));
                tvHospitals.setText(String.valueOf(hospitalCount));
                tvLogs.setText(String.valueOf(logCount));
                logsAdapter.updateList(finalLogs);
                if (finalLogs.isEmpty()) {
                    cardRecentLogs.setVisibility(View.GONE);
                    emptyStateRecentLogs.setVisibility(View.VISIBLE);
                } else {
                    cardRecentLogs.setVisibility(View.VISIBLE);
                    emptyStateRecentLogs.setVisibility(View.GONE);
                }
            });
        }).start();
    }

    private void showBroadcastDialog() {
        android.app.Dialog dialog = new android.app.Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_broadcast);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        com.google.android.material.textfield.TextInputEditText etTitle = dialog.findViewById(R.id.etBroadcastTitle);
        com.google.android.material.textfield.TextInputEditText etMessage = dialog.findViewById(R.id.etBroadcastMessage);
        com.google.android.material.button.MaterialButton btnSend = dialog.findViewById(R.id.btnSendBroadcast);

        btnSend.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String message = etMessage.getText().toString().trim();

            if (title.isEmpty() || message.isEmpty()) {
                android.widget.Toast.makeText(requireContext(), "Please fill in all fields", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }

            sendBroadcastNotification(title, message);
            dialog.dismiss();
            android.widget.Toast.makeText(requireContext(), "Broadcast Sent!", android.widget.Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void sendBroadcastNotification(String title, String message) {
        // 1. Save to Room Database
        AppDatabase db = AppDatabase.getDatabase(requireContext());
        new Thread(() -> {
            com.example.instacare.data.local.Notification notification = new com.example.instacare.data.local.Notification(
                    title, message, System.currentTimeMillis(), false, "BROADCAST", 0, "SYSTEM", 0
            );
            db.notificationDao().insert(notification);
            
            // Log this action for Admin Audit
            db.activityLogDao().insert(new ActivityLog(
                "BROADCAST", 
                "Admin sent a push notification: " + title, 
                0, 
                "System", 
                System.currentTimeMillis()
            ));

            requireActivity().runOnUiThread(() -> {
                // 2. Trigger System Notification
                com.example.instacare.NotificationHelper.sendLocalNotification(requireContext(), title, message);
                loadStats(); // Refresh logs count on dashboard
            });
        }).start();
    }

    private void showUserAuditTrail(ActivityLog registrationLog) {
        if (registrationLog.userId <= 0) return;

        com.google.android.material.bottomsheet.BottomSheetDialog dialog = 
            new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme);
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_user_audit_trail, null);
        dialog.setContentView(dialogView);

        TextView tvSubtitle = dialogView.findViewById(R.id.tvAuditUserEmail);
        tvSubtitle.setText("User ID: " + registrationLog.userId);

        RecyclerView rvUserLogs = dialogView.findViewById(R.id.rvUserLogs);
        rvUserLogs.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        // Use the same adapter but without click listener for nested view
        AdminLogsAdapter userAdapter = new AdminLogsAdapter(new ArrayList<>());
        rvUserLogs.setAdapter(userAdapter);

        AppDatabase db = AppDatabase.getDatabase(requireContext());
        new Thread(() -> {
            List<ActivityLog> userLogs = db.activityLogDao().getLogsByUserSync(registrationLog.userId);
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
