package com.example.instacare;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.view.animation.AccelerateInterpolator;
import androidx.appcompat.app.AppCompatActivity;
import com.example.instacare.databinding.ActivityThemeSelectionBinding;

public class ThemeSelectionActivity extends AppCompatActivity {

    private ActivityThemeSelectionBinding binding;
    private static boolean hasAnimatedHeader = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Handle the system splash screen transition
        androidx.core.splashscreen.SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);

        // --- 1. Load Theme Preferences & Apply Theme ---
        android.content.SharedPreferences prefs = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean("is_first_launch", true);
        int defaultTheme = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
        
        int savedTheme = prefs.getInt("theme_mode", defaultTheme);
        
        if (isFirstLaunch) {
            prefs.edit().putBoolean("is_first_launch", false).putInt("theme_mode", androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO).apply();
            savedTheme = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;
        }
        
        // Force apply the theme
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(savedTheme);

        // --- 2. Inflate Layout & Set Content View ---
        binding = ActivityThemeSelectionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // --- 3. Check Persistence -> Route to Login or Stay ---
        android.content.SharedPreferences appPrefs = getSharedPreferences("InstaCarePrefs", MODE_PRIVATE);
        boolean onboardingDone = appPrefs.getBoolean("ONBOARDING_COMPLETE", false);

        if (onboardingDone) {
            // Intro already finished once -> Direct to Login as requested
            startActivity(new Intent(this, LoginActivity.class));
            overridePendingTransition(0, 0);
            finish();
            return;
        }

        // --- 4. Entry Animations ---
        if (hasAnimatedHeader) {
            setFinalVisibilityState();
        } else {
            binding.themeLogo.setAlpha(0f);
            binding.themeTitle.setAlpha(0f);
            binding.themeSubtitle.setAlpha(0f);
            binding.themeTitle.setTranslationY(80f);
            binding.themeSubtitle.setTranslationY(40f);
            binding.getRoot().postDelayed(this::startEntryAnimations, 100);
        }
        
        // --- 5. Setup Click Listeners & Initial State ---
        updateCardSelection(savedTheme);
        applySystemBarSync(savedTheme);

        binding.lightModeCard.setOnClickListener(v -> {
            applyTheme(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        });

        binding.darkModeCard.setOnClickListener(v -> {
            applyTheme(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        });

        binding.themeContinueBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
        });
    }

    private void applyTheme(int mode) {
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(mode);
        getSharedPreferences("ThemePrefs", MODE_PRIVATE).edit().putInt("theme_mode", mode).apply();
        updateCardSelection(mode);
        applySystemBarSync(mode);
        
        // Disable recreation animation for instant feel
        overridePendingTransition(0, 0);
    }

    private void applySystemBarSync(int mode) {
        boolean isDark = mode == androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;
        // background_dark = #111827
        int barColor = isDark ? android.graphics.Color.parseColor("#111827") : android.graphics.Color.WHITE;
        
        getWindow().setStatusBarColor(barColor);
        getWindow().setNavigationBarColor(barColor);
        
        androidx.core.view.WindowInsetsControllerCompat controller = 
            androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(!isDark);
            controller.setAppearanceLightNavigationBars(!isDark);
        }
    }

    private void setFinalVisibilityState() {
        if (binding == null) return;
        binding.themeLogo.setAlpha(1f);
        binding.themeTitle.setAlpha(1f);
        binding.themeSubtitle.setAlpha(1f);
        binding.themeTitle.setTranslationY(0f);
        binding.themeSubtitle.setTranslationY(0f);
        binding.themeLogo.setScaleX(1.0f);
        binding.themeLogo.setScaleY(1.0f);
    }

    private void startEntryAnimations() {
        if (binding == null || hasAnimatedHeader) return;
        hasAnimatedHeader = true;

        // Entry animations for Logo
        binding.themeLogo.animate()
            .alpha(1f)
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(500)
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    binding.themeLogo.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).start();
                }
            }).start();
        
        // Title Fade In + Slide Up - Snappier Feel
        binding.themeTitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(800) // Reduced from 1800 for snappier feel
            .setStartDelay(200)
            .setInterpolator(new android.view.animation.DecelerateInterpolator())
            .withEndAction(() -> {
                binding.themeTitle.setAlpha(1f);
                binding.themeTitle.setTranslationY(0f);
            })
            .start();
            
        // Subtitle Fade In + Slide Up - Quick Stagger
        binding.themeSubtitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(800)
            .setStartDelay(400) // Staggered for flow
            .setInterpolator(new android.view.animation.DecelerateInterpolator())
            .withEndAction(() -> {
                binding.themeSubtitle.setAlpha(1f);
                binding.themeSubtitle.setTranslationY(0f);
            })
            .start();
    }

    private void updateCardSelection(int mode) {
        boolean isDark = mode == androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;
        
        binding.lightModeCard.setStrokeWidth(isDark ? 0 : 4);
        binding.darkModeCard.setStrokeWidth(isDark ? 4 : 0);
        
        if (isDark) {
            binding.darkModeCard.setStrokeColor(getColor(R.color.primary));
            binding.darkModeCheck.setVisibility(View.VISIBLE);
            binding.lightModeCheck.setVisibility(View.GONE);
        } else {
            binding.lightModeCard.setStrokeColor(getColor(R.color.primary));
            binding.lightModeCheck.setVisibility(View.VISIBLE);
            binding.darkModeCheck.setVisibility(View.GONE);
        }
    }
}
