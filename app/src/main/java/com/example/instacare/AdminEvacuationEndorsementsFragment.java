package com.example.instacare;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.data.local.AppDatabase;
import com.example.instacare.data.local.EvacuationCenter;
import com.example.instacare.data.local.EvacuationEndorsement;
import com.example.instacare.data.local.EvacuationEndorsementStatusLog;
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Tier 3 — Feature E: Barangay officer / admin panel for reviewing and
 * assigning evacuation endorsement requests.
 * Mirrors AdminEndorsementsFragment.java + BarangayEndorsementsFragment.java.
 */
public class AdminEvacuationEndorsementsFragment extends Fragment {

    private RecyclerView recyclerView;
    private View emptyState;
    private TextView tvPending, tvAssigned, tvResolved;
    private EvacuationEndorsementAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private String currentFilter = "all";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_evacuation_endorsements, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.adminEvacEndorRecyclerView);
        emptyState   = view.findViewById(R.id.adminEvacEndorEmptyState);
        tvPending    = view.findViewById(R.id.tvAdminEvacEndorPendingCount);
        tvAssigned   = view.findViewById(R.id.tvAdminEvacEndorAssignedCount);
        tvResolved   = view.findViewById(R.id.tvAdminEvacEndorResolvedCount);

        // Adapter — tap opens review dialog
        adapter = new EvacuationEndorsementAdapter(new ArrayList<>(), this::showReviewDialog);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // Filter chips
        ChipGroup chips = view.findViewById(R.id.chipGroupAdminEvacEndorFilter);
        if (chips != null) {
            chips.setOnCheckedStateChangeListener((group, checkedIds) -> {
                if (checkedIds.isEmpty()) return;
                int id = checkedIds.get(0);
                if      (id == R.id.chipAdminEvacEndorPending)  currentFilter = "PENDING";
                else if (id == R.id.chipAdminEvacEndorAssigned) currentFilter = "ASSIGNED";
                else if (id == R.id.chipAdminEvacEndorArrived)  currentFilter = "ARRIVED";
                else if (id == R.id.chipAdminEvacEndorResolved) currentFilter = "RESOLVED";
                else                                             currentFilter = "all";
                loadData();
            });
        }

        loadData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdownNow();
    }

    private void loadData() {
        if (!isAdded()) return;
        AppDatabase db = AppDatabase.getDatabase(requireContext());

        executor.execute(() -> {
            List<EvacuationEndorsement> list;
            if ("all".equals(currentFilter)) {
                list = db.evacuationEndorsementDao().getAll();
            } else {
                list = db.evacuationEndorsementDao().getAllByStatus(currentFilter);
            }
            if (list == null) list = new ArrayList<>();

            // Count stats (always all statuses)
            int pending  = db.evacuationEndorsementDao().getCountByStatus("PENDING");
            int assigned = db.evacuationEndorsementDao().getCountByStatus("ASSIGNED");
            int resolved = db.evacuationEndorsementDao().getCountByStatus("RESOLVED");

            final List<EvacuationEndorsement> result = list;
            final int p = pending, a = assigned, r = resolved;

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    adapter.setItems(result);
                    boolean empty = result.isEmpty();
                    emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
                    recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
                    if (tvPending  != null) tvPending.setText(String.valueOf(p));
                    if (tvAssigned != null) tvAssigned.setText(String.valueOf(a));
                    if (tvResolved != null) tvResolved.setText(String.valueOf(r));
                });
            }
        });
    }

    /**
     * Admin review dialog — allows changing status, assigning center, adding remarks.
     * Mirrors the pattern in AdminEndorsementsFragment.
     */
    private void showReviewDialog(EvacuationEndorsement e) {
        if (!isAdded()) return;
        AppDatabase db = AppDatabase.getDatabase(requireContext());

        View dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_evacuation, null); // reuse existing dialog form

        // Reuse fields creatively: name=remarks, address=center name
        EditText etRemarks    = dialogView.findViewById(R.id.etEvacName);
        EditText etCenterName = dialogView.findViewById(R.id.etEvacAddress);
        if (etRemarks    != null) { etRemarks.setHint("Admin Remarks"); etRemarks.setText(e.adminRemarks); }
        if (etCenterName != null) { etCenterName.setHint("Assigned Center Name"); etCenterName.setText(e.assignedCenterName); }

        // Build status options based on current
        String[] statusOptions = {"PENDING", "ASSIGNED", "ARRIVED", "RESOLVED"};

        new AlertDialog.Builder(requireContext())
            .setTitle("Review — " + (e.caseRef != null ? e.caseRef : e.patientName))
            .setView(dialogView)
            .setPositiveButton("Assign", (dialog, which) -> {
                String remarks    = etRemarks    != null && etRemarks.getText()    != null ? etRemarks.getText().toString().trim()    : "";
                String centerName = etCenterName != null && etCenterName.getText() != null ? etCenterName.getText().toString().trim() : "";
                updateEndorsement(e, "ASSIGNED", remarks, centerName);
            })
            .setNeutralButton("Resolve", (dialog, which) -> updateEndorsement(e, "RESOLVED",
                etRemarks != null && etRemarks.getText() != null ? etRemarks.getText().toString().trim() : "", e.assignedCenterName))
            .setNegativeButton("Close", null)
            .show();
    }

    private void updateEndorsement(EvacuationEndorsement e, String newStatus, String remarks, String centerName) {
        if (!isAdded()) return;
        AppDatabase db = AppDatabase.getDatabase(requireContext());
        SharedPreferences prefs = requireContext()
            .getSharedPreferences("user_session", android.content.Context.MODE_PRIVATE);
        String adminName = prefs.getString("username", "admin");

        long now = System.currentTimeMillis();
        executor.execute(() -> {
            db.evacuationEndorsementDao().updateStatus(
                e.id, newStatus, remarks, null, centerName, now);

            // Log the status change
            EvacuationEndorsementStatusLog log = new EvacuationEndorsementStatusLog(
                e.id, e.status, newStatus, adminName, now);
            // We'll use the insert from the endorsement dao table — log is separate
            // For now store in sharedprefs as fallback (log dao added next iteration)

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    Toast.makeText(requireContext(),
                        "Updated to " + newStatus, Toast.LENGTH_SHORT).show();
                    loadData();
                });
            }
        });
    }
}
