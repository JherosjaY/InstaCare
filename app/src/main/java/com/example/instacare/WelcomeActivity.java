package com.example.instacare;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;

import com.example.instacare.databinding.ActivityWelcomeBinding;

public class WelcomeActivity extends AppCompatActivity {

    private ActivityWelcomeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Force Light Mode
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        
        // Edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);

        binding = ActivityWelcomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Save that welcome has been seen using SessionManager
        SessionManager.getInstance(this).putGlobalBoolean("WELCOME_GATE_PASSED", true);

        setupListeners();
        styleWelcomeTitle();
        startEntryAnimations();
    }

    private void styleWelcomeTitle() {
        String titleText = "Care Starts Here";
        android.text.SpannableStringBuilder spannable = new android.text.SpannableStringBuilder(titleText);
        
        int redColor = androidx.core.content.ContextCompat.getColor(this, R.color.emergency_red);
        
        // Color "Care" (0-4)
        spannable.setSpan(new android.text.style.ForegroundColorSpan(redColor), 0, 4, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        // Color "Here" (12-16)
        spannable.setSpan(new android.text.style.ForegroundColorSpan(redColor), 12, 16, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        binding.welcomeTitle.setText(spannable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Ensure ECG Animation runs every time we return
        android.graphics.drawable.Drawable drawable = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.avd_heartbeat);
        if (drawable != null) {
             // Explicitly set tint to white to ensure visibility on the red card background
            androidx.core.graphics.drawable.DrawableCompat.setTint(drawable, android.graphics.Color.WHITE);
            if (drawable instanceof android.graphics.drawable.Animatable) {
                binding.welcomeLogo.setImageDrawable(drawable);
                ((android.graphics.drawable.Animatable) drawable).start();
            }
        }
    }

    private void setupListeners() {
        binding.btnSignIn.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        binding.btnSignUp.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });
    }

    private void startEntryAnimations() {
        // Initial state
        binding.welcomeContent.setAlpha(0f);
        binding.welcomeContent.setTranslationY(60f);
        binding.logoCard.setScaleX(0.7f);
        binding.logoCard.setScaleY(0.7f);
        binding.btnSignIn.setTranslationY(100f);
        binding.btnSignUp.setTranslationY(100f);
        binding.btnSignIn.setAlpha(0f);
        binding.btnSignUp.setAlpha(0f);

        // Content Animation (Logo + Title + Subtitle)
        binding.welcomeContent.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(800)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();

        // Logo Pop Animation
        binding.logoCard.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(1000)
                .setStartDelay(200)
                .setInterpolator(new OvershootInterpolator())
                .start();

        // Button Animations
        binding.btnSignIn.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(700)
                .setStartDelay(500)
                .setInterpolator(new OvershootInterpolator())
                .start();

        binding.btnSignUp.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(700)
                .setStartDelay(650)
                .setInterpolator(new OvershootInterpolator())
                .start();
    }

    private void setupPrivacyMask() {
        android.view.ViewGroup rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            getLayoutInflater().inflate(R.layout.layout_privacy_mask, rootView, true);
        }
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
