package com.example.instacare;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.instacare.databinding.ActivityLoginBinding;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private static boolean hasAnimatedHeader = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // --- 1. Session Policy: Always show Login on fresh start ---
        // (Removed auto-redirect to Dashboard to ensure "Opens app - Login" flow as requested)

        // Enable Edge-to-Edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        
        // Dynamic Navigation Bar Color
        boolean isDark = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        getWindow().setNavigationBarColor(getResources().getColor(R.color.surface_card, getTheme()));

        // System Bar Controllers
        androidx.core.view.WindowInsetsControllerCompat controller = 
            androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(false); // White icons for red status bar
            controller.setAppearanceLightNavigationBars(!isDark); // Dark icons for light nav, light for dark nav
        }

        // Adjust for system bars
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            
            // Apply top inset as padding to the logo section so it doesn't hit the top
            binding.logoSection.setPadding(
                binding.logoSection.getPaddingLeft(),
                insets.top + (int)(12 * getResources().getDisplayMetrics().density),
                binding.logoSection.getPaddingRight(),
                binding.logoSection.getPaddingBottom()
            );

            // Apply bottom inset to the scroll content container's padding
            binding.scrollContent.setPadding(
                binding.scrollContent.getPaddingLeft(),
                binding.scrollContent.getPaddingTop(),
                binding.scrollContent.getPaddingRight(),
                insets.bottom + (int)(24 * getResources().getDisplayMetrics().density)
            );
            return androidx.core.view.WindowInsetsCompat.CONSUMED;
        });

        // Red icon tint on focus
        setupFocusIconTint(binding.usernameInputLayout);
        setupFocusIconTint(binding.passwordInputLayout);

        // Auto-fill preserved email
        android.content.SharedPreferences prefs = getSharedPreferences("InstaCarePrefs", MODE_PRIVATE);
        String savedUsername = prefs.getString("USER_USERNAME", "");
        if (!savedUsername.isEmpty()) {
            binding.usernameEditText.setText(savedUsername);
        }

        binding.loginButton.setOnClickListener(v -> handleLogin());

        // Navigate to Forgot Password Activity
        binding.forgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });

        // Navigate to Register Activity
        binding.registerLink.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
            finish(); // Optional: finish Login so back button behaves as expected (or keep it)
        });

        if (hasAnimatedHeader) {
            // Already animated, set full visibility immediately
            binding.logoSection.setAlpha(1f);
            binding.loginTitle.setAlpha(1f);
            binding.loginSubtitle.setAlpha(1f);
            binding.loginTitle.setTranslationY(0f);
            binding.loginSubtitle.setTranslationY(0f);
        } else {
            // Initially hide for entry animation
            binding.logoSection.setAlpha(0f);
            binding.loginTitle.setAlpha(0f);
            binding.loginSubtitle.setAlpha(0f);
            binding.loginTitle.setTranslationY(40f);
            binding.loginSubtitle.setTranslationY(20f);
            
            // Trigger animations
            binding.getRoot().postDelayed(this::startLoginEntryAnimations, 100);
        }
    }

    private void startLoginEntryAnimations() {
        if (binding == null || hasAnimatedHeader) return;
        hasAnimatedHeader = true;

        // Animate Header Section Fade-in
        binding.logoSection.animate()
            .alpha(1f)
            .setDuration(600)
            .start();
            
        // Animating Title
        binding.loginTitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(700)
            .setStartDelay(200)
            .setInterpolator(new android.view.animation.DecelerateInterpolator())
            .start();
            
        // Animating Subtitle
        binding.loginSubtitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(700)
            .setStartDelay(400)
            .setInterpolator(new android.view.animation.DecelerateInterpolator())
            .start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Ensure Heartbeat Animation runs every time we return
        android.graphics.drawable.Drawable drawable = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.avd_heartbeat);
        if (drawable instanceof android.graphics.drawable.Animatable) {
            // Re-find the ImageView (Child 1 of MaterialCardView, which is Child 0 of headerSec)
            if (binding.logoSection.getChildCount() > 0 && binding.logoSection.getChildAt(0) instanceof com.google.android.material.card.MaterialCardView) {
                 com.google.android.material.card.MaterialCardView card = (com.google.android.material.card.MaterialCardView) binding.logoSection.getChildAt(0);
                 if (card.getChildCount() > 1 && card.getChildAt(1) instanceof android.widget.ImageView) {
                     android.widget.ImageView img = (android.widget.ImageView) card.getChildAt(1);
                     img.setImageDrawable(drawable);
                     ((android.graphics.drawable.Animatable) drawable).start();
                 }
            }
        }
    }

    private void handleLogin() {
        String username = binding.usernameEditText.getText().toString().trim();
        String password = binding.passwordEditText.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // --- Built-in Admin Account ---
        if (username.equals("admin") && password.equals("admin123")) {
            android.content.SharedPreferences prefs = getSharedPreferences("InstaCarePrefs", MODE_PRIVATE);
            prefs.edit()
                .putString("USER_USERNAME", username)
                .putString("USER_EMAIL", "admin@instacare.com")
                .putBoolean("IS_ADMIN", true)
                .apply();
            
            Intent adminIntent = new Intent(this, AdminDashboardActivity.class);
            adminIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(adminIntent);
            finish();
            return;
        }

        // --- Built-in Barangay Staff Account ---
        if (username.equals("barangay") && password.equals("barangay123")) {
            android.content.SharedPreferences prefs = getSharedPreferences("InstaCarePrefs", MODE_PRIVATE);
            prefs.edit()
                .putString("USER_USERNAME", username)
                .putString("USER_EMAIL", "staff@barangay.gov.ph")
                .putBoolean("IS_ADMIN", false)
                .putBoolean("IS_BARANGAY", true)
                .apply();

            Intent brgyIntent = new Intent(this, BarangayDashboardActivity.class);
            brgyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(brgyIntent);
            finish();
            return;
        }

        // Spinner Animation State
        binding.loginButton.setText(""); // Hide text
        binding.loginButton.setClickable(false); // Disable clicks but keep style
        binding.loginProgressBar.setVisibility(android.view.View.VISIBLE); // Show spinner

        // Simulate Login Delay
        // Authenticate Logic (InBackground)
        new Thread(() -> {
            com.example.instacare.data.local.UserDao userDao = com.example.instacare.data.local.AppDatabase.getDatabase(this).userDao();
            com.example.instacare.data.local.User user = userDao.loginByUsername(username, password);

            runOnUiThread(() -> {
                if (user != null) {
                    // Success: Save Session
                    android.content.SharedPreferences prefs = getSharedPreferences("InstaCarePrefs", MODE_PRIVATE);
                    prefs.edit()
                        .putString("USER_USERNAME", user.username) // Save Username
                        .putString("USER_EMAIL", user.email)
                        .putString("USER_PHONE", user.phone)
                        .putBoolean("EMAIL_BOUND", user.isVerified) // Sync verified status
                        .apply();

                    // Log Activity
                    new Thread(() -> {
                         com.example.instacare.data.local.ActivityLog log = new com.example.instacare.data.local.ActivityLog(
                                 "LOGIN", 
                                 "User logged in: " + user.email, 
                                 user.email,
                                 null,
                                 System.currentTimeMillis()
                         );
                         com.example.instacare.data.local.AppDatabase.getDatabase(LoginActivity.this).activityLogDao().insert(log);
                    }).start();

                    // Check verification status
                    if (!user.isVerified) {
                        // Redirect to verification screen
                        Intent intent = new Intent(LoginActivity.this, VerifyEmailActivity.class);
                        intent.putExtra("email", user.email);
                        startActivity(intent);
                        finish();
                    } else {
                        // Check if setup is complete
                        boolean setupDone = prefs.getBoolean("SETUP_COMPLETE", false);
                        if (!setupDone) {
                            // First time after verification — go to Setup
                            Intent intent = new Intent(LoginActivity.this, SetupDashboardActivity.class);
                            startActivity(intent);
                            finish();
                        } else {
                            // Navigate to Dashboard
                            Intent intent = new Intent(LoginActivity.this, UserDashboardActivity.class);
                            startActivity(intent);
                            finish();
                        }
                    }
                } else {
                     // Failed
                     Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                     
                     // Stop Loading Animation
                     binding.loginProgressBar.setVisibility(android.view.View.GONE);
                     binding.loginButton.setText("Sign In");
                     binding.loginButton.setAlpha(1f);
                      binding.loginButton.setClickable(true);
                }
            });
        }).start();
    }

    private void setupFocusIconTint(com.google.android.material.textfield.TextInputLayout layout) {
        if (layout.getEditText() != null) {
            layout.getEditText().setOnFocusChangeListener((v, hasFocus) -> {
                int color = hasFocus ?
                    getResources().getColor(R.color.primary, null) :
                    getResources().getColor(R.color.text_secondary, null);
                layout.setStartIconTintList(android.content.res.ColorStateList.valueOf(color));
                layout.setEndIconTintList(android.content.res.ColorStateList.valueOf(color));
            });
        }
    }
}
