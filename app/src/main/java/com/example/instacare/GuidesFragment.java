package com.example.instacare;

import android.content.Context;
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
    private GenericGuidesAdapter consolidatedAdapter;
    private RecyclerView guidesRecyclerView;
    private View segmentedControl, segmentedSlider;
    private TextView btnTypeFirstAid, btnTypeDisaster;
    private ChipGroup categoryChips;
    private ChipGroup disasterCategoryChips;
    private com.google.android.material.textfield.TextInputEditText searchEditText;
    private boolean isShowingBookmarksOnly = false;
    private boolean isShowingDisasterReadiness = false;
    private android.widget.ImageView btnViewBookmarks;
    private String currentCategory = "All";
    private String currentDisasterCategory = "All";
    private float cornerRadiusPx;
    private View loadingState;
    private boolean isInitialLoadDelayed = false;
    private android.os.Handler searchHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable searchRunnable;

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
        loadingState = view.findViewById(R.id.loadingStateGuides);

        // Edge-to-Edge inset handling for the AppBarLayout
        com.google.android.material.appbar.AppBarLayout appBar = view.findViewById(R.id.appBarLayout);
        if (appBar != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(appBar, (v, windowInsets) -> {
                androidx.core.graphics.Insets insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
                v.setPadding(v.getPaddingLeft(), insets.top, v.getPaddingRight(), v.getPaddingBottom());
                // --- PREMIUM UX: Dynamic Status Bar Icon Tinting ---
                if (getActivity() != null && getActivity().getWindow() != null) {
                    androidx.core.view.WindowInsetsControllerCompat controller = 
                        androidx.core.view.WindowCompat.getInsetsController(getActivity().getWindow(), getActivity().getWindow().getDecorView());
                    if (controller != null) {
                        boolean isDarkMode = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) 
                                           == android.content.res.Configuration.UI_MODE_NIGHT_YES;
                        // Light background (not dark mode) = Dark icons (LightStatusBar = true)
                        // Dark background (dark mode) = White icons (LightStatusBar = false)
                        controller.setAppearanceLightStatusBars(!isDarkMode);
                    }
                }
                return windowInsets;
            });

            // Dynamic corners based on scroll
            cornerRadiusPx = getResources().getDisplayMetrics().density * 24; // 24dp
            setupDynamicCorners(appBar);
        }

        setupRecyclerView();
        setupMainToggle();
        setupChips();
        setupSearch();
        setupBookmarkToggle();

        updateHeaderTitle();
        showLoading(true);

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

    private void updateStickyHeader(boolean isBookmarked) {
        TextView tvTitle = getView().findViewById(R.id.tvTitleGuides);

        if (isBookmarked) {
            // Keep segmentedControl VISIBLE but update the title
            if (tvTitle != null) {
                String text = "My Saved Guides";
                android.text.SpannableStringBuilder spannable = new android.text.SpannableStringBuilder(text);
                int start = text.indexOf("Saved");
                if (start != -1) {
                    int color = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.highlight_yellow);
                    spannable.setSpan(new android.text.style.ForegroundColorSpan(color), start, start + 5, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                tvTitle.setText(spannable);
            }
        } else {
            if (tvTitle != null) updateHeaderTitle();
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

        consolidatedAdapter = new GenericGuidesAdapter();

        // Set initial adapter
        guidesRecyclerView.setAdapter(adapter);

        AppDatabase db = AppDatabase.getDatabase(requireContext().getApplicationContext());
        
        // Observe First Aid Guides
        db.guideDao().getAllGuides().observe(getViewLifecycleOwner(), guides -> {
            allGuides = guides;
            if (!isShowingDisasterReadiness) {
                if (!isInitialLoadDelayed) {
                    isInitialLoadDelayed = true;
                    guidesRecyclerView.postDelayed(() -> {
                        showLoading(false);
                        applyFilters();
                        checkBookmarkAvailability();
                    }, 800);
                } else {
                    applyFilters();
                }
            }
            checkBookmarkAvailability();
        });

        // Observe Disaster Readiness Guides
        db.disasterGuideDao().getAllDisasterGuides().observe(getViewLifecycleOwner(), guides -> {
            allDisasterGuides = guides;
            if (isShowingDisasterReadiness) {
                if (!isInitialLoadDelayed) {
                    isInitialLoadDelayed = true;
                    guidesRecyclerView.postDelayed(() -> {
                        showLoading(false);
                        applyFilters();
                        checkBookmarkAvailability();
                    }, 800);
                } else {
                    applyFilters();
                }
            }
            checkBookmarkAvailability();
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
        showLoading(true);
        guidesRecyclerView.postDelayed(() -> {
            showLoading(false);
            applyFilters();
        }, 300);
    }

    private void setupBookmarkToggle() {
        View bookmarkContainer = getView().findViewById(R.id.bookmarkListCard);
        if (bookmarkContainer != null) {
            bookmarkContainer.setOnClickListener(v -> {
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
                
                updateStickyHeader(isShowingBookmarksOnly);
                showLoading(true);
                guidesRecyclerView.postDelayed(() -> {
                    showLoading(false);
                    applyFilters();
                }, 200);
            });
        }
    }

    private void checkBookmarkAvailability() {
        boolean hasAnyBookmark = false;
        if (allGuides != null) {
            for (com.example.instacare.data.local.Guide g : allGuides) {
                if (g.isBookmarked) { hasAnyBookmark = true; break; }
            }
        }
        if (!hasAnyBookmark && allDisasterGuides != null) {
            for (com.example.instacare.data.local.DisasterGuide dg : allDisasterGuides) {
                if (dg.isBookmarked) { hasAnyBookmark = true; break; }
            }
        }

        View bookmarkContainer = getView() != null ? getView().findViewById(R.id.bookmarkListCard) : null;
        if (bookmarkContainer != null) {
            bookmarkContainer.setEnabled(hasAnyBookmark);
            bookmarkContainer.setAlpha(hasAnyBookmark ? 1.0f : 0.4f);
            
            // If we are currently showing bookmarks and the last one was removed
            if (!hasAnyBookmark && isShowingBookmarksOnly) {
                isShowingBookmarksOnly = false;
                btnViewBookmarks.setImageResource(R.drawable.ic_bookmark);
                btnViewBookmarks.setImageTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), R.color.emergency_red)));
                updateStickyHeader(false);
                applyFilters();
            }
        }
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
            
            showLoading(true);
            guidesRecyclerView.postDelayed(() -> {
                showLoading(false);
                applyFilters();
            }, 200);
        });

        // Disaster sub-categories
        disasterCategoryChips.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int checkedId = checkedIds.get(0);
            currentDisasterCategory = "All";
            
            if (checkedId == R.id.chipTyphoon) currentDisasterCategory = "Typhoon";
            else if (checkedId == R.id.chipEarthquake) currentDisasterCategory = "Earthquake";
            else if (checkedId == R.id.chipFire) currentDisasterCategory = "Fire";
            
            showLoading(true);
            guidesRecyclerView.postDelayed(() -> {
                showLoading(false);
                applyFilters();
            }, 200);
        });
    }

    private void setupSearch() {
        if (searchEditText != null) {
            searchEditText.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                    showLoading(true);
                    searchRunnable = () -> {
                        showLoading(false);
                        applyFilters();
                    };
                    searchHandler.postDelayed(searchRunnable, 300);
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });
        }
    }

    private void showLoading(boolean show) {
        if (!isAdded()) return;
        requireActivity().runOnUiThread(() -> {
            if (loadingState != null) {
                loadingState.setVisibility(show ? View.VISIBLE : View.GONE);
                if (show) {
                    android.view.animation.Animation pulse = android.view.animation.AnimationUtils.loadAnimation(requireContext(), R.anim.pulse_shimmer);
                    loadingState.startAnimation(pulse);
                } else {
                    loadingState.clearAnimation();
                }
            }
            if (show) {
                guidesRecyclerView.setVisibility(View.GONE);
                View emptyGuides = getView() != null ? getView().findViewById(R.id.emptyStateGuides) : null;
                if (emptyGuides != null) emptyGuides.setVisibility(View.GONE);
                View emptyBookmarks = getView() != null ? getView().findViewById(R.id.emptyStateBookmarks) : null;
                if (emptyBookmarks != null) emptyBookmarks.setVisibility(View.GONE);
            }
        });
    }

    private void applyFilters() {
        String query = searchEditText != null && searchEditText.getText() != null ? 
                        searchEditText.getText().toString().toLowerCase().trim() : "";

        if (isShowingDisasterReadiness) {
            guidesRecyclerView.setAdapter(disasterAdapter);
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
            guidesRecyclerView.setAdapter(adapter);
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
                // AUTO-REDIRECT: If no bookmarks left in THIS section, check if there are ANY bookmarks at all
                boolean hasAnyBookmark = false;
                if (allGuides != null) { for (com.example.instacare.data.local.Guide g : allGuides) { if (g.isBookmarked) { hasAnyBookmark = true; break; } } }
                if (!hasAnyBookmark && allDisasterGuides != null) { for (com.example.instacare.data.local.DisasterGuide dg : allDisasterGuides) { if (dg.isBookmarked) { hasAnyBookmark = true; break; } } }

                if (!hasAnyBookmark) {
                    // Switch back to normal First Aid view
                    isShowingBookmarksOnly = false;
                    isShowingDisasterReadiness = false;
                    btnViewBookmarks.setImageResource(R.drawable.ic_bookmark);
                    btnViewBookmarks.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.emergency_red)));
                    updateStickyHeader(false);
                    applyFilters();
                    return;
                }

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

    private void setupDynamicCorners(com.google.android.material.appbar.AppBarLayout appBar) {
        View emptyGuides = getView() != null ? getView().findViewById(R.id.emptyStateGuides) : null;
        View emptyBookmarks = getView() != null ? getView().findViewById(R.id.emptyStateBookmarks) : null;

        appBar.addOnOffsetChangedListener((appBarLayout, verticalOffset) -> {
            float totalRange = appBarLayout.getTotalScrollRange();
            if (totalRange == 0) return;

            float percentage = Math.abs(verticalOffset) / totalRange;
            // 0.0 means fully expanded (rounded), 1.0 means fully collapsed (flat)
            float currentRadius = cornerRadiusPx * (1.0f - percentage);

            updateBackgroundCorners(guidesRecyclerView, currentRadius);
            if (emptyGuides != null) updateBackgroundCorners(emptyGuides, currentRadius);
            if (emptyBookmarks != null) updateBackgroundCorners(emptyBookmarks, currentRadius);
            if (loadingState != null) updateBackgroundCorners(loadingState, currentRadius);

            // Show/Hide Scroll-to-Top based on scroll progress
            if (getActivity() instanceof UserDashboardActivity) {
                UserDashboardActivity activity = (UserDashboardActivity) getActivity();
                boolean shouldShow = percentage > 0.8f; // Show when mostly collapsed
                activity.setScrollTopAction(shouldShow, v -> {
                    if (guidesRecyclerView != null) {
                        guidesRecyclerView.smoothScrollToPosition(0);
                        appBarLayout.setExpanded(true, true);
                    }
                });
            }
        });
    }

    private void updateBackgroundCorners(View view, float radius) {
        if (view == null) return;
        android.graphics.drawable.Drawable background = view.getBackground();
        if (background instanceof android.graphics.drawable.GradientDrawable) {
            float[] radii = {radius, radius, radius, radius, 0, 0, 0, 0};
            ((android.graphics.drawable.GradientDrawable) background).setCornerRadii(radii);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (searchEditText != null) {
            searchEditText.clearFocus();
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(searchEditText.getWindowToken(), 0);
        }
    }
}
