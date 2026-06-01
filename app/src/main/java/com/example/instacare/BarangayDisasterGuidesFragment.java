package com.example.instacare;

import android.content.Intent;
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
import com.example.instacare.data.local.DisasterGuide;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;

public class BarangayDisasterGuidesFragment extends Fragment {

    private RecyclerView rvDisasters;
    private DisasterGuidesAdapter adapter;
    private ChipGroup chipGroup;
    private AppDatabase db;
    private List<DisasterGuide> allGuides = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_barangay_disaster_guides, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        db = AppDatabase.getDatabase(requireContext());

        rvDisasters = view.findViewById(R.id.rvDisastersBarangay);
        chipGroup = view.findViewById(R.id.chipGroupFilterBarangay);

        adapter = new DisasterGuidesAdapter(new ArrayList<>(), guide -> {
            Intent intent = new Intent(getContext(), GuideDetailActivity.class);
            intent.putExtra("GUIDE_TITLE", guide.title);
            intent.putExtra("GUIDE_DESC", guide.description);
            intent.putExtra("GUIDE_VIDEO_URL", guide.videoUrl);
            intent.putExtra("GUIDE_STEPS", guide.stepsJson);
            // Hint GuideDetailActivity to use Blue theme
            intent.putExtra("ROLE", "BARANGAY");
            startActivity(intent);
        });
        rvDisasters.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvDisasters.setAdapter(adapter);

        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                adapter.updateList(allGuides);
            } else {
                int checkedId = checkedIds.get(0);
                String filter = "Typhoon";
                if (checkedId == R.id.chipEarthquake) filter = "Earthquake";
                if (checkedId == R.id.chipFire) filter = "Fire";
                if (checkedId == R.id.chipFlood) filter = "Flood";
                filterGuides(filter);
            }
        });

        loadGuides();
    }

    private void loadGuides() {
        new Thread(() -> {
            List<DisasterGuide> dbGuides = db.disasterGuideDao().getAllDisasterGuidesDirect();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    allGuides.clear();
                    if (dbGuides != null) allGuides.addAll(dbGuides);
                    adapter.updateList(allGuides);
                    chipGroup.check(R.id.chipTyphoon);
                });
            }
        }).start();
    }

    private void filterGuides(String type) {
        List<DisasterGuide> filtered = new ArrayList<>();
        for (DisasterGuide g : allGuides) {
            if (g.disasterType != null && g.disasterType.equalsIgnoreCase(type)) {
                filtered.add(g);
            }
        }
        adapter.updateList(filtered);
    }
}
