package com.example.instacare;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.data.local.AppDatabase;
import com.example.instacare.data.local.DisasterGuide;
import com.example.instacare.data.local.DisasterGuideDao;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;

public class DisasterGuidesActivity extends AppCompatActivity {

    private RecyclerView rvDisasters;
    private DisasterGuidesAdapter adapter;
    private List<DisasterGuide> allGuides = new ArrayList<>();
    private ChipGroup chipGroup;
    private TextInputEditText searchEditText;
    private LinearLayout emptyState;
    private String currentCategory = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_disaster_guides);

        // Removed redundant back button listener as it causes NPE after layout change
        rvDisasters = findViewById(R.id.rvDisasters);
        chipGroup = findViewById(R.id.chipGroupFilter);
        searchEditText = findViewById(R.id.searchEditTextDisaster);
        emptyState = findViewById(R.id.emptyStateDisaster);

        adapter = new DisasterGuidesAdapter(new ArrayList<>(), new DisasterGuidesAdapter.Listener() {
            @Override
            public void onClick(DisasterGuide guide) {
                Intent intent = new Intent(DisasterGuidesActivity.this, GuideDetailActivity.class);
                intent.putExtra("GUIDE_TITLE", guide.title);
                intent.putExtra("GUIDE_DESC", guide.description);
                intent.putExtra("GUIDE_VIDEO_URL", guide.videoUrl);
                intent.putExtra("ROLE", "USER");
                startActivity(intent);
            }

            @Override
            public void onBookmarkClick(DisasterGuide guide) {
                toggleBookmark(guide);
            }
        });

        rvDisasters.setLayoutManager(new LinearLayoutManager(this));
        rvDisasters.setAdapter(adapter);

        setupFilters();
        setupSearch();
        loadData();
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterGuides(); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void setupFilters() {
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            Chip chip = findViewById(checkedIds.get(0));
            if (chip != null) {
                currentCategory = chip.getText().toString();
                filterGuides();
            }
        });
    }

    private void loadData() {
        new Thread(() -> {
            allGuides = AppDatabase.getDatabase(this).disasterGuideDao().getAllDisasterGuidesDirect();
            runOnUiThread(this::filterGuides);
        }).start();
    }

    private void filterGuides() {
        String query = searchEditText.getText().toString().toLowerCase().trim();
        List<DisasterGuide> filtered = new ArrayList<>();
        
        for (DisasterGuide g : allGuides) {
            boolean matchesCategory = currentCategory.equals("All") || g.disasterType.equalsIgnoreCase(currentCategory);
            boolean matchesQuery = g.title.toLowerCase().contains(query) || g.description.toLowerCase().contains(query);
            
            if (matchesCategory && matchesQuery) {
                filtered.add(g);
            }
        }
        
        adapter.updateList(filtered);
        emptyState.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void toggleBookmark(DisasterGuide guide) {
        guide.isBookmarked = !guide.isBookmarked;
        new Thread(() -> {
            AppDatabase.getDatabase(this).disasterGuideDao().updateBookmarkStatus(guide.id, guide.isBookmarked);
            runOnUiThread(() -> {
                adapter.notifyDataSetChanged();
                Toast.makeText(this, guide.isBookmarked ? "Guide Bookmarked" : "Removed from Bookmarks", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }
}
