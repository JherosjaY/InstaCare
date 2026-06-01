package com.example.instacare;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.data.local.AppDatabase;
import com.example.instacare.data.local.EvacuationAnnouncement;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Tier 2 — Feature C (Admin side)
 * Admin/Barangay officer can post, view, and archive evacuation announcements.
 */
public class AdminEvacuationAnnouncementsFragment extends Fragment {

    private RecyclerView recyclerView;
    private View emptyState;
    private EvacuationAnnouncementAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_evacuation_announcements, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.announcementsRecyclerView);
        emptyState   = view.findViewById(R.id.announcementEmptyState);

        adapter = new EvacuationAnnouncementAdapter(new ArrayList<>());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // Long-press to archive
        recyclerView.addOnItemTouchListener(new androidx.recyclerview.widget.RecyclerView.SimpleOnItemTouchListener() {
            // handled via adapter item click in production; simplified here
        });

        // Use existing FAB from coordinator (if present) or skip
        View fab = view.findViewWithTag("fab_post");
        if (fab != null) fab.setOnClickListener(v -> showPostDialog());

        loadAnnouncements();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdownNow();
    }

    private void loadAnnouncements() {
        if (!isAdded()) return;
        AppDatabase db = AppDatabase.getDatabase(requireContext());
        executor.execute(() -> {
            List<EvacuationAnnouncement> list = db.evacuationAnnouncementDao().getAllActive();
            if (list == null) list = new ArrayList<>();
            final List<EvacuationAnnouncement> result = list;
            if (getActivity() != null) getActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                adapter.setItems(result);
                boolean empty = result.isEmpty();
                emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
                recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
            });
        });
    }

    private void showPostDialog() {
        if (!isAdded()) return;
        View dv = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_evacuation, null);

        // Reuse fields: evacName→title, evacAddress→message
        EditText etTitle   = dv.findViewById(R.id.etEvacName);
        EditText etMessage = dv.findViewById(R.id.etEvacAddress);
        if (etTitle   != null) etTitle.setHint("Announcement title");
        if (etMessage != null) etMessage.setHint("Details / message body");

        new AlertDialog.Builder(requireContext())
            .setTitle("Post Evacuation Alert")
            .setView(dv)
            .setPositiveButton("Post", (dialog, which) -> {
                String title   = etTitle   != null ? etTitle.getText().toString().trim()   : "";
                String message = etMessage != null ? etMessage.getText().toString().trim() : "";
                if (title.isEmpty() || message.isEmpty()) {
                    Toast.makeText(requireContext(), "Fill in title and message", Toast.LENGTH_SHORT).show();
                    return;
                }
                postAnnouncement(title, message, "URGENT");
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void postAnnouncement(String title, String message, String priority) {
        AppDatabase db = AppDatabase.getDatabase(requireContext());
        executor.execute(() -> {
            EvacuationAnnouncement ann = new EvacuationAnnouncement(
                null, "All Centers", title, message, "admin",
                System.currentTimeMillis(), 1, priority);
            db.evacuationAnnouncementDao().insert(ann);
            if (getActivity() != null) getActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Announcement posted", Toast.LENGTH_SHORT).show();
                loadAnnouncements();
            });
        });
    }
}
