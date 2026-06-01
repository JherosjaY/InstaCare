package com.example.instacare;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.data.local.AppDatabase;
import com.example.instacare.data.local.EvacuationAnnouncement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Tier 2 — Feature C: Evacuation Announcement Board
 * Shows all active announcements from barangay officers.
 * Accessible from the dashboard "Evacuation Alerts" card.
 */
public class EvacuationAnnouncementsFragment extends Fragment {

    private RecyclerView recyclerView;
    private View emptyState;
    private EvacuationAnnouncementAdapter adapter;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /** Optional: pass a centerId to filter announcements for one center */
    private String filterCenterId = null;

    public static EvacuationAnnouncementsFragment newInstance(@Nullable String centerId) {
        EvacuationAnnouncementsFragment f = new EvacuationAnnouncementsFragment();
        if (centerId != null) {
            Bundle args = new Bundle();
            args.putString("center_id", centerId);
            f.setArguments(args);
        }
        return f;
    }

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

        if (getArguments() != null) {
            filterCenterId = getArguments().getString("center_id", null);
        }

        recyclerView = view.findViewById(R.id.announcementsRecyclerView);
        emptyState   = view.findViewById(R.id.announcementEmptyState);

        adapter = new EvacuationAnnouncementAdapter(new ArrayList<>());
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

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
            List<EvacuationAnnouncement> list;
            if (filterCenterId != null) {
                list = db.evacuationAnnouncementDao().getForCenter(filterCenterId);
            } else {
                list = db.evacuationAnnouncementDao().getAllActive();
            }
            final List<EvacuationAnnouncement> result =
                list != null ? list : new ArrayList<>();

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    adapter.setItems(result);
                    boolean empty = result.isEmpty();
                    emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
                    recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
                });
            }
        });
    }
}
