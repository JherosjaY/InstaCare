package com.example.instacare;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.instacare.data.local.ActivityLog;
import com.example.instacare.data.local.AppDatabase;
import com.example.instacare.databinding.ActivityRegisterBinding;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private static boolean hasAnimatedHeader = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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
            
            // Apply top inset as padding to the header section
            binding.headerSec.setPadding(
                binding.headerSec.getPaddingLeft(),
                insets.top + (int)(12 * getResources().getDisplayMetrics().density),
                binding.headerSec.getPaddingRight(),
                binding.headerSec.getPaddingBottom()
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
        setupFocusIconTint(binding.usernameLayout);
        setupFocusIconTint(binding.emailLayout);
        setupFocusIconTint(binding.phoneLayout);
        setupFocusIconTint(binding.passwordLayout);
        setupFocusIconTint(binding.confirmLayout);

        if (hasAnimatedHeader) {
            // Already animated, set full visibility immediately
            binding.headerSec.setAlpha(1f);
            binding.registerTitle.setAlpha(1f);
            binding.registerSubtitle.setAlpha(1f);
            binding.registerTitle.setTranslationY(0f);
            binding.registerSubtitle.setTranslationY(0f);
        } else {
            // Initially hide for entry animation
            binding.headerSec.setAlpha(0f);
            binding.registerTitle.setAlpha(0f);
            binding.registerSubtitle.setAlpha(0f);
            binding.registerTitle.setTranslationY(40f);
            binding.registerSubtitle.setTranslationY(20f);
            
            // Trigger animations
            binding.getRoot().postDelayed(this::startRegisterEntryAnimations, 100);
        }

        binding.registerButton.setOnClickListener(v -> handleRegister());
        
        binding.loginLink.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        // Phone digit counter (visual progress bar)
        binding.phoneInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                int len = s.toString().trim().length();
                if (len > 0 && len < 11) {
                    binding.phoneProgress.setVisibility(android.view.View.VISIBLE);
                    binding.phoneProgress.setProgressCompat(len, true);
                } else {
                    binding.phoneProgress.setVisibility(android.view.View.GONE);
                    binding.phoneProgress.setProgressCompat(0, false);
                }
            }
        });

        // Advanced "Flaticon" Style Animation (Heartbeat + ECG) for Register Screen
        android.graphics.drawable.Drawable drawable = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.avd_heartbeat);
        if (drawable instanceof android.graphics.drawable.Animatable) {
            // binding.headerSec is the LinearLayout.
            // Child 0 is now the MaterialCardView (we just added it).
            // Child 0 of CardView is ImageView.
            if (binding.headerSec.getChildCount() > 0 && binding.headerSec.getChildAt(0) instanceof com.google.android.material.card.MaterialCardView) {
                 com.google.android.material.card.MaterialCardView card = (com.google.android.material.card.MaterialCardView) binding.headerSec.getChildAt(0);
                 // The CardView now has 2 children: Background View (0) and ImageView (1)
                 if (card.getChildCount() > 1 && card.getChildAt(1) instanceof android.widget.ImageView) {
                     android.widget.ImageView img = (android.widget.ImageView) card.getChildAt(1);
                     img.setImageDrawable(drawable);
                     ((android.graphics.drawable.Animatable) drawable).start();
                 }
            }
        }
    }

    private void startRegisterEntryAnimations() {
        if (binding == null || hasAnimatedHeader) return;
        hasAnimatedHeader = true;

        // Animate Header Section Fade-in
        binding.headerSec.animate()
            .alpha(1f)
            .setDuration(600)
            .start();

        // Animating Title
        binding.registerTitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(700)
            .setStartDelay(200)
            .setInterpolator(new android.view.animation.DecelerateInterpolator())
            .start();
            
        // Animating Subtitle
        binding.registerSubtitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(700)
            .setStartDelay(400)
            .setInterpolator(new android.view.animation.DecelerateInterpolator())
            .start();
    }

    private void handleRegister() {
        // Real implementation starts here:
        String username = binding.usernameInput.getText().toString().trim();
        String email = binding.emailInput.getText().toString().trim(); 
        String phone = binding.phoneInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString();
        String confirm = binding.confirmInput.getText().toString();

        // Clear previous errors
        binding.usernameLayout.setError(null);
        binding.emailLayout.setError(null);
        binding.phoneLayout.setError(null);
        binding.passwordLayout.setError(null);
        binding.confirmLayout.setError(null);

        // --- 1. Empty Field Check ---
        boolean hasError = false;
        if (username.isEmpty() || username.contains(" ")) {
            binding.usernameLayout.setError("Valid Username required (no spaces)");
            shakeView(binding.usernameLayout);
            hasError = true;
        }
        if (email.isEmpty()) {
            binding.emailLayout.setError("Email is required");
            shakeView(binding.emailLayout);
            hasError = true;
        }
        if (phone.isEmpty()) {
            binding.phoneLayout.setError("Phone number is required");
            shakeView(binding.phoneLayout);
            hasError = true;
        }
        if (password.isEmpty()) {
            binding.passwordLayout.setError("Password is required");
            shakeView(binding.passwordLayout);
            hasError = true;
        }
        if (confirm.isEmpty()) {
            binding.confirmLayout.setError("Confirm your password");
            shakeView(binding.confirmLayout);
            hasError = true;
        }
        if (hasError) return;

        // --- 2. Email Format Validation ---
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.setError("Enter a valid email address");
            shakeView(binding.emailLayout);
            binding.emailInput.requestFocus();
            return;
        }

        // --- 3. PH Phone Number Validation ---
        if (!isValidPhoneNumber(phone)) {
            binding.phoneLayout.setError("Enter a valid PH number (09XXXXXXXXX or +639XXXXXXXXX)");
            shakeView(binding.phoneLayout);
            binding.phoneInput.requestFocus();
            return;
        }

        // --- 3. Password Strength Validation ---
        if (password.length() < 8) {
            binding.passwordLayout.setError("Password must be at least 8 characters");
            shakeView(binding.passwordLayout);
            binding.passwordInput.requestFocus();
            return;
        }
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{}|;':\",.\\<\\>?/~`].*")) {
            binding.passwordLayout.setError("Password must contain at least 1 special character");
            shakeView(binding.passwordLayout);
            binding.passwordInput.requestFocus();
            return;
        }

        // --- 4. Password Match ---
        if (!password.equals(confirm)) {
            binding.confirmLayout.setError("Passwords do not match");
            shakeView(binding.confirmLayout);
            binding.confirmInput.requestFocus();
            return;
        }

        // Spinner Animation State
        binding.registerButton.setText(""); // Hide text
        binding.registerButton.setIcon(null); // Hide icon
        binding.registerButton.setClickable(false); // Disable clicks but keep style
        binding.registerProgressBar.setVisibility(android.view.View.VISIBLE); // Show spinner

        // Simulate Registration Delay
        // Simulate Registration Delay (and save to DB in background)
        new Thread(() -> {
            com.example.instacare.data.local.UserDao userDao = com.example.instacare.data.local.AppDatabase.getDatabase(this).userDao();
            
            // Check if username exists
            if (userDao.checkUsernameExists(username) != null) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Username already taken", Toast.LENGTH_SHORT).show();
                    // Restore State
                    binding.registerProgressBar.setVisibility(android.view.View.GONE);
                    binding.registerButton.setText("Create Account");
                    binding.registerButton.setIcon(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_arrow_right));
                    binding.registerButton.setClickable(true);
                });
                return;
            }

            // Check if email exists
            if (userDao.checkEmailExists(email) != null) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Email already registered", Toast.LENGTH_SHORT).show();
                    // Restore State
                    binding.registerProgressBar.setVisibility(android.view.View.GONE);
                    binding.registerButton.setText("Create Account");
                    binding.registerButton.setIcon(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_arrow_right));
                    binding.registerButton.setClickable(true);
                });
                return;
            }

            // Create User (Save Username to DB)
            com.example.instacare.data.local.User newUser = new com.example.instacare.data.local.User(username, email, password, phone);
            userDao.registerUser(newUser);

            // Log Registration Activity
            ActivityLog log = new ActivityLog(
                    "REGISTER",
                    "New user account created: " + email,
                    email,
                    null,
                    System.currentTimeMillis()
            );
            AppDatabase.getDatabase(this).activityLogDao().insert(log);

            // Save Session
            android.content.SharedPreferences prefs = getSharedPreferences("InstaCarePrefs", MODE_PRIVATE);
            prefs.edit()
                .putString("USER_USERNAME", username) // Save Username
                .putString("USER_EMAIL", email)
                .putString("USER_PHONE", phone)
                .apply();

            runOnUiThread(() -> {
                // Restore State
                binding.registerProgressBar.setVisibility(android.view.View.GONE);
                binding.registerButton.setText("Create Account");
                binding.registerButton.setIcon(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_arrow_right));
                binding.registerButton.setClickable(true);

                // Pass email to VerifyEmailActivity
                Intent intent = new Intent(RegisterActivity.this, VerifyEmailActivity.class);
                intent.putExtra("email", email);
                startActivity(intent);
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

    private boolean isValidPhoneNumber(String phone) {
        // PH formats: 09XXXXXXXXX (11 digits) or +639XXXXXXXXX (13 chars)
        String cleaned = phone.replaceAll("[\\s\\-()]", "");
        return cleaned.matches("^09\\d{9}$") || cleaned.matches("^\\+639\\d{9}$");
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
