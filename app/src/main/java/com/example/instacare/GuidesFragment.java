package com.example.instacare;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.data.local.AppDatabase;
import com.example.instacare.data.local.DisasterGuide;
import com.example.instacare.VideoPlaybackActivity;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GuidesFragment extends Fragment {

    private List<com.example.instacare.data.local.Guide> allGuides;
    private List<com.example.instacare.data.local.DisasterGuide> allDisasterGuides;

    private GuidesAdapter adapter;
    private DisasterGuidesAdapter disasterAdapter;
    private RecyclerView guidesRecyclerView;
    private View segmentedControl, segmentedSlider;
    private TextView btnTypeFirstAid, btnTypeDisaster;
    private ChipGroup categoryChips;
    private ChipGroup disasterCategoryChips;
    private TextInputEditText searchEditText;
    private boolean isShowingBookmarksOnly = false;
    private boolean isShowingDisasterReadiness = false;
    private android.widget.ImageView btnViewBookmarks;
    private String currentCategory = "All";
    private String currentDisasterCategory = "All";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_guides, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        guidesRecyclerView = view.findViewById(R.id.guidesRecyclerView);
        segmentedControl = view.findViewById(R.id.segmentedControl);
        segmentedSlider = view.findViewById(R.id.segmentedSlider);
        btnTypeFirstAid = view.findViewById(R.id.btnTypeFirstAid);
        btnTypeDisaster = view.findViewById(R.id.btnTypeDisaster);
        categoryChips = view.findViewById(R.id.categoryChips);
        disasterCategoryChips = view.findViewById(R.id.disasterCategoryChips);
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
        setupMainToggle();
        setupChips();
        setupSearch();
        setupBookmarkToggle();

        updateHeaderTitle();

        // Handle deep-link from Home screen card
        if (getArguments() != null && getArguments().getBoolean("SELECT_DISASTER", false)) {
            switchToggle(true, false);
        }
    }

    private void updateHeaderTitle() {
        TextView tvTitle = getView().findViewById(R.id.tvTitleGuides);
        if (tvTitle != null) {
            String fullText = isShowingDisasterReadiness ? "Disaster Readiness" : "First Aid Guides";
            android.text.SpannableStringBuilder spannable = new android.text.SpannableStringBuilder(fullText);
            
            String highlight = isShowingDisasterReadiness ? "Readiness" : "Aid";
            int start = fullText.indexOf(highlight);
            if (start != -1) {
                int color = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.emergency_red);
                spannable.setSpan(
                    new android.text.style.ForegroundColorSpan(color),
                    start, 
                    start + highlight.length(), 
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
            tvTitle.setText(spannable);
        }
    }

    private void setupRecyclerView() {
        guidesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Initialize both adapters
        adapter = new GuidesAdapter(new ArrayList<>());
        disasterAdapter = new DisasterGuidesAdapter(new ArrayList<>(), new DisasterGuidesAdapter.Listener() {
            @Override
            public void onClick(DisasterGuide guide) {
                android.content.Intent intent = new android.content.Intent(getContext(), GuideDetailActivity.class);
                intent.putExtra("GUIDE_ID", guide.id);
                intent.putExtra("GUIDE_TITLE", guide.title);
                intent.putExtra("GUIDE_DESC", guide.description);
                intent.putExtra("GUIDE_VIDEO_URL", guide.videoUrl);
                intent.putExtra("GUIDE_STEPS", guide.stepsJson);
                intent.putExtra("IS_DISASTER", true);
                startActivity(intent);
            }

            @Override
            public void onBookmarkClick(DisasterGuide guide) {
                boolean newStatus = !guide.isBookmarked;
                guide.isBookmarked = newStatus;
                
                new Thread(() -> {
                    com.example.instacare.data.local.AppDatabase.getDatabase(requireContext().getApplicationContext())
                        .disasterGuideDao().updateBookmarkStatus(guide.id, newStatus);
                }).start();

                android.widget.Toast.makeText(getContext(), 
                    newStatus ? "Saved to readiness list!" : "Removed from saves.", 
                    android.widget.Toast.LENGTH_SHORT).show();
                
                // Refresh list visually immediately if needed
                disasterAdapter.notifyDataSetChanged();
            }
        });

        // Set initial adapter
        guidesRecyclerView.setAdapter(adapter);

        AppDatabase db = AppDatabase.getDatabase(requireContext().getApplicationContext());
        
        // Observe First Aid Guides
        db.guideDao().getAllGuides().observe(getViewLifecycleOwner(), guides -> {
            allGuides = guides;
            if (!isShowingDisasterReadiness) applyFilters();
        });

        // Observe Disaster Readiness Guides
        db.disasterGuideDao().getAllDisasterGuides().observe(getViewLifecycleOwner(), guides -> {
            allDisasterGuides = guides;
            if (isShowingDisasterReadiness) applyFilters();
        });
    }

    private void setupMainToggle() {
        btnTypeFirstAid.setOnClickListener(v -> switchToggle(false, true));
        btnTypeDisaster.setOnClickListener(v -> switchToggle(true, true));
    }

    private void switchToggle(boolean isDisaster, boolean animate) {
        if (isShowingDisasterReadiness == isDisaster && animate) return;
        
        isShowingDisasterReadiness = isDisaster;
        
        // --- Animate Slider ---
        // btnTypeFirstAid.getWidth() might be 0 if called before layout, so we use a fallback if needed
        float width = btnTypeFirstAid.getWidth();
        if (width == 0) width = (int)(140 * getResources().getDisplayMetrics().density); // roughly half of 280dp

        float translationX = isDisaster ? width : 0f;
        if (animate) {
            segmentedSlider.animate()
                .translationX(translationX)
                .setDuration(300)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
        } else {
            segmentedSlider.setTranslationX(translationX);
        }

        // --- Update Text Colors ---
        int activeColor = ContextCompat.getColor(requireContext(), R.color.white);
        int inactiveColor = ContextCompat.getColor(requireContext(), R.color.text_secondary);
        
        btnTypeFirstAid.setTextColor(isDisaster ? inactiveColor : activeColor);
        btnTypeDisaster.setTextColor(isDisaster ? activeColor : inactiveColor);

        // --- Sync Content ---
        categoryChips.setVisibility(isDisaster ? View.GONE : View.VISIBLE);
        disasterCategoryChips.setVisibility(isDisaster ? View.VISIBLE : View.GONE);
        
        guidesRecyclerView.setAdapter(isDisaster ? disasterAdapter : adapter);
        
        updateHeaderTitle();
        applyFilters();
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
                    ContextCompat.getColor(requireContext(), R.color.emergency_red)));
            }
            
            applyFilters();
        });
    }

    private void setupChips() {
        // First Aid sub-categories
        categoryChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            currentCategory = "All";
            
            if (checkedId == R.id.chipBleeding) currentCategory = "Bleeding";
            else if (checkedId == R.id.chipCardiac) currentCategory = "Cardiac";
            else if (checkedId == R.id.chipBurn) currentCategory = "Burn";
            else if (checkedId == R.id.chipChoking) currentCategory = "Choking";
            
            applyFilters();
        });

        // Disaster sub-categories
        disasterCategoryChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            currentDisasterCategory = "All";
            
            if (checkedId == R.id.chipTyphoon) currentDisasterCategory = "Typhoon";
            else if (checkedId == R.id.chipEarthquake) currentDisasterCategory = "Earthquake";
            else if (checkedId == R.id.chipFire) currentDisasterCategory = "Fire";
            
            applyFilters();
        });
    }

    private void setupSearch() {
        if (searchEditText != null) {
            searchEditText.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilters(); }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });
        }
    }

    private void applyFilters() {
        String query = searchEditText != null && searchEditText.getText() != null ? 
                        searchEditText.getText().toString().toLowerCase().trim() : "";

        if (isShowingDisasterReadiness) {
            if (allDisasterGuides == null) return;
            List<com.example.instacare.data.local.DisasterGuide> filtered = new ArrayList<>();
            for (com.example.instacare.data.local.DisasterGuide item : allDisasterGuides) {
                boolean matchesCategory = currentDisasterCategory.equals("All") || item.disasterType.equalsIgnoreCase(currentDisasterCategory);
                boolean matchesBookmark = !isShowingBookmarksOnly || item.isBookmarked;
                boolean matchesSearch = query.isEmpty() || item.title.toLowerCase().contains(query) || item.description.toLowerCase().contains(query);
                
                if (matchesCategory && matchesBookmark && matchesSearch) filtered.add(item);
            }
            disasterAdapter.updateList(filtered);
            updateEmptyStates(filtered.isEmpty());
        } else {
            if (allGuides == null) return;
            List<com.example.instacare.data.local.Guide> filtered = new ArrayList<>();
            for (com.example.instacare.data.local.Guide item : allGuides) {
                boolean matchesCategory = currentCategory.equals("All") || item.category.equalsIgnoreCase(currentCategory);
                boolean matchesBookmark = !isShowingBookmarksOnly || item.isBookmarked;
                boolean matchesSearch = query.isEmpty() || item.title.toLowerCase().contains(query) || item.description.toLowerCase().contains(query);
                
                if (matchesCategory && matchesBookmark && matchesSearch) filtered.add(item);
            }
            adapter.setGuides(filtered);
            updateEmptyStates(filtered.isEmpty());
        }
    }

    private void updateEmptyStates(boolean isEmpty) {
        View emptyGuides = getView() != null ? getView().findViewById(R.id.emptyStateGuides) : null;
        View emptyBookmarks = getView() != null ? getView().findViewById(R.id.emptyStateBookmarks) : null;
        
        if (isEmpty) {
            guidesRecyclerView.setVisibility(View.GONE);
            if (isShowingBookmarksOnly) {
                if (emptyBookmarks != null) emptyBookmarks.setVisibility(View.VISIBLE);
                if (emptyGuides != null) emptyGuides.setVisibility(View.GONE);
            } else {
                if (emptyGuides != null) {
                    emptyGuides.setVisibility(View.VISIBLE);
                    TextView tvMsg = emptyGuides.findViewById(R.id.tvEmptyGuidesMsg);
                    if (tvMsg != null) {
                        tvMsg.setText("No guides found for this selection.");
                    }
                }
                if (emptyBookmarks != null) emptyBookmarks.setVisibility(View.GONE);
            }
        } else {
            guidesRecyclerView.setVisibility(View.VISIBLE);
            if (emptyGuides != null) emptyGuides.setVisibility(View.GONE);
            if (emptyBookmarks != null) emptyBookmarks.setVisibility(View.GONE);
        }
    }
}
