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
        // Force Light Mode
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // --- PRE-WARM VOICE ENGINE ---
        // Initialize Cara's voice early (Login page) to eliminate first-speak delay in tutorial
        com.example.instacare.utils.VoiceManager.getInstance(this);
        
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

        // Auto-fill preserved username using SessionManager
        SessionManager sessionManager = SessionManager.getInstance(this);
        String lastUser = sessionManager.getLastLoggedInUsername();
        if (!lastUser.isEmpty() && !"admin".equals(lastUser) && !"barangay".equals(lastUser)) {
            binding.usernameEditText.setText(lastUser);
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
            
            // Always restart the heartbeat AVD so ECG line persists when returning from Sign Up
            startHeartbeatAnimation();
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

        // Start heartbeat AVD
        startHeartbeatAnimation();
    }

    /** Start the ECG heartbeat animated vector drawable inside the heart icon */
    private void startHeartbeatAnimation() {
        if (binding == null) return;
        android.graphics.drawable.Drawable drawable = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.avd_heartbeat);
        if (drawable instanceof android.graphics.drawable.Animatable) {
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

    @Override
    protected void onResume() {
        super.onResume();
        checkSuspensionStatus();
    }
    
    private void checkSuspensionStatus() {
        new Thread(() -> {
            com.example.instacare.data.local.User user = com.example.instacare.data.local.AppDatabase.getDatabase(this)
                    .userDao().getUserById(SessionManager.getInstance(this).getCurrentUserUid());
            if (user != null && user.isSuspended) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Account suspended by administrator.", Toast.LENGTH_LONG).show();
                    SessionManager.getInstance(this).endSession();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            }
        }).start();
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
            SessionManager sessionManager = SessionManager.getInstance(this);
            // Admin Virtual UID: -100
            sessionManager.startSession(-100, "admin@instacare.com", "admin");
            
            sessionManager.putString("USER_ROLE", "ADMIN");
            sessionManager.putBoolean("IS_ADMIN", true);
            
            Intent adminIntent = new Intent(this, AdminDashboardActivity.class);
            adminIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(adminIntent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
            return;
        }

        // --- Built-in Barangay Staff Account ---
        if (username.equals("barangay") && password.equals("barangay123")) {
            SessionManager sessionManager = SessionManager.getInstance(this);
            // Barangay Virtual UID: -200
            sessionManager.startSession(-200, "staff@barangay.gov.ph", username);

            sessionManager.putString("USER_ROLE", "BARANGAY");
            sessionManager.putBoolean("IS_ADMIN", false);
            sessionManager.putBoolean("IS_BARANGAY", true);

            Intent brgyIntent = new Intent(this, CommunitySelectorActivity.class);
            brgyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(brgyIntent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
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
                    if (user.isSuspended) {
                        runOnUiThread(() -> {
                            Toast.makeText(LoginActivity.this, "Your account has been suspended. Please contact administrator.", Toast.LENGTH_LONG).show();
                            binding.loginProgressBar.setVisibility(android.view.View.GONE);
                            binding.loginButton.setText("Sign In");
                            binding.loginButton.setClickable(true);
                        });
                        return;
                    }
                    
                    SessionManager sessionManager = SessionManager.getInstance(LoginActivity.this);
                    // Success: Save Session via SessionManager (Using UID anchor)
                    sessionManager.startSession(user.uid, user.email, user.username);
                    
                    sessionManager.putString("USER_USERNAME", user.username);
                    sessionManager.putString("USER_EMAIL", user.email);
                    sessionManager.putString("USER_PHONE", user.phone);
                    sessionManager.putString("USER_ROLE", user.role);
                    sessionManager.putBoolean("EMAIL_BOUND", user.isVerified);
                    
                    sessionManager.putBoolean("IS_ADMIN", "ADMIN".equals(user.role));
                    sessionManager.putBoolean("IS_BARANGAY", "BARANGAY".equals(user.role));

                    // Log Activity
                    new Thread(() -> {
                         com.example.instacare.data.local.ActivityLog log = new com.example.instacare.data.local.ActivityLog(
                                 "LOGIN", 
                                 "User logged in: " + user.email + " (Role: " + user.role + ")", 
                                 user.uid,
                                 null,
                                 System.currentTimeMillis()
                         );
                         com.example.instacare.data.local.AppDatabase.getDatabase(LoginActivity.this).activityLogDao().insert(log);
                    }).start();

                    // Route based on role
                    if ("ADMIN".equals(user.role)) {
                        Intent intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    } else if ("BARANGAY".equals(user.role)) {
                        Intent intent = new Intent(LoginActivity.this, CommunitySelectorActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                        finish();
                    } else {
                        // Regular USER - Always trigger reveal animation for premium feel
                        int userTheme = sessionManager.getTheme();
                        
                        // 1. Capture Login screenshot
                        android.view.View content = getWindow().getDecorView();
                        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(content.getWidth(), content.getHeight(), android.graphics.Bitmap.Config.ARGB_8888);
                        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
                        content.draw(canvas);

                        // 2. Set EXACT reveal coordinates (center of Sign In button)
                        int[] loc = new int[2];
                        binding.loginButton.getLocationInWindow(loc); // Window relative
                        int rx = loc[0] + binding.loginButton.getWidth() / 2;
                        int ry = loc[1] + binding.loginButton.getHeight() / 2;

                        // 3. Prepare target activities with absolute coordinates and Login Signal
                        UserDashboardActivity.themeTransitionBitmap = bitmap;
                        UserDashboardActivity.revealX = rx;
                        UserDashboardActivity.revealY = ry;
                        UserDashboardActivity.isToDarkMode = (userTheme == androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
                        UserDashboardActivity.isFromLogin = true; // Signal for Expanding-only on login

                        SetupDashboardActivity.themeTransitionBitmap = bitmap;
                        SetupDashboardActivity.revealX = rx;
                        SetupDashboardActivity.revealY = ry;
                        SetupDashboardActivity.isToDarkMode = UserDashboardActivity.isToDarkMode;
                        SetupDashboardActivity.shouldAnimateTheme = true;

                        if (!user.isVerified) {
                            Intent intent = new Intent(LoginActivity.this, VerifyEmailActivity.class);
                            intent.putExtra("email", user.email);
                            startActivity(intent);
                            overridePendingTransition(0, 0); // Animation handled by reveal or subsequent activities
                            finish();
                        } else {
                            // Isolated setup check via existing sessionManager
                            boolean setupDone = sessionManager.getBoolean("SETUP_COMPLETE", false);
                            if (!setupDone) {
                                Intent intent = new Intent(LoginActivity.this, SetupDashboardActivity.class);
                                startActivity(intent);
                                overridePendingTransition(0, 0);
                                finish();
                            } else {
                                Intent intent = new Intent(LoginActivity.this, UserDashboardActivity.class);
                                startActivity(intent);
                                overridePendingTransition(0, 0);
                                finish();
                            }
                        }
                    }
                } else {
                     // Failed: Apply Shake (All Modes) & Red Border (1s Auto-Reset)
                     Toast.makeText(LoginActivity.this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                     
                     shakeView(binding.usernameInputLayout);
                     shakeView(binding.passwordInputLayout);

                     // Stop Loading Animation
                     binding.loginProgressBar.setVisibility(android.view.View.GONE);
                     binding.loginButton.setText("Sign In");
                     binding.loginButton.setAlpha(1f);
                     binding.loginButton.setClickable(true);
                }
            });
        }).start();
    }

    private void shakeView(android.view.View view) {
        android.view.animation.Animation shake = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.shake);
        shake.setAnimationListener(new android.view.animation.Animation.AnimationListener() {
            @Override public void onAnimationStart(android.view.animation.Animation a) {}
            @Override public void onAnimationRepeat(android.view.animation.Animation a) {}
            @Override
            public void onAnimationEnd(android.view.animation.Animation a) {
                if (view instanceof com.google.android.material.textfield.TextInputLayout) {
                    view.postDelayed(() -> {
                        com.google.android.material.textfield.TextInputLayout til = (com.google.android.material.textfield.TextInputLayout) view;
                        til.setError(null);
                        til.setErrorEnabled(false);
                    }, 1000);
                }
            }
        });
        view.startAnimation(shake);
    }

    private void setupPrivacyMask() {
        android.view.ViewGroup rootView = findViewById(android.R.id.content);
        if (rootView != null) {
            getLayoutInflater().inflate(R.layout.layout_privacy_mask, rootView, true);
        }
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
