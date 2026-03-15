package com.example.instacare;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.example.instacare.databinding.ActivityOnboardingBinding;
import android.animation.ValueAnimator;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

public class OnboardingActivity extends AppCompatActivity {

    private OnboardingAdapter adapter;
    private android.widget.LinearLayout indicatorLayout;
    private ActivityOnboardingBinding binding;
    private ValueAnimator smoothScrollAnimator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Enable edge-to-edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);
        
        super.onCreate(savedInstanceState);
        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        indicatorLayout = binding.indicatorLayout;

        // Apply insets for edge-to-edge
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.bottomContainer, (v, insets) -> {
            androidx.core.graphics.Insets systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom + (int)(24 * getResources().getDisplayMetrics().density));
            return insets;
        });

        // Setup ViewPager adapter with premium illustrations
        java.util.List<OnboardingItem> items = new java.util.ArrayList<>();
        items.add(new OnboardingItem(R.drawable.onboarding_first_aid_kit, "Instant First Aid", "Get step-by-step guidance for emergencies instantly."));
        items.add(new OnboardingItem(R.drawable.onboarding_nearby_hospitals, "Nearby Hospitals", "Locate nearest hospitals and medical centers effortlessly."));
        items.add(new OnboardingItem(R.drawable.onboarding_sos_calling, "Emergency SOS", "Alert emergency services and family with just one tap."));

        adapter = new OnboardingAdapter(items);
        binding.viewPager.setAdapter(adapter);
        
        // Add PageTransformer for the fade-out effect requested
        // Add Cool Parallax + Depth PageTransformer
        binding.viewPager.setPageTransformer((page, position) -> {
            int pageWidth = page.getWidth();

            if (position < -1) { // Way off-screen to the left
                page.setAlpha(0f);
            } else if (position <= 1) { // [-1,1]
                // Depth Effect: Scale down the page as it moves away
                float scaleFactor = Math.max(0.85f, 1 - Math.abs(position));
                page.setScaleX(scaleFactor);
                page.setScaleY(scaleFactor);

                // Parallax Effect: Move background image slower than the foreground
                android.view.View image = page.findViewById(R.id.imageOnboarding);
                if (image != null) {
                    image.setTranslationX(-position * (pageWidth / 2)); // 0.5x speed
                }

                // Text Effect: Fade out faster and slide slightly
                android.view.View text = page.findViewById(R.id.textContainer);
                if (text != null) {
                    text.setAlpha(1 - Math.abs(position));
                    text.setTranslationX(position * (pageWidth / 4)); // Slide smoothly
                }

                // Alpha Fade for the whole page based on scale
                page.setAlpha(0.5f + (scaleFactor - 0.85f) / (1 - 0.85f) * (1 - 0.5f));

            } else { // Way off-screen to the right
                page.setAlpha(0f);
            }
        });

        setupIndicators(items.size());
        setupIndicators(items.size());
        
        // Initial state
        updateIndicators(0, 0f);

        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels);
                // Update indicators continuously during scroll
                updateIndicators(position, positionOffset);
            }

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // No drastic changes here, handled by onPageScrolled
            }
        });
        
        binding.nextBtn.setOnClickListener(v -> {
            int current = binding.viewPager.getCurrentItem();
            if (current < items.size() - 1) {
                // Use custom premium smooth scroll
                smoothScrollToPageWithFakeDrag(current + 1);
            } else {
                // Mark Onboarding as Complete
                getSharedPreferences("InstaCarePrefs", MODE_PRIVATE)
                        .edit()
                        .putBoolean("ONBOARDING_COMPLETE", true)
                        .apply();

                startActivity(new Intent(this, PermissionsActivity.class));
                finish();
            }
        });

        // Add entry animation for the first page
        binding.viewPager.post(() -> {
            View firstPage = binding.viewPager.getChildAt(0);
            if (firstPage instanceof androidx.recyclerview.widget.RecyclerView) {
                androidx.recyclerview.widget.RecyclerView rv = (androidx.recyclerview.widget.RecyclerView) firstPage;
                View itemView = rv.findViewHolderForAdapterPosition(0).itemView;
                if (itemView != null) {
                    View image = itemView.findViewById(R.id.imageOnboarding);
                    View text = itemView.findViewById(R.id.textContainer);
                    
                    if (image != null) {
                        image.setScaleX(0.8f);
                        image.setScaleY(0.8f);
                        image.setAlpha(0f);
                        image.animate().scaleX(1.0f).scaleY(1.0f).alpha(1.0f).setDuration(800).setInterpolator(new android.view.animation.OvershootInterpolator()).start();
                    }
                    if (text != null) {
                        text.setTranslationY(100f);
                        text.setAlpha(0f);
                        text.animate().translationY(0f).alpha(1.0f).setDuration(600).setStartDelay(200).start();
                    }
                }
            }
        });
    }

    /**
     * Premium smooth scroll using Fake Drag for precise motion control
     */
    private void smoothScrollToPageWithFakeDrag(int targetPage) {
        if (binding.viewPager.isFakeDragging()) return; // Already moving

        int currentItem = binding.viewPager.getCurrentItem();
        int pxToDrag = binding.viewPager.getWidth() * (targetPage - currentItem);
        
        // Handle rapid clicks: Cancel previous animation if running
        if (smoothScrollAnimator != null && smoothScrollAnimator.isRunning()) {
            smoothScrollAnimator.cancel();
        }

        smoothScrollAnimator = ValueAnimator.ofInt(0, pxToDrag);
        smoothScrollAnimator.setDuration(500);
        smoothScrollAnimator.setInterpolator(new FastOutSlowInInterpolator());
        
        final int[] previousValue = {0};

        smoothScrollAnimator.addUpdateListener(animation -> {
            int currentValue = (int) animation.getAnimatedValue();
            int deltaPx = currentValue - previousValue[0];
            
            if (binding.viewPager.isFakeDragging()) {
                binding.viewPager.fakeDragBy(-deltaPx);
            }
            previousValue[0] = currentValue;
        });

        smoothScrollAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                binding.viewPager.beginFakeDrag();
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (binding.viewPager.isFakeDragging()) {
                    binding.viewPager.endFakeDrag();
                }
            }
        });

        smoothScrollAnimator.start();
    }

    private void setupIndicators(int count) {
        indicatorLayout.removeAllViews(); // Clear existing
        android.widget.ImageView[] indicators = new android.widget.ImageView[count];
        
        // Resolve theme-aware inactive color
        int inactiveColor = 0xFFBDBDBD; // Default fallback
        android.util.TypedValue typedValue = new android.util.TypedValue();
        if (getTheme().resolveAttribute(R.attr.onboardingInactiveIndicatorColor, typedValue, true)) {
            inactiveColor = typedValue.data;
        }

        float density = getResources().getDisplayMetrics().density;
        int size = (int) (6 * density);
        int margin = (int) (4 * density); // Reduced margin for tighter look

        for (int i = 0; i < count; i++) {
            indicators[i] = new android.widget.ImageView(this);
            
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                    size, 
                    size
            );
            params.setMargins(margin, 0, margin, 0);
            indicators[i].setLayoutParams(params);

            // Start with inactive drawable
            android.graphics.drawable.Drawable inactiveDrawable = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.indicator_inactive);
            if (inactiveDrawable != null) {
                android.graphics.drawable.Drawable mutableDrawable = inactiveDrawable.mutate();
                mutableDrawable.setTint(inactiveColor);
                indicators[i].setImageDrawable(mutableDrawable);
            }
            indicatorLayout.addView(indicators[i]);
        }
    }

    private void updateIndicators(int position, float positionOffset) {
        int childCount = indicatorLayout.getChildCount();
        if (childCount == 0) return;

        // Define sizes in pixels for the animation
        float density = getResources().getDisplayMetrics().density;
        int activeWidth = (int) (20 * density);
        int inactiveSize = (int) (6 * density);
        int diff = activeWidth - inactiveSize;

        // Resolve theme colors
        int activeColor = androidx.core.content.ContextCompat.getColor(this, R.color.primary);
        int inactiveColor = 0xFFBDBDBD;
        android.util.TypedValue typedValue = new android.util.TypedValue();
        if (getTheme().resolveAttribute(R.attr.onboardingInactiveIndicatorColor, typedValue, true)) {
            inactiveColor = typedValue.data;
        }

        for (int i = 0; i < childCount; i++) {
            android.widget.ImageView imageView = (android.widget.ImageView) indicatorLayout.getChildAt(i);
            android.widget.LinearLayout.LayoutParams lp = (android.widget.LinearLayout.LayoutParams) imageView.getLayoutParams();
            
            float width = inactiveSize;
            float alpha = 0.5f;
            boolean isActive = false;

            if (i == position) {
                // Current position: shrinking as we scroll away (offset 0 -> 1)
                width = activeWidth - (diff * positionOffset);
                alpha = 1.0f - (0.5f * positionOffset);
                isActive = true; // Still considered active/transitioning
            } else if (i == position + 1) {
                // Next position: growing as we scroll towards (offset 0 -> 1)
                width = inactiveSize + (diff * positionOffset);
                alpha = 0.5f + (0.5f * positionOffset);
                isActive = true; // Transitioning to active
            }

            lp.width = (int) width;
            lp.height = inactiveSize; // Keep height constant to avoid jumping
            imageView.setLayoutParams(lp);
            imageView.setAlpha(alpha);

            // Swap drawable based on dominance to ensure color correctness
            // Or ideally use a color filter transition. 
            // Simple approach: usage active drawable if width > threshold or simply tint.
            // But indicator_active has corners, inactive is oval. 
            // Let's stick to inactive shape for all but change width? 
            // No, the user wants the "pill" look.
            // Let's use GradientDrawable to support dynamic corner radius if needed, 
            // but for now swapping drawables might cause flicker.
            
            // Better approach: Use a custom drawable or tint. 
            // Since active is a rounded rect and inactive is a circle (rounded rect with radius = height/2).
            // We can just use the active drawable (rounded rect) for the "expanding" one.
            
            if (width > inactiveSize + (diff * 0.1f)) {
                 // It's expanding or active
                 imageView.setImageDrawable(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.indicator_active));
                 if (i == position) {
                     // Fading out
                     // Ideally mix colors: active -> inactive
                 }
            } else {
                 android.graphics.drawable.Drawable inactiveDrawable = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.indicator_inactive);
                 if (inactiveDrawable != null) {
                    android.graphics.drawable.Drawable mutableDrawable = inactiveDrawable.mutate();
                    mutableDrawable.setTint(inactiveColor);
                    imageView.setImageDrawable(mutableDrawable);
                 }
            }
        }
    }
}
