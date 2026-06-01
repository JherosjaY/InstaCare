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
    private int currentSelectedMode; // Tracks the specifically selected mode before finalizing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Handle the system splash screen transition
        super.onCreate(savedInstanceState);
        
        // --- Smart Status Bar Icon Visibility Fix ---
        SessionManager sessionManager = SessionManager.getInstance(this);
        int appTheme = sessionManager.getTheme();
        boolean isAppDark;
        if (appTheme == androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES) {
            isAppDark = true;
        } else if (appTheme == androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO) {
            isAppDark = false;
        } else {
            // Follow System
            isAppDark = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) 
                        == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        }

        // Apply icons based on theme (Edge-to-edge default)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        androidx.core.view.WindowInsetsControllerCompat controller = 
            androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(!isAppDark); 
            controller.setAppearanceLightNavigationBars(!isAppDark);
        }

        // --- 1. Routing Engine (Speed-Oriented) ---
        
        // Always start in the user's preferred theme if already in a session
        if (sessionManager.isLoggedIn()) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(sessionManager.getTheme());
        } else {
            // Default visually clean Light Mode for guest/new users
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        }

        boolean onboardingDone = sessionManager.getGlobalBoolean("ONBOARDING_COMPLETE", false);
        boolean permissionsDone = sessionManager.getGlobalBoolean("PERMISSIONS_COMPLETED", false);
        boolean welcomeDone = sessionManager.getGlobalBoolean("WELCOME_GATE_PASSED", false);

        if (!onboardingDone) {
            startActivity(new Intent(this, OnboardingActivity.class));
        } else if (!permissionsDone) {
            startActivity(new Intent(this, PermissionsActivity.class));
        } else if (!welcomeDone) {
            startActivity(new Intent(this, WelcomeActivity.class));
        } else {
            // --- 2. Enhanced Session Persistence Engine ---
            if (sessionManager.isLoggedIn()) {
                String role = sessionManager.getString("USER_ROLE", "USER");
                
                if ("ADMIN".equals(role) || "BARANGAY".equals(role)) {
                    // Security Policy: Force logout for Sensitive Roles on app restart
                    sessionManager.endSession();
                    startActivity(new Intent(this, LoginActivity.class));
                } else {
                    // Convenience Policy: Maintain session for regular USER role
                    boolean isVerified = sessionManager.getBoolean("EMAIL_BOUND", false);
                    boolean setupDone = sessionManager.getBoolean("SETUP_COMPLETE", false);
                    
                    Intent targetIntent;
                    if (!isVerified || !setupDone) {
                        // SMART SESSION: If the session is incomplete (unverified or unfinished setup),
                        // force a re-login instead of auto-resuming.
                        sessionManager.endSession();
                        targetIntent = new Intent(this, LoginActivity.class);
                    } else {
                        targetIntent = new Intent(this, UserDashboardActivity.class);
                    }
                    
                    targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(targetIntent);
                }
            } else {
                // No active session
                startActivity(new Intent(this, LoginActivity.class));
            }
        }
        
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
