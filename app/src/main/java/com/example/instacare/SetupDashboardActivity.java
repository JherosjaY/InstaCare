package com.example.instacare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.example.instacare.databinding.ActivitySetupDashboardBinding;
import com.example.instacare.data.local.User;
import com.yalantis.ucrop.UCrop;
import java.io.File;

public class SetupDashboardActivity extends AppCompatActivity {

    private ActivitySetupDashboardBinding binding;
    private int currentStep = 1;
    private Uri selectedPhotoUri = null;
    private boolean isFirstLoad = true;

    // Photo Picker → UCrop
    private final ActivityResultLauncher<String> pickImageLauncher =
        registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                // Launch UCrop
                Uri destUri = Uri.fromFile(new File(getCacheDir(), "avatar_cropped.jpg"));
                boolean isDarkMode = (getResources().getConfiguration().uiMode 
                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                    == android.content.res.Configuration.UI_MODE_NIGHT_YES;
                
                int toolbarColor = isDarkMode ? android.graphics.Color.parseColor("#111827") : android.graphics.Color.WHITE;
                int toolbarTextColor = isDarkMode ? android.graphics.Color.WHITE : android.graphics.Color.parseColor("#111827");
                int activeColor = getResources().getColor(R.color.primary, null);
                
                UCrop.Options options = new UCrop.Options();
                options.setCircleDimmedLayer(true);
                options.setShowCropGrid(false);
                options.setShowCropFrame(false);
                options.setToolbarTitle("Crop Photo");
                options.setCompressionQuality(90);
                options.setToolbarColor(toolbarColor);
                options.setStatusBarColor(toolbarColor);
                options.setToolbarWidgetColor(toolbarTextColor);
                options.setActiveControlsWidgetColor(activeColor);
                options.setRootViewBackgroundColor(toolbarColor);
                options.setDimmedLayerColor(isDarkMode ? android.graphics.Color.parseColor("#CC111827") : android.graphics.Color.parseColor("#CCF9FAFB"));

                UCrop.of(uri, destUri)
                    .withAspectRatio(1, 1)
                    .withMaxResultSize(512, 512)
                    .withOptions(options)
                    .start(this);
            }
        });

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            Uri croppedUri = UCrop.getOutput(data);
            if (croppedUri != null) {
                selectedPhotoUri = croppedUri;
                binding.profileAvatar.setImageURI(croppedUri);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySetupDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Edge-to-Edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        boolean isDark = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        int barColor = isDark ? android.graphics.Color.parseColor("#111827") : android.graphics.Color.WHITE;
        getWindow().setStatusBarColor(barColor);
        getWindow().setNavigationBarColor(barColor);
        androidx.core.view.WindowInsetsControllerCompat controller =
            androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(!isDark);
            controller.setAppearanceLightNavigationBars(!isDark);
        }

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout, (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, insets.top, 0, insets.bottom);
            return androidx.core.view.WindowInsetsCompat.CONSUMED;
        });

        // Pre-fill user data
        SharedPreferences prefs = getSharedPreferences("InstaCarePrefs", MODE_PRIVATE);
        binding.setupNameInput.setText(prefs.getString("USER_NAME", ""));
        binding.setupEmailInput.setText(prefs.getString("USER_EMAIL", ""));
        binding.setupPhoneInput.setText(prefs.getString("USER_PHONE", ""));

        // Red icon tint on focus
        setupFocusIconTint(binding.setupNameLayout);
        setupFocusIconTint(binding.setupEmailLayout);
        setupFocusIconTint(binding.setupPhoneLayout);

        // Avatar tap
        View.OnClickListener avatarClick = v -> pickImageLauncher.launch("image/*");
        binding.profileAvatar.setOnClickListener(avatarClick);
        binding.cameraBadge.setOnClickListener(avatarClick);

        // Next button
        binding.nextButton.setOnClickListener(v -> {
            if (currentStep == 2) {
                String fullName = binding.setupNameInput.getText().toString().trim();
                if (fullName.isEmpty()) {
                    binding.setupNameLayout.setError("Full Name is required");
                    shakeView(binding.setupNameLayout);
                    binding.setupNameInput.requestFocus();
                    return;
                } else {
                    binding.setupNameLayout.setErrorEnabled(false);
                    binding.setupNameLayout.setError(null);
                }
            }

            if (currentStep < 3) {
                currentStep++;
                updateStep();
            } else {
                completeSetup();
            }
        });

        // Back button
        binding.backButton.setOnClickListener(v -> {
            if (currentStep > 1) {
                currentStep--;
                updateStep();
            }
        });

        // Initial state: Next button centered, Back hidden
        updateStep();
    }

    private void setupFocusIconTint(com.google.android.material.textfield.TextInputLayout layout) {
        layout.getEditText().setOnFocusChangeListener((v, hasFocus) -> {
            int color = hasFocus ?
                getResources().getColor(R.color.primary, null) :
                getResources().getColor(R.color.text_secondary, null);
            layout.setStartIconTintList(android.content.res.ColorStateList.valueOf(color));
            layout.setEndIconTintList(android.content.res.ColorStateList.valueOf(color));
        });
    }

    private void updateStep() {
        // Hide all content
        binding.step1Content.setVisibility(View.GONE);
        binding.step2Content.setVisibility(View.GONE);
        binding.step3Content.setVisibility(View.GONE);

        // Reset all circles
        binding.stepCircle1.setBackgroundResource(R.drawable.bg_step_inactive);
        binding.stepCircle2.setBackgroundResource(R.drawable.bg_step_inactive);
        binding.stepCircle3.setBackgroundResource(R.drawable.bg_step_inactive);
        int inactiveTextColor = getResources().getColor(R.color.stepper_inactive_text, null);
        binding.stepText1.setTextColor(inactiveTextColor);
        binding.stepText2.setTextColor(inactiveTextColor);
        binding.stepText3.setTextColor(inactiveTextColor);

        // Reset lines
        int inactiveLineColor = getResources().getColor(R.color.stepper_inactive, null);
        binding.stepLine1.setBackgroundColor(inactiveLineColor);
        binding.stepLine2.setBackgroundColor(inactiveLineColor);

        // Mark completed steps
        int greenColor = getResources().getColor(R.color.brand_green, null);
        for (int i = 1; i < currentStep; i++) {
            View circle = i == 1 ? binding.stepCircle1 : (i == 2 ? binding.stepCircle2 : binding.stepCircle3);
            android.widget.TextView text = i == 1 ? binding.stepText1 : (i == 2 ? binding.stepText2 : binding.stepText3);
            circle.setBackgroundResource(R.drawable.bg_step_done);
            text.setText("✓");
            text.setTextColor(android.graphics.Color.WHITE);
        }
        if (currentStep > 1) binding.stepLine1.setBackgroundColor(greenColor);
        if (currentStep > 2) binding.stepLine2.setBackgroundColor(greenColor);

        // Set current step active
        View currentCircle;
        android.widget.TextView currentText;
        switch (currentStep) {
            case 1:
                currentCircle = binding.stepCircle1;
                currentText = binding.stepText1;
                binding.step1Content.setVisibility(View.VISIBLE);
                binding.setupSubtitle.setText("Let's personalize your experience");
                break;
            case 2:
                currentCircle = binding.stepCircle2;
                currentText = binding.stepText2;
                binding.step2Content.setVisibility(View.VISIBLE);
                binding.setupSubtitle.setText("Confirm your details");
                break;
            case 3:
            default:
                currentCircle = binding.stepCircle3;
                currentText = binding.stepText3;
                binding.step3Content.setVisibility(View.VISIBLE);
                binding.setupSubtitle.setText("You're ready to go!");

                // Play check animation
                binding.completeIcon.setImageResource(R.drawable.avd_check_circle);
                android.graphics.drawable.Drawable d = binding.completeIcon.getDrawable();
                if (d instanceof android.graphics.drawable.Animatable) {
                    ((android.graphics.drawable.Animatable) d).start();
                }
                break;
        }
        currentCircle.setBackgroundResource(R.drawable.bg_step_active);
        currentText.setText(String.valueOf(currentStep));
        currentText.setTextColor(android.graphics.Color.WHITE);

        // Update button text and icon
        if (currentStep == 3) {
            binding.nextButton.setText("Let's Go!");
            binding.nextButton.setIconResource(R.drawable.ic_rocket);
            binding.nextButton.setIconGravity(com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_END);
        } else {
            binding.nextButton.setText("Next");
            binding.nextButton.setIcon(null);
        }

        // Button animation: first load = centered, after = Back slides in from left
        animateButtons();

        // Animate step content (Static on Step 3)
        View visibleContent = currentStep == 1 ? binding.step1Content :
                              (currentStep == 2 ? binding.step2Content : binding.step3Content);
        
        if (currentStep == 3) {
            visibleContent.setAlpha(1f);
            visibleContent.setTranslationY(0f);
        } else {
            visibleContent.setAlpha(0f);
            visibleContent.setTranslationY(30f);
            visibleContent.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .setInterpolator(new DecelerateInterpolator())
                .start();
        }
    }

    private void animateButtons() {
        // Use TransitionManager for smooth layout changes
        android.transition.TransitionManager.beginDelayedTransition((ViewGroup) binding.nextButton.getParent(), 
            new android.transition.AutoTransition().setDuration(300));

        if (currentStep == 1) {
            // Next centered, Back hidden
            binding.backButton.setVisibility(View.GONE);
            binding.backButton.setAlpha(0f);
            
            LinearLayout.LayoutParams nextParams = (LinearLayout.LayoutParams) binding.nextButton.getLayoutParams();
            nextParams.weight = 0;
            nextParams.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            nextParams.setMarginStart(0);
            binding.nextButton.setLayoutParams(nextParams);
            binding.nextButton.setPaddingRelative(dpToPx(48), 0, dpToPx(48), 0);
        } else {
            // Show Back button with FADE IN
            if (binding.backButton.getVisibility() == View.GONE) {
                binding.backButton.setVisibility(View.VISIBLE);
                binding.backButton.setAlpha(0f);
                binding.backButton.animate().alpha(1f).setDuration(400).start();
            }

            // Move Next to right (SLIDE RIGHT effect caused by weight change + TransitionManager)
            LinearLayout.LayoutParams nextParams = (LinearLayout.LayoutParams) binding.nextButton.getLayoutParams();
            nextParams.weight = 1;
            nextParams.width = 0;
            nextParams.setMarginStart(dpToPx(12));
            binding.nextButton.setLayoutParams(nextParams);
            binding.nextButton.setPaddingRelative(0, 0, 0, 0);

            LinearLayout.LayoutParams backParams = (LinearLayout.LayoutParams) binding.backButton.getLayoutParams();
            backParams.weight = 1;
            backParams.width = 0;
            binding.backButton.setLayoutParams(backParams);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void completeSetup() {
        String updatedName = binding.setupNameInput.getText().toString().trim();
        String updatedPhone = binding.setupPhoneInput.getText().toString().trim();

        SharedPreferences prefs = getSharedPreferences("InstaCarePrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (!updatedName.isEmpty()) {
            editor.putString("USER_NAME", updatedName);
            editor.putString("USER_FULL_NAME", updatedName);
            
            // Real-time Database Activity: Update DB
            String username = prefs.getString("USER_USERNAME", "");
            if (!username.isEmpty()) {
                new Thread(() -> {
                    com.example.instacare.data.local.UserDao userDao = 
                        com.example.instacare.data.local.AppDatabase.getDatabase(this).userDao();
                    User user = userDao.getUserByUsername(username);
                    if (user != null) {
                        userDao.updateProfile(username, updatedName, user.email, updatedPhone.isEmpty() ? user.phone : updatedPhone);
                    }
                }).start();
            }
        }
        if (!updatedPhone.isEmpty()) {
            editor.putString("USER_PHONE", updatedPhone);
        }
        if (selectedPhotoUri != null) {
            String email = prefs.getString("USER_EMAIL", "");
            // Copy cropped image to permanent per-user file (cache file is shared and gets overwritten)
            try {
                java.io.InputStream inputStream = getContentResolver().openInputStream(selectedPhotoUri);
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(inputStream);
                if (inputStream != null) inputStream.close();
                
                File permanentFile = new File(getFilesDir(), "profile_avatar_" + email + ".png");
                java.io.FileOutputStream fos = new java.io.FileOutputStream(permanentFile);
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();
                
                editor.putString("USER_AVATAR_" + email, Uri.fromFile(permanentFile).toString());
            } catch (Exception e) {
                // Fallback: save cache URI if copy fails
                editor.putString("USER_AVATAR_" + email, selectedPhotoUri.toString());
            }
        }

        editor.putBoolean("SETUP_COMPLETE", true);
        editor.apply();

        Intent intent = new Intent(this, UserDashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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
}
