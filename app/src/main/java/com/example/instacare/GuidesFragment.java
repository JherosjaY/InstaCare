package com.example.instacare;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.data.local.AppDatabase;
import com.example.instacare.data.local.Guide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;

public class GuidesFragment extends Fragment {

    private List<Guide> allGuides;

    private GuidesAdapter adapter;
    private RecyclerView guidesRecyclerView;
    private ChipGroup categoryChips;
    private TextInputEditText searchEditText;
    private boolean isShowingBookmarksOnly = false;
    private android.widget.ImageView btnViewBookmarks;
    private String currentCategory = "All";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_guides, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        guidesRecyclerView = view.findViewById(R.id.guidesRecyclerView);
        categoryChips = view.findViewById(R.id.categoryChips);
        searchEditText = view.findViewById(R.id.searchEditText);
        btnViewBookmarks = view.findViewById(R.id.btnViewBookmarks);

        // Edge-to-Edge inset handling for the top bar
        View topBar = view.findViewById(R.id.topBar);
        if (topBar != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, windowInsets) -> {
                androidx.core.graphics.Insets insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
                int padding20 = (int)(20 * getResources().getDisplayMetrics().density);
                v.setPadding(padding20, insets.top + padding20, padding20, padding20);
                return windowInsets;
            });
        }

        setupRecyclerView();
        setupChips();
        setupBookmarkToggle();
    }

    private void setupRecyclerView() {
        guidesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new GuidesAdapter(new ArrayList<>());
        guidesRecyclerView.setAdapter(adapter);

        com.example.instacare.data.local.AppDatabase.getDatabase(requireContext().getApplicationContext())
            .guideDao().getAllGuides().observe(getViewLifecycleOwner(), guides -> {
                if (guides != null) {
                    allGuides = guides;
                    applyFilters();
                }
            });
    }

    private void setupBookmarkToggle() {
        btnViewBookmarks.setOnClickListener(v -> {
            isShowingBookmarksOnly = !isShowingBookmarksOnly;
            
            // Visual feedback
            if (isShowingBookmarksOnly) {
                btnViewBookmarks.setImageResource(R.drawable.ic_bookmark_filled);
                btnViewBookmarks.setImageTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.highlight_yellow)));
            } else {
                btnViewBookmarks.setImageResource(R.drawable.ic_bookmark);
                btnViewBookmarks.setImageTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.text_primary)));
            }
            
            applyFilters();
        });
    }

    private void setupChips() {
        categoryChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            
            int checkedId = checkedIds.get(0);
            currentCategory = "All";
            
            if (checkedId == R.id.chipBleeding) currentCategory = "Bleeding";
            else if (checkedId == R.id.chipCardiac) currentCategory = "Cardiac";
            else if (checkedId == R.id.chipBurn) currentCategory = "Burn";
            else if (checkedId == R.id.chipChoking) currentCategory = "Choking";
            else if (checkedId == R.id.chipFracture) currentCategory = "Fracture";
            else if (checkedId == R.id.chipPoisoning) currentCategory = "Poisoning";
            else if (checkedId == R.id.chipWounds) currentCategory = "Wounds";
            
            applyFilters();
            
            // Unfocus search and hide keyboard
            if (searchEditText != null) {
                searchEditText.clearFocus();
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) 
                    requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
                }
            }
        });
    }

    private void applyFilters() {
        if (allGuides == null) return;
        
        List<com.example.instacare.data.local.Guide> filtered = new ArrayList<>();
        for (com.example.instacare.data.local.Guide item : allGuides) {
            boolean matchesCategory = currentCategory.equals("All") || item.category.equalsIgnoreCase(currentCategory);
            boolean matchesBookmark = !isShowingBookmarksOnly || item.isBookmarked;
            
            if (matchesCategory && matchesBookmark) {
                filtered.add(item);
            }
        }
        adapter.setGuides(filtered);
    }
}
