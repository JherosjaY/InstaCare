package com.example.instacare;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.data.local.AppDatabase;
import com.example.instacare.data.local.EvacuationCenter;
import com.example.instacare.data.local.EvacueeCheckIn;
import com.example.instacare.data.local.EvacuationResource;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Features A + B combined:
 *   A — Evacuation Supply/Resource Tracker (resources list)
 *   B — Evacuee Check-In System (I'm Here / Check Out)
 *
 * Shown as a bottom sheet when user taps an evacuation center card.
 */
public class CenterDetailBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_CENTER_ID   = "center_id";
    private static final String ARG_CENTER_NAME = "center_name";
    private static final String ARG_CAPACITY    = "capacity";
    private static final String ARG_STATUS      = "status";
    private static final String PREFS_NAME      = "evac_checkin_prefs";
    private static final String KEY_CHECKIN_ID  = "active_checkin_id";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    private EvacuationResourceAdapter resourceAdapter;
    private TextView tvLiveCount, tvCapLabel, tvCenterStatus;
    private ProgressBar progressCapacity;
    private MaterialButton btnCheckIn, btnCheckOut;

    private String centerId, centerName, status;
    private int capacity;

    public static CenterDetailBottomSheet newInstance(EvacuationCenter center) {
        CenterDetailBottomSheet f = new CenterDetailBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_CENTER_ID,   center.id);
        args.putString(ARG_CENTER_NAME, center.name);
        args.putString(ARG_STATUS,      center.status != null ? center.status : "Open");
        args.putInt(ARG_CAPACITY,       center.capacity);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottomsheet_center_detail, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            centerId   = getArguments().getString(ARG_CENTER_ID);
            centerName = getArguments().getString(ARG_CENTER_NAME);
            status     = getArguments().getString(ARG_STATUS, "Open");
            capacity   = getArguments().getInt(ARG_CAPACITY, 0);
        }

        // Bind header
        TextView tvName = view.findViewById(R.id.tvCheckInCenterName);
        tvCenterStatus  = view.findViewById(R.id.tvCheckInCenterStatus);
        tvLiveCount     = view.findViewById(R.id.tvLiveCount);
        tvCapLabel      = view.findViewById(R.id.tvCapacityLabel);
        progressCapacity= view.findViewById(R.id.progressCapacity);
        btnCheckIn      = view.findViewById(R.id.btnCheckIn);
        btnCheckOut     = view.findViewById(R.id.btnCheckOut);

        if (tvName != null)   tvName.setText(centerName);
        bindStatusLabel();

        // Resources RecyclerView
        RecyclerView rvResources = view.findViewById(R.id.rvCenterResources);
        resourceAdapter = new EvacuationResourceAdapter(new ArrayList<>());
        rvResources.setLayoutManager(new LinearLayoutManager(getContext()));
        rvResources.setAdapter(resourceAdapter);
        rvResources.setNestedScrollingEnabled(false);

        // Check-In / Check-Out buttons
        btnCheckIn.setOnClickListener(v -> doCheckIn());
        btnCheckOut.setOnClickListener(v -> doCheckOut());

        loadData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdownNow();
    }

    // ── Data ─────────────────────────────────────────────────────────────────

    private void loadData() {
        if (!isAdded()) return;
        AppDatabase db = AppDatabase.getDatabase(requireContext());
        int userId = getCurrentUserId();

        // Auto-expire old check-ins (> 24h)
        long now = System.currentTimeMillis();
        long cutoff = now - (24 * 60 * 60 * 1000L);

        executor.execute(() -> {
            db.evacueeCheckInDao().expireOldCheckIns(now, cutoff);

            // Feature A — resources
            List<EvacuationResource> resources =
                db.evacuationResourceDao().getResourcesForCenter(centerId);

            // Feature B — live count + user check-in state
            int liveCount = db.evacueeCheckInDao().getActiveCountForCenter(centerId);
            EvacueeCheckIn myActive = db.evacueeCheckInDao().getActiveCheckInForUser(userId);
            boolean iAmCheckedIn = myActive != null && centerId.equals(myActive.centerId);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;

                    // Feature A — show resources
                    if (resources == null || resources.isEmpty()) {
                        seedDefaultResources(db);
                    } else {
                        resourceAdapter.setItems(resources);
                    }

                    // Feature B — live count + progress
                    tvLiveCount.setText(liveCount + " evacuee" + (liveCount != 1 ? "s" : ""));
                    updateCapacityBar(liveCount);

                    // Feature B — toggle button state
                    if (iAmCheckedIn) {
                        btnCheckIn.setVisibility(View.GONE);
                        btnCheckOut.setVisibility(View.VISIBLE);
                    } else {
                        btnCheckIn.setVisibility(View.VISIBLE);
                        btnCheckOut.setVisibility(View.GONE);
                    }
                });
            }
        });
    }

    // ── Feature B: Check-In ───────────────────────────────────────────────────

    private void doCheckIn() {
        if (!isAdded()) return;
        AppDatabase db = AppDatabase.getDatabase(requireContext());
        int userId = getCurrentUserId();

        executor.execute(() -> {
            // Check out from any previous center first
            db.evacueeCheckInDao().checkOut(userId, System.currentTimeMillis());

            EvacueeCheckIn checkIn = new EvacueeCheckIn(
                userId, centerId, centerName,
                System.currentTimeMillis(), 1, "general");
            db.evacueeCheckInDao().insert(checkIn);

            int liveCount = db.evacueeCheckInDao().getActiveCountForCenter(centerId);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                        "✅ Checked in at " + centerName, Toast.LENGTH_SHORT).show();
                    tvLiveCount.setText(liveCount + " evacuee" + (liveCount != 1 ? "s" : ""));
                    updateCapacityBar(liveCount);
                    btnCheckIn.setVisibility(View.GONE);
                    btnCheckOut.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    private void doCheckOut() {
        if (!isAdded()) return;
        AppDatabase db = AppDatabase.getDatabase(requireContext());
        int userId = getCurrentUserId();

        executor.execute(() -> {
            db.evacueeCheckInDao().checkOut(userId, System.currentTimeMillis());
            int liveCount = db.evacueeCheckInDao().getActiveCountForCenter(centerId);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                        "Checked out from " + centerName, Toast.LENGTH_SHORT).show();
                    tvLiveCount.setText(liveCount + " evacuee" + (liveCount != 1 ? "s" : ""));
                    updateCapacityBar(liveCount);
                    btnCheckIn.setVisibility(View.VISIBLE);
                    btnCheckOut.setVisibility(View.GONE);
                });
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void updateCapacityBar(int liveCount) {
        if (capacity > 0) {
            int pct = Math.min((liveCount * 100) / capacity, 100);
            progressCapacity.setProgress(pct);
            tvCapLabel.setText("Capacity: " + liveCount + " / " + capacity + "  (" + pct + "%)");
            int color;
            if (pct >= 90)      color = Color.parseColor("#EF4444"); // red
            else if (pct >= 70) color = Color.parseColor("#F97316"); // orange
            else                color = Color.parseColor("#10B981"); // green
            progressCapacity.setProgressTintList(
                android.content.res.ColorStateList.valueOf(color));
        } else {
            tvCapLabel.setText("Capacity: " + liveCount + " / —");
            progressCapacity.setProgress(0);
        }
    }

    private void bindStatusLabel() {
        if (tvCenterStatus == null) return;
        int color;
        switch (status.toLowerCase()) {
            case "full":   color = Color.parseColor("#EF4444"); break;
            case "closed": color = Color.parseColor("#6B7280"); break;
            default:       color = Color.parseColor("#10B981"); break;
        }
        tvCenterStatus.setText("● " + status);
        tvCenterStatus.setTextColor(color);
    }

    /** Feature A: seed default resources so the list is never empty on first open. */
    private void seedDefaultResources(AppDatabase db) {
        if (!isAdded()) return;
        long now = System.currentTimeMillis();
        List<EvacuationResource> defaults = new ArrayList<>();
        defaults.add(new EvacuationResource(centerId, "Food Packs",   0, "pcs",    now, 0));
        defaults.add(new EvacuationResource(centerId, "Drinking Water",0, "liters", now, 0));
        defaults.add(new EvacuationResource(centerId, "Blankets",      0, "pcs",    now, 0));
        defaults.add(new EvacuationResource(centerId, "Medicine Kit",  0, "sets",   now, 0));
        defaults.add(new EvacuationResource(centerId, "Sleeping Area", 0, "beds",   now, 0));
        db.evacuationResourceDao().insertAll(defaults);
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (isAdded()) resourceAdapter.setItems(defaults);
            });
        }
    }

    private int getCurrentUserId() {
        if (!isAdded()) return -1;
        SharedPreferences prefs = requireContext()
            .getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE);
        return prefs.getInt("userId", -1);
    }
}
