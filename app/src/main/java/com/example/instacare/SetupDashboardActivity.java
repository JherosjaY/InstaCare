package com.example.instacare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.graphics.Color;
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
    private static final int TOTAL_STEPS = 4;
    private Uri selectedPhotoUri = null;
    private boolean isFirstLoad = true;
    private int selectedTheme = androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;

    // --- Premium Wave Animation Support ---
    public static android.graphics.Bitmap themeTransitionBitmap;
    public static int revealX, revealY, revealWidth, revealHeight;
    public static boolean isToDarkMode;
    public static boolean shouldAnimateTheme = false;

    // Photo Picker → UCrop
    private final ActivityResultLauncher<String> pickImageLauncher =
        registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            com.example.instacare.utils.BlurUtils.unblurActivityRoot(this);
            if (uri != null) {
                // Launch UCrop
                Uri destUri = Uri.fromFile(new File(getCacheDir(), "avatar_cropped.jpg"));
                // Theme Colors - ADAPTIVE Fix
                boolean isDarkMode = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;
                
                int toolbarColor = isDarkMode ? Color.parseColor("#111827") : Color.WHITE;
                int textColor = isDarkMode ? Color.WHITE : Color.BLACK;
                int bgColor = isDarkMode ? Color.BLACK : Color.parseColor("#F9FAFB");

                UCrop.Options options = new UCrop.Options();
                options.setCircleDimmedLayer(true);
                options.setShowCropGrid(false);
                options.setShowCropFrame(true);
                options.setCompressionQuality(90);

                options.setToolbarColor(toolbarColor);
                options.setStatusBarColor(toolbarColor);
                options.setToolbarWidgetColor(textColor); 
                options.setActiveControlsWidgetColor(androidx.core.content.ContextCompat.getColor(this, R.color.primary));
                options.setRootViewBackgroundColor(bgColor);
                options.setToolbarTitle("Crop Photo");
                
                // Visibility Fix: Bright Gray Rulers (Frame & Grid)
                int gridColor = android.graphics.Color.parseColor("#80FFFFFF"); 
                int frameColor = android.graphics.Color.WHITE; 
                options.setCropFrameColor(frameColor);
                options.setCropGridColor(gridColor);
                options.setCropGridColumnCount(2);
                options.setCropGridRowCount(2);

                com.example.instacare.utils.BlurUtils.blurActivityRoot(this, 15f);
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
        if (requestCode == UCrop.REQUEST_CROP) {
            com.example.instacare.utils.BlurUtils.unblurActivityRoot(this);
            if (resultCode == RESULT_OK && data != null) {
                Uri croppedUri = UCrop.getOutput(data);
                if (croppedUri != null) {
                    selectedPhotoUri = croppedUri;
                    binding.profileAvatar.setImageURI(croppedUri);
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Restore user's saved theme before creating the activity (UID-isolated via SessionManager)
        SessionManager sessionManager = SessionManager.getInstance(this);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(sessionManager.getTheme());

        super.onCreate(savedInstanceState);
        binding = ActivitySetupDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Edge-to-Edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        // --- Smart Status Bar Icon Visibility Fix ---
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
        
        // Ensure Icons adapt
        androidx.core.view.WindowInsetsControllerCompat controller = 
            androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(!isAppDark); // Dark icons for light mode
            controller.setAppearanceLightNavigationBars(!isAppDark);
        }

        // Sync system bars with header surface_card color
        int barColor = getResources().getColor(R.color.surface_card, null);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(barColor);

        // --- Premium Reveal Execution ---
        if (shouldAnimateTheme && themeTransitionBitmap != null) {
            // Reverted: Use Setup's original Vertical Slide for all transitions in this activity
            performRoundedRectThemeReveal();
            shouldAnimateTheme = false;
        }

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.rootLayout, (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            // Apply top inset as padding to the HEADER so background extends to edge but text stays safe
            binding.headerSection.setPadding(
                binding.headerSection.getPaddingLeft(),
                insets.top + dpToPx(16), // Balanced margin
                binding.headerSection.getPaddingRight(),
                binding.headerSection.getPaddingBottom()
            );
            // APPLY TO CONTENT CONTAINER, NOT ROOT, TO AVOID OVERLAY MISALIGNMENT
            binding.mainContentContainer.setPadding(0, 0, 0, insets.bottom);
            v.setPadding(0, 0, 0, 0); // Keep root unpadded for full-screen overlay
            return androidx.core.view.WindowInsetsCompat.CONSUMED;
        });

        // Pre-fill user data using Isolated SessionManager
        sessionManager = SessionManager.getInstance(this);
        // Safety: Clear contacts setup flag for this account (hardened via SessionManager partitioning)
        sessionManager.putBoolean("CONTACTS_SETUP", false);
        
        String email = sessionManager.getCurrentUserEmail();
        binding.setupNameInput.setText(sessionManager.getString("USER_NAME", ""));
        binding.setupEmailInput.setText(email);
        binding.setupPhoneInput.setText(sessionManager.getString("USER_PHONE", ""));
        // Address is now also handled via partitioned sessionManager
        binding.setupAddressInput.setText(sessionManager.getString("USER_ADDRESS", ""));

        // Red icon tint on focus
        setupFocusIconTint(binding.setupNameLayout);
        setupFocusIconTint(binding.setupEmailLayout);
        setupFocusIconTint(binding.setupPhoneLayout);
        setupFocusIconTint(binding.setupAddressLayout);
        
        // Custom TextWatcher for Phone UI animation (Progress Bar)
        binding.setupPhoneInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                int len = s.toString().trim().length();
                if (len > 0 && len < 11) {
                    binding.phoneProgress.setVisibility(View.VISIBLE);
                    binding.phoneProgress.setProgressCompat(len, true);
                } else {
                    binding.phoneProgress.setVisibility(View.GONE);
                    binding.phoneProgress.setProgressCompat(0, false);
                }
            }
        });

        // Avatar tap
        View.OnClickListener avatarClick = v -> {
            com.example.instacare.utils.BlurUtils.blurActivityRoot(this, 15f);
            pickImageLauncher.launch("image/*");
        };
        binding.profileAvatar.setOnClickListener(avatarClick);
        binding.cameraBadge.setOnClickListener(avatarClick);

        // Pre-set selected theme from SessionManager if exists
        selectedTheme = sessionManager.getTheme();

        // Theme Selection
        setupThemeSelection();

        // Next button
        binding.nextButton.setOnClickListener(v -> {
            if (currentStep == 3) { // Personal Info
                String fullName = binding.setupNameInput.getText().toString().trim();
                if (fullName.isEmpty()) {
                    binding.setupNameLayout.setError("Full Name is required");
                    shakeView(binding.setupNameLayout);
                    vibratePhone();
                    binding.setupNameInput.requestFocus();
                    return;
                } else {
                    binding.setupNameLayout.setErrorEnabled(false);
                    binding.setupNameLayout.setError(null);
                }
            }

            if (currentStep < TOTAL_STEPS) {
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

        // --- Premium Branding: Highlight specific words in Red ---
        String setupTitleText = "Setup <font color='#E53935'>Your</font> Dashboard";
        binding.setupTitle.setText(android.text.Html.fromHtml(setupTitleText, android.text.Html.FROM_HTML_MODE_LEGACY));

        // Step 3 Title (Internal to the step layout)
        if (binding.step3InfoContent.getChildAt(0) instanceof android.widget.TextView) {
            android.widget.TextView step3Title = (android.widget.TextView) binding.step3InfoContent.getChildAt(0);
            String step3TitleText = "Confirm <font color='#E53935'>Your Info</font>";
            step3Title.setText(android.text.Html.fromHtml(step3TitleText, android.text.Html.FROM_HTML_MODE_LEGACY));
        }

        // Initial state: Next button centered, Back hidden
        if (savedInstanceState != null) {
            currentStep = savedInstanceState.getInt("currentStep", 1);
        }
        updateStep();
    }

    @Override
    protected void onSaveInstanceState(android.os.Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("currentStep", currentStep);
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
        binding.step1ThemeContent.setVisibility(View.GONE);
        binding.step2PhotoContent.setVisibility(View.GONE);
        binding.step3InfoContent.setVisibility(View.GONE);
        binding.step4CompleteContent.setVisibility(View.GONE);

        // Reset all circles
        binding.stepCircle1.setBackgroundResource(R.drawable.bg_step_inactive);
        binding.stepCircle2.setBackgroundResource(R.drawable.bg_step_inactive);
        binding.stepCircle3.setBackgroundResource(R.drawable.bg_step_inactive);
        binding.stepCircle4.setBackgroundResource(R.drawable.bg_step_inactive);
        
        int inactiveTextColor = getResources().getColor(R.color.stepper_inactive_text, null);
        binding.stepText1.setTextColor(inactiveTextColor);
        binding.stepText2.setTextColor(inactiveTextColor);
        binding.stepText3.setTextColor(inactiveTextColor);
        binding.stepText4.setTextColor(inactiveTextColor);

        // Reset lines
        int inactiveLineColor = getResources().getColor(R.color.stepper_inactive, null);
        binding.stepLine1.setBackgroundColor(inactiveLineColor);
        binding.stepLine2.setBackgroundColor(inactiveLineColor);
        binding.stepLine3.setBackgroundColor(inactiveLineColor);

        // Mark completed steps
        int greenColor = getResources().getColor(R.color.brand_green, null);
        for (int i = 1; i < currentStep; i++) {
            View circle = (i == 1) ? binding.stepCircle1 : (i == 2 ? binding.stepCircle2 : (i == 3 ? binding.stepCircle3 : binding.stepCircle4));
            android.widget.TextView text = (i == 1) ? binding.stepText1 : (i == 2 ? binding.stepText2 : (i == 3 ? binding.stepText3 : binding.stepText4));
            circle.setBackgroundResource(R.drawable.bg_step_done);
            text.setText("✓");
            text.setTextColor(android.graphics.Color.WHITE);
        }
        if (currentStep > 1) binding.stepLine1.setBackgroundColor(greenColor);
        if (currentStep > 2) binding.stepLine2.setBackgroundColor(greenColor);
        if (currentStep > 3) binding.stepLine3.setBackgroundColor(greenColor);

        // Set current step active
        View currentCircle;
        android.widget.TextView currentText;
        View visibleContent;
        
        switch (currentStep) {
            case 1:
                currentCircle = binding.stepCircle1;
                currentText = binding.stepText1;
                visibleContent = binding.step1ThemeContent;
                binding.setupSubtitle.setText("Let's personalize your experience");
                break;
            case 2:
                currentCircle = binding.stepCircle2;
                currentText = binding.stepText2;
                visibleContent = binding.step2PhotoContent;
                binding.setupSubtitle.setText("Add your profile photo");
                break;
            case 3:
                currentCircle = binding.stepCircle3;
                currentText = binding.stepText3;
                visibleContent = binding.step3InfoContent;
                binding.setupSubtitle.setText("Confirm your details");
                break;
            case 4:
            default:
                currentCircle = binding.stepCircle4;
                currentText = binding.stepText4;
                visibleContent = binding.step4CompleteContent;
                binding.setupSubtitle.setText("You're ready to go!");

                // Play check animation
                binding.completeIcon.setImageResource(R.drawable.avd_check_circle);
                android.graphics.drawable.Drawable d = binding.completeIcon.getDrawable();
                if (d instanceof android.graphics.drawable.Animatable) {
                    ((android.graphics.drawable.Animatable) d).start();
                }
                break;
        }
        
        visibleContent.setVisibility(View.VISIBLE);
        currentCircle.setBackgroundResource(R.drawable.bg_step_active);
        currentText.setText(String.valueOf(currentStep));
        currentText.setTextColor(android.graphics.Color.WHITE);

        // Update button text and icon
        if (currentStep == TOTAL_STEPS) {
            binding.nextButton.setText("Let's Go!");
            binding.nextButton.setIconResource(R.drawable.ic_rocket);
            binding.nextButton.setIconGravity(com.google.android.material.button.MaterialButton.ICON_GRAVITY_TEXT_END);
        } else {
            binding.nextButton.setText("Next");
            binding.nextButton.setIcon(null);
        }

        // Button animation
        animateButtons();

        // Animate step content (Static on final step)
        if (currentStep == TOTAL_STEPS) {
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
        String updatedAddress = binding.setupAddressInput.getText().toString().trim();

        SessionManager sessionManager = SessionManager.getInstance(this);

        if (!updatedName.isEmpty()) {
            sessionManager.putString("USER_NAME", updatedName);
            sessionManager.putString("USER_FULL_NAME", updatedName);
            
            // Real-time Database Activity: Update DB (Hardened via UID)
            int currentUserId = sessionManager.getCurrentUserUid();
            if (currentUserId != -1) {
                new Thread(() -> {
                    com.example.instacare.data.local.UserDao userDao = 
                        com.example.instacare.data.local.AppDatabase.getDatabase(this).userDao();
                    User user = userDao.getUserById(currentUserId); 
                    if (user != null) {
                        userDao.updateProfileById(currentUserId, updatedName, user.email, updatedPhone.isEmpty() ? user.phone : updatedPhone, updatedAddress);
                    }
                }).start();
            }
        }
        if (!updatedPhone.isEmpty()) {
            sessionManager.putString("USER_PHONE", updatedPhone);
        }
        if (!updatedAddress.isEmpty()) {
            sessionManager.putString("USER_ADDRESS", updatedAddress);
        }
        
        // Save Per-UID Theme selection
        sessionManager.setTheme(selectedTheme);

        if (selectedPhotoUri != null) {
            String email = sessionManager.getCurrentUserEmail();
            // Copy cropped image to permanent per-user file
            try {
                java.io.InputStream inputStream = getContentResolver().openInputStream(selectedPhotoUri);
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(inputStream);
                if (inputStream != null) inputStream.close();
                
                int currentUserIdForMedia = sessionManager.getCurrentUserUid();
                File permanentFile = new File(getFilesDir(), "profile_avatar_" + currentUserIdForMedia + ".png");
                java.io.FileOutputStream fos = new java.io.FileOutputStream(permanentFile);
                bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();
                
                sessionManager.putString("USER_AVATAR", Uri.fromFile(permanentFile).toString());
            } catch (Exception e) {
                sessionManager.putString("USER_AVATAR", selectedPhotoUri.toString());
            }
        }

        sessionManager.putBoolean("SETUP_COMPLETE", true);

        Intent intent = new Intent(this, UserDashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupThemeSelection() {
        binding.setupLightModeCard.setOnClickListener(v -> {
            if (selectedTheme != androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO) {
                startThemeTransition(false, v);
            }
        });
        binding.setupDarkModeCard.setOnClickListener(v -> {
            if (selectedTheme != androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES) {
                startThemeTransition(true, v);
            }
        });
        
        // Match initial UI to default
        updateThemeUI(selectedTheme);
    }

    private void startThemeTransition(boolean toDarkMode, View anchor) {
        // 1. Capture the ENTIRE window (perfect alignment with DecorView)
        View windowView = getWindow().getDecorView();
        themeTransitionBitmap = android.graphics.Bitmap.createBitmap(windowView.getWidth(), windowView.getHeight(), android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(themeTransitionBitmap);
        windowView.draw(canvas);

        // 2. Set EXACT reveal origin from absolute Screen coordinates
        int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        
        revealWidth = anchor.getWidth();
        revealHeight = anchor.getHeight();
        revealX = location[0] + revealWidth / 2;
        revealY = location[1] + revealHeight / 2;
        
        isToDarkMode = toDarkMode;
        shouldAnimateTheme = true;
        selectedTheme = toDarkMode ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO;

        // 3. Apply and Recreate
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(selectedTheme);
        SessionManager.getInstance(this).setTheme(selectedTheme);
        
        overridePendingTransition(0, 0);
        recreate();
    }

    private void updateThemeUI(int mode) {
        selectedTheme = mode;
        boolean isDark = mode == androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES;
        float density = getResources().getDisplayMetrics().density;

        // Visual feedback for Selection
        binding.setupLightModeCard.setStrokeWidth(isDark ? 0 : (int)(2 * density));
        binding.setupDarkModeCard.setStrokeWidth(isDark ? (int)(2 * density) : 0);
        
        binding.setupLightModeCheck.setVisibility(isDark ? View.GONE : View.VISIBLE);
        binding.setupDarkModeCheck.setVisibility(isDark ? View.VISIBLE : View.GONE);
        
        // Text colors are now hardcoded in XML to stay representative (White card/Dark text vs Dark card/Light text)
    }

    private void performRoundedRectThemeReveal() {
        android.view.ViewGroup decorView = (android.view.ViewGroup) getWindow().getDecorView();

        View overlay = new View(this) {
            private android.graphics.Paint clearPaint;
            private android.graphics.Rect srcRect;
            private android.graphics.RectF dstRect;
            private android.graphics.RectF holeRect = new android.graphics.RectF();
            private float cornerRadius;

            {
                clearPaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
                clearPaint.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR));
                srcRect = new android.graphics.Rect(0, 0, themeTransitionBitmap.getWidth(), themeTransitionBitmap.getHeight());
                dstRect = new android.graphics.RectF();
                cornerRadius = 28f * getResources().getDisplayMetrics().density; // Match high-radius pill buttons
                setLayerType(View.LAYER_TYPE_HARDWARE, null); 
                setTag(0f);
            }

            @Override
            protected void onDraw(android.graphics.Canvas canvas) {
                if (themeTransitionBitmap == null || themeTransitionBitmap.isRecycled()) return;
                
                float progress = getTag() != null ? (float) getTag() : 0f;
                int saveCount = canvas.saveLayer(dstRect, null);
                
                // Draw OLD screenshot
                canvas.drawBitmap(themeTransitionBitmap, srcRect, dstRect, null);
                
                // Calculate expanding rounded rect bounds
                // Start from revealWidth/Height and expand to full width/height
                float currentWidth = revealWidth + (getWidth() * 2f * progress);
                float currentHeight = revealHeight + (getHeight() * 2f * progress);
                
                holeRect.set(
                    revealX - currentWidth / 2,
                    revealY - currentHeight / 2,
                    revealX + currentWidth / 2,
                    revealY + currentHeight / 2
                );

                canvas.drawRoundRect(holeRect, cornerRadius, cornerRadius, clearPaint);
                canvas.restoreToCount(saveCount);
            }
        };

        decorView.addView(overlay, new android.view.ViewGroup.LayoutParams(-1, -1));

        overlay.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                overlay.getViewTreeObserver().removeOnPreDrawListener(this);

                android.animation.ValueAnimator anim = android.animation.ValueAnimator.ofFloat(0, 1f);
                anim.setDuration(800);
                anim.setInterpolator(new android.view.animation.DecelerateInterpolator(1.2f));
                
                anim.addUpdateListener(animation -> {
                    overlay.setTag(animation.getAnimatedValue());
                    overlay.invalidate();
                });

                anim.addListener(new android.animation.AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(android.animation.Animator animation) {
                        decorView.removeView(overlay);
                        themeTransitionBitmap = null;
                        isToDarkMode = false;
                    }
                });
                anim.start();
                
                return true;
            }
        });
    }

    private void shakeView(android.view.View view) {
        vibratePhone();
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
                    }, 1500);
                }
            }
        });
        view.startAnimation(shake);
    }

    private void vibratePhone() {
        android.os.Vibrator v = (android.os.Vibrator) getSystemService(android.content.Context.VIBRATOR_SERVICE);
        if (v != null && v.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                v.vibrate(android.os.VibrationEffect.createOneShot(300, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(300);
            }
        }
    }
}
