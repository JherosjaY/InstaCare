package com.example.instacare;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.data.local.AppDatabase;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.util.ArrayList;
import java.util.List;

public class NotificationBottomSheetFragment extends BottomSheetDialogFragment {

    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private View emptyState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notification_bottom_sheet, container, false);

        recyclerView = view.findViewById(R.id.notificationsRecyclerView);
        emptyState = view.findViewById(R.id.emptyStateContainer);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NotificationAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);
        
        loadNotifications(view);

        // Expand logic
        view.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            com.google.android.material.bottomsheet.BottomSheetDialog dialog = (com.google.android.material.bottomsheet.BottomSheetDialog) getDialog();
            if (dialog != null) {
                View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                if (bottomSheet != null) {
                    com.google.android.material.bottomsheet.BottomSheetBehavior<View> behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet);
                    // Open fully expanded by default so empty state is visible
                    behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
                    behavior.setSkipCollapsed(true); // Don't peek, just show full
                }

                // Fix Navigation Bar Color to match Bottom Sheet Background
                if (dialog.getWindow() != null) {
                    android.view.Window window = dialog.getWindow();
                    // Use dashboard_surface color to match the sheet background
                    window.setNavigationBarColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.dashboard_surface));
                    
                    // Adjust icons (Light icons for Dark Mode, Dark icons for Light Mode)
                    boolean isDark = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;
                    androidx.core.view.WindowInsetsControllerCompat controller = androidx.core.view.WindowCompat.getInsetsController(window, window.getDecorView());
                    if (controller != null) {
                        controller.setAppearanceLightNavigationBars(!isDark);
                    }
                }
            }
        });

        return view;
    }

    private void loadNotifications(View view) {
        AppDatabase db = AppDatabase.getDatabase(requireContext());
        new Thread(() -> {
            List<com.example.instacare.data.local.Notification> notifications = db.notificationDao().getAllNotifications();
            db.notificationDao().markAllAsRead(); // Mark them as read when viewed

            requireActivity().runOnUiThread(() -> {
                adapter.updateData(notifications);
                
                if (notifications.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    emptyState.setVisibility(View.VISIBLE);
                    
                    // Animate Empty Icon (AVD)
                    android.widget.ImageView icon = view.findViewById(R.id.emptyStateIcon);
                    icon.setImageDrawable(androidx.core.content.ContextCompat.getDrawable(requireContext(), R.drawable.avd_bell_ringing));
                    
                    if (icon.getDrawable() instanceof android.graphics.drawable.Animatable) {
                        ((android.graphics.drawable.Animatable) icon.getDrawable()).start();
                    }
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyState.setVisibility(View.GONE);
                }
            });
        }).start();
    }

    @Override
    public int getTheme() {
        return R.style.BottomSheetDialogTheme;
    }
}
