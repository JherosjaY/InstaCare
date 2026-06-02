package com.example.instacare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import com.google.android.material.bottomappbar.BottomAppBar;
import com.google.android.material.bottomappbar.BottomAppBarTopEdgeTreatment;
import com.google.android.material.shape.CornerFamily;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import android.widget.Toast;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.graphics.drawable.GradientDrawable;
import androidx.core.content.ContextCompat;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Shader;
import android.graphics.LinearGradient;
import android.widget.TextView;
import android.widget.ImageView;
import java.io.File;
import android.speech.tts.TextToSpeech;

public class UserDashboardActivity extends AppCompatActivity {

    private BottomAppBar bottomAppBar;
    private View btnNavHome, btnNavHospitals, btnNavGuides, btnNavProfile;
    private ImageView ivNavHome, ivNavHospitals, ivNavGuides;
    private com.google.android.material.imageview.ShapeableImageView ivNavProfile;
    private TextView tvNavHome, tvNavHospitals, tvNavGuides, tvNavProfile;
    private FloatingActionButton fabSOS;
    private View fabScrollTop;
    private int currentTabIndex = 0; // 0: Home, 1: Hospitals, 2: Guides, 3: Profile
    public boolean isFirstLoad = true; // Tracks if this is the initial session load

    // --- Premium Theme Reveal Support ---
    public static Bitmap themeTransitionBitmap;
    public static int revealX, revealY;
    public static boolean isToDarkMode;
    public static boolean isFromLogin = false; // NEW: Partition Login vs Toggle logic

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Restore user's saved theme before creating the activity (UID-isolated via SessionManager)
        SessionManager sessionManager = SessionManager.getInstance(this);
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(sessionManager.getTheme());

        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_user_dashboard);

        // --- PRE-WARM VOICE ENGINE ---
        // Warm up TTS early so it's ready when the tutorial starts
        com.example.instacare.utils.VoiceManager.getInstance(this);

        // Modern Back Navigation Handling
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (currentTabIndex != 0) {
                    loadFragment(new HomeFragment(), 0, true);
                } else {
                    long now = System.currentTimeMillis();
                    if (now - lastBackPressTime < 2000) {
                        setEnabled(false);
                        getOnBackPressedDispatcher().onBackPressed();
                        // Exit app
                    } else {
                        lastBackPressTime = now;
                        Toast.makeText(UserDashboardActivity.this, "Double tap to exit", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // Status bar setup
        android.view.Window window = getWindow();
        int colorBg = androidx.core.content.ContextCompat.getColor(this, R.color.dashboard_background);
        int surfaceColor = androidx.core.content.ContextCompat.getColor(this, R.color.dashboard_surface);
        
        // Make the status bar match the header color so it looks seamless
        window.setStatusBarColor(surfaceColor);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            boolean alreadyAsked = sessionManager.getBoolean("NOTIF_PERMISSION_ASKED", false);
            
            if (!alreadyAsked && androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
                sessionManager.putBoolean("NOTIF_PERMISSION_ASKED", true);
            }
        }
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
        
        // Use Surface Color for Bottom Nav Bar blending
        surfaceColor = androidx.core.content.ContextCompat.getColor(this, R.color.dashboard_surface);
        getWindow().setNavigationBarColor(surfaceColor);
        
        // Ensure Icons adapt
        androidx.core.view.WindowInsetsControllerCompat controller = 
            androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            if (themeTransitionBitmap != null) {
                // During reveal: keep icons matching the OLD theme (screenshot visible)
                // isToDarkMode means old=light, so light status bars (dark icons)
                // !isToDarkMode means old=dark, so dark status bars (white icons)
                controller.setAppearanceLightStatusBars(isToDarkMode);
                controller.setAppearanceLightNavigationBars(isToDarkMode);
            } else {
                controller.setAppearanceLightStatusBars(!isAppDark); // Dark icons for light mode, White for dark
                controller.setAppearanceLightNavigationBars(!isAppDark);
            }
        }

        bottomAppBar = findViewById(R.id.bottomAppBar);
        
        // Manual Nav Views
        btnNavHome = findViewById(R.id.btnNavHome);
        btnNavHospitals = findViewById(R.id.btnNavHospitals);
        btnNavGuides = findViewById(R.id.btnNavGuides);
        btnNavProfile = findViewById(R.id.btnNavProfile);
        
        ivNavHome = findViewById(R.id.ivNavHome);
        ivNavHospitals = findViewById(R.id.ivNavHospitals);
        ivNavGuides = findViewById(R.id.ivNavGuides);
        ivNavProfile = findViewById(R.id.ivNavProfile);
        
        tvNavHome = findViewById(R.id.tvNavHome);
        tvNavHospitals = findViewById(R.id.tvNavHospitals);
        tvNavGuides = findViewById(R.id.tvNavGuides);
        tvNavProfile = findViewById(R.id.tvNavProfile);
        
        fabSOS = findViewById(R.id.fabSOS);
        fabScrollTop = findViewById(R.id.fabScrollTop);

        // --- FINAL ROUNDED NOTCHED BAR FIX (Choy Edition) ---
        if (bottomAppBar != null) {
            float density = getResources().getDisplayMetrics().density;
            float cornerRadius = 32 * density;

            // 1. Get the existing Shape Model (which already has the Notch)
            // Use explicit cast to MaterialShapeDrawable for guaranteed compilation
            if (bottomAppBar.getBackground() instanceof MaterialShapeDrawable) {
                MaterialShapeDrawable shapeDrawable = (MaterialShapeDrawable) bottomAppBar.getBackground();
                ShapeAppearanceModel model = shapeDrawable.getShapeAppearanceModel()
                    .toBuilder()
                    .setTopLeftCorner(CornerFamily.ROUNDED, cornerRadius)
                    .setTopRightCorner(CornerFamily.ROUNDED, cornerRadius)
                    .build();

                // 3. Apply it BACK to the existing drawable (Don't replace background!)
                shapeDrawable.setShapeAppearanceModel(model);
            }
            
            // 4. Ensure Theme Color (Surface)
            bottomAppBar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(surfaceColor));
        }

        // --- Programmatic SOS Gradient (Flagship Fix) ---
        if (fabSOS != null) {
            int startColor = 0xFFE53935; // Red-600
            int endColor = 0xFFB71C1C;   // Red-900 (Deep Crimson)
            
            GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{startColor, endColor}
            );
            gradient.setShape(GradientDrawable.OVAL);
            
            // Set as IMAGE drawable to preserve FAB elevation/notch logic
            fabSOS.setImageDrawable(gradient);
            
            fabSOS.setImageTintList(null);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                fabSOS.setSupportImageTintList(null);
            }
            
            fabSOS.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
            fabSOS.setSupportBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
        }

        setupBottomNavigation();

        if (savedInstanceState == null) {
            // Check if we're coming from a theme switch (tab preserved in SessionManager)
            int savedTab = sessionManager.getInt("CURRENT_TAB_INDEX", -1);
            if (savedTab >= 0 && themeTransitionBitmap != null) {
                // Theme switch: restore the tab user was on
                currentTabIndex = savedTab;
                loadFragmentByIndex(savedTab, false);
            } else {
                loadFragment(new HomeFragment(), 0, false);
            }
        } else {
            currentTabIndex = savedInstanceState.getInt("CURRENT_TAB_INDEX", 0);
            updateTabUI(); // Re-apply bottom nav highlight to match restored tab
        }

        setupFAB();
        loadProfileImage();
        
        // --- SOS Label Gradient (White Sheen) ---
        TextView tvSOS = findViewById(R.id.tvSOSLabel);
        if (tvSOS != null) {
            tvSOS.post(() -> {
                Shader textShader = new LinearGradient(0, 0, 0, tvSOS.getLineHeight(),
                    new int[]{0xFFFFFFFF, 0xFFE0E0E0}, null, Shader.TileMode.CLAMP);
                tvSOS.getPaint().setShader(textShader);
                tvSOS.invalidate();
            });
        }

        // --- Premium Reveal Execution ---
        if (themeTransitionBitmap != null) {
            if (isFromLogin) {
                performScannerThemeReveal();
            } else {
                performCircularThemeReveal();
            }
        }
        
        // Clear saved tab index after restoration to avoid stale navigations
        sessionManager.remove("CURRENT_TAB_INDEX");
        
        // --- Background News Pre-fetching ---
        new com.example.instacare.utils.NewsHelper().fetchLatestNews(new com.example.instacare.utils.NewsHelper.NewsCallback() {
            @Override
            public void onSuccess(java.util.List<com.example.instacare.NewsItem> newsList) {
                com.example.instacare.utils.NewsRepository.getInstance().setLatestNews(newsList);
            }
            @Override
            public void onError(String error) {}
        });
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

    public void navigateToFragment(String tag) {
        navigateToFragment(tag, null);
    }

    public void navigateToFragment(String tag, Bundle args) {
        Fragment selectedFragment = null;
        int tabIndex = 0;
        switch (tag) {
            case "Home":       selectedFragment = new HomeFragment();       tabIndex = 0; break;
            case "Hospitals": selectedFragment = new HospitalsFragment();  tabIndex = 1; break;
            case "Evacuation": selectedFragment = new EvacuationFragment(); tabIndex = 1; break;
            case "EvacAlerts": selectedFragment = new EvacuationAnnouncementsFragment(); tabIndex = 1; break;
            case "Guides":    selectedFragment = new GuidesFragment();     tabIndex = 2; break;
            case "Profile":   selectedFragment = new ProfileFragment();    tabIndex = 3; break;
        }
        if (selectedFragment != null) {
            if (args != null) selectedFragment.setArguments(args);
            loadFragment(selectedFragment, tabIndex, true);
        }
    }

    private void performScannerThemeReveal() {
        android.view.ViewGroup decorView = (android.view.ViewGroup) getWindow().getDecorView();
        isFromLogin = false;
        View scannerOverlay = new View(this) {
            private android.graphics.Paint screenshotPaint = new android.graphics.Paint();
            private android.graphics.Paint scanLinePaint = new android.graphics.Paint();
            private android.graphics.Paint clearPaint = new android.graphics.Paint();
            private android.graphics.Rect srcRect = new android.graphics.Rect(0, 0, themeTransitionBitmap.getWidth(), themeTransitionBitmap.getHeight());
            private android.graphics.RectF dstRect = new android.graphics.RectF();
            {
                clearPaint.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR));
                clearPaint.setAntiAlias(true);
                setLayerType(View.LAYER_TYPE_HARDWARE, null);
                setTag(0f);
            }
            @Override
            protected void onDraw(android.graphics.Canvas canvas) {
                if (themeTransitionBitmap == null || themeTransitionBitmap.isRecycled()) return;
                dstRect.set(0, 0, getWidth(), getHeight());
                int saveCount = canvas.saveLayer(dstRect, null);
                canvas.drawBitmap(themeTransitionBitmap, srcRect, dstRect, screenshotPaint);
                float currentY = getTag() != null ? (float) getTag() : 0f;
                float radius = 28f * getResources().getDisplayMetrics().density;
                android.graphics.RectF revealRect = new android.graphics.RectF(0, -radius, getWidth(), currentY);
                canvas.drawRoundRect(revealRect, radius, radius, clearPaint);
                float lineHeight = 40f * getResources().getDisplayMetrics().density;
                android.graphics.LinearGradient gradient = new android.graphics.LinearGradient(0, currentY - lineHeight, 0, currentY, new int[]{0x00E53935, 0x80E53935, 0xFFE53935}, null, android.graphics.Shader.TileMode.CLAMP);
                scanLinePaint.setShader(gradient);
                canvas.drawRect(0, currentY - lineHeight, getWidth(), currentY, scanLinePaint);
                canvas.restoreToCount(saveCount);
            }
        };
        decorView.addView(scannerOverlay, new android.view.ViewGroup.LayoutParams(-1, -1));
        scannerOverlay.post(() -> {
            android.animation.ValueAnimator anim = android.animation.ValueAnimator.ofFloat(0, (float) decorView.getHeight() + 200);
            anim.setDuration(1200);
            anim.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
            anim.addUpdateListener(animation -> { scannerOverlay.setTag(animation.getAnimatedValue()); scannerOverlay.invalidate(); });
            anim.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(android.animation.Animator a) {
                    decorView.removeView(scannerOverlay);
                    themeTransitionBitmap = null;
                    applyFinalStatusBarAppearance();
                }
            });
            anim.start();
        });
    }

    private void performCircularThemeReveal() {
        android.view.ViewGroup decorView = (android.view.ViewGroup) getWindow().getDecorView();
        boolean shouldExpand = isFromLogin || isToDarkMode;
        isFromLogin = false;
        if (shouldExpand) {
            View overlay = new View(this) {
                private android.graphics.Paint clearPaint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
                private android.graphics.Rect srcRect = new android.graphics.Rect(0, 0, themeTransitionBitmap.getWidth(), themeTransitionBitmap.getHeight());
                private android.graphics.RectF dstRect = new android.graphics.RectF();
                {
                    clearPaint.setXfermode(new android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.CLEAR));
                    setLayerType(View.LAYER_TYPE_HARDWARE, null); setTag(0f);
                }
                @Override
                protected void onDraw(android.graphics.Canvas canvas) {
                    if (themeTransitionBitmap == null || themeTransitionBitmap.isRecycled()) return;
                    dstRect.set(0, 0, getWidth(), getHeight());
                    int saveCount = canvas.saveLayer(dstRect, null);
                    canvas.drawBitmap(themeTransitionBitmap, srcRect, dstRect, null);
                    float currentRadius = getTag() != null ? (float) getTag() : 0f;
                    canvas.drawCircle(revealX, revealY, currentRadius, clearPaint);
                    canvas.restoreToCount(saveCount);
                }
            };
            decorView.addView(overlay, new android.view.ViewGroup.LayoutParams(-1, -1));
            overlay.post(() -> {
                float endRadius = (float) Math.hypot(Math.max(revealX, decorView.getWidth() - revealX), Math.max(revealY, decorView.getHeight() - revealY));
                android.animation.ValueAnimator anim = android.animation.ValueAnimator.ofFloat(0, endRadius);
                anim.setDuration(800);
                anim.addUpdateListener(a -> { overlay.setTag(a.getAnimatedValue()); overlay.invalidate(); });
                anim.addListener(new android.animation.AnimatorListenerAdapter() {
                    @Override public void onAnimationEnd(android.animation.Animator a) {
                        decorView.removeView(overlay);
                        themeTransitionBitmap = null;
                        applyFinalStatusBarAppearance();
                    }
                });
                anim.start();
            });
        } else {
            final android.widget.ImageView overlay = new android.widget.ImageView(this);
            overlay.setImageBitmap(themeTransitionBitmap);
            decorView.addView(overlay, new android.view.ViewGroup.LayoutParams(-1, -1));
            overlay.post(() -> {
                float initialRadius = (float) Math.hypot(Math.max(revealX, decorView.getWidth() - revealX), Math.max(revealY, decorView.getHeight() - revealY));
                android.animation.Animator anim = android.view.ViewAnimationUtils.createCircularReveal(overlay, revealX, revealY, initialRadius, 0f);
                anim.setDuration(800);
                anim.addListener(new android.animation.AnimatorListenerAdapter() {
                    @Override public void onAnimationEnd(android.animation.Animator a) {
                        decorView.removeView(overlay);
                        themeTransitionBitmap = null;
                        applyFinalStatusBarAppearance();
                    }
                });
                anim.start();
            });
        }
    }

    /**
     * Re-apply the correct status bar icon appearance AFTER the reveal animation completes.
     * This ensures battery/clock/signal icons become WHITE in dark mode.
     */
    public void applyFinalStatusBarAppearance() {
        SessionManager sm = SessionManager.getInstance(this);
        int appTheme = sm.getTheme();
        boolean isAppDark;
        if (appTheme == androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES) {
            isAppDark = true;
        } else if (appTheme == androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO) {
            isAppDark = false;
        } else {
            isAppDark = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                        == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        }
        
        androidx.core.view.WindowInsetsControllerCompat ctrl =
            androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (ctrl != null) {
            ctrl.setAppearanceLightStatusBars(!isAppDark);
            ctrl.setAppearanceLightNavigationBars(!isAppDark);
        }
    }

    private void loadFragmentByIndex(int index, boolean animate) {
        Fragment fragment;
        switch (index) {
            case 1: fragment = new HospitalsFragment(); break;
            case 2: fragment = new GuidesFragment(); break;
            case 3: fragment = new ProfileFragment(); break;
            default: fragment = new HomeFragment(); break;
        }
        // Note: Evacuation is navigated by name tag, not tab index
        loadFragment(fragment, index, animate);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("CURRENT_TAB_INDEX", currentTabIndex);
    }

    private void setupBottomNavigation() {
        btnNavHome.setOnClickListener(v -> { if (currentTabIndex != 0) loadFragment(new HomeFragment(), 0, true); });
        btnNavHospitals.setOnClickListener(v -> { if (currentTabIndex != 1) loadFragment(new HospitalsFragment(), 1, true); });
        btnNavGuides.setOnClickListener(v -> { if (currentTabIndex != 2) loadFragment(new GuidesFragment(), 2, true); });
        btnNavProfile.setOnClickListener(v -> { if (currentTabIndex != 3) loadFragment(new ProfileFragment(), 3, true); });
        updateTabUI();
    }

    private void updateTabUI() {
        int selectedColor = ContextCompat.getColor(this, R.color.bottom_nav_selected);
        int unselectedColor = ContextCompat.getColor(this, R.color.text_secondary);
        ivNavHome.setImageTintList(android.content.res.ColorStateList.valueOf(unselectedColor));
        ivNavHospitals.setImageTintList(android.content.res.ColorStateList.valueOf(unselectedColor));
        ivNavGuides.setImageTintList(android.content.res.ColorStateList.valueOf(unselectedColor));
        
        // Fade out all labels before hiding
        fadeOutLabel(tvNavHome);
        fadeOutLabel(tvNavHospitals);
        fadeOutLabel(tvNavGuides);
        fadeOutLabel(tvNavProfile);
        
        // Reset Profile tab to unselected state by default
        tvNavProfile.setTextColor(unselectedColor);
        if (!hasCustomProfileImage()) {
            ivNavProfile.setImageTintList(android.content.res.ColorStateList.valueOf(unselectedColor));
        }
        
        switch (currentTabIndex) {
            case 0: ivNavHome.setImageTintList(android.content.res.ColorStateList.valueOf(selectedColor)); tvNavHome.setTextColor(selectedColor); fadeInLabel(tvNavHome); break;
            case 1: ivNavHospitals.setImageTintList(android.content.res.ColorStateList.valueOf(selectedColor)); tvNavHospitals.setTextColor(selectedColor); fadeInLabel(tvNavHospitals); break;
            case 2: ivNavGuides.setImageTintList(android.content.res.ColorStateList.valueOf(selectedColor)); tvNavGuides.setTextColor(selectedColor); fadeInLabel(tvNavGuides); break;
            case 3:
                tvNavProfile.setTextColor(selectedColor);
                fadeInLabel(tvNavProfile);
                // Tint default person icon red when active (skip if custom avatar is loaded)
                if (!hasCustomProfileImage()) {
                    ivNavProfile.setImageTintList(android.content.res.ColorStateList.valueOf(selectedColor));
                }
                break;
        }
    }

    /** Bounce in a tab label */
    private void fadeInLabel(TextView label) {
        label.setVisibility(View.VISIBLE);
        label.setScaleX(0f);
        label.setScaleY(0f);
        label.setAlpha(1f);
        label.animate().scaleX(1f).scaleY(1f).setDuration(1000)
            .setInterpolator(new android.view.animation.OvershootInterpolator(2.0f)).start();
    }

    /** Instantly hide a tab label (no exit animation) */
    private void fadeOutLabel(TextView label) {
        label.setVisibility(View.GONE);
    }
    
    /**
     * Check if a custom profile avatar file exists for the current user.
     * If false, the default person icon (ic_nav_profile) is showing.
     */
    private boolean hasCustomProfileImage() {
        SessionManager sm = SessionManager.getInstance(this);
        File file = new File(getFilesDir(), "profile_avatar_" + sm.getCurrentUserUid() + ".png");
        return file.exists();
    }

    private void setupFAB() { fabSOS.setOnClickListener(v -> SOSFlowManager.startFlow(this)); }

    public void dispatchAddContact() {
        HomeFragment hf = new HomeFragment(); Bundle args = new Bundle(); args.putBoolean("TRIGGER_ADD_CONTACT", true); hf.setArguments(args);
        currentTabIndex = 0; updateTabUI();
        getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, hf).commit();
    }

    private void loadFragment(Fragment fragment, int index, boolean animate) {
        if (currentTabIndex == index && index != 0 && !isFirstLoad) return;
        isFirstLoad = false;
        boolean isForward = index > currentTabIndex;
        currentTabIndex = index; updateTabUI();
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (animate) { if (isForward) ft.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left); else ft.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right); }
        ft.replace(R.id.fragmentContainer, fragment).commit();
        
        // --- GUIDES ONLY TWEAK ---
        // Hide scroll button by default on every switch
        setScrollTopAction(false, null);
    }

    public void hideBottomNav() {
        bottomAppBar.animate().translationY(bottomAppBar.getHeight() + 100).setDuration(300).start();
        fabSOS.animate().translationY(bottomAppBar.getHeight() + 100).setDuration(300).start();
        findViewById(R.id.tvSOSLabel).animate().translationY(bottomAppBar.getHeight() + 100).setDuration(300).start();
    }

    public void showBottomNav() {
        bottomAppBar.animate().translationY(0).setDuration(300).start();
        fabSOS.animate().translationY(0).setDuration(300).start();
        findViewById(R.id.tvSOSLabel).animate().translationY(0).setDuration(300).start();
    }

    public void loadProfileImage() {
        try {
            SessionManager sm = SessionManager.getInstance(this);
            File file = new File(getFilesDir(), "profile_avatar_" + sm.getCurrentUserUid() + ".png");
            Bitmap bitmap = file.exists() ? BitmapFactory.decodeFile(file.getAbsolutePath()) : null;
            if (bitmap != null) {
                Bitmap sq = cropToSquare(bitmap);
                androidx.core.graphics.drawable.RoundedBitmapDrawable rbd = androidx.core.graphics.drawable.RoundedBitmapDrawableFactory.create(getResources(), sq);
                rbd.setCircular(true); if (ivNavProfile != null) ivNavProfile.setImageDrawable(rbd);
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private Bitmap cropToSquare(Bitmap bitmap) {
        int w = bitmap.getWidth(), h = bitmap.getHeight();
        if (w == h) return bitmap;
        int size = Math.min(w, h);
        return Bitmap.createBitmap(bitmap, (w - size) / 2, (h - size) / 2, size, size);
    }

    // Removed legacy onBackPressed() to use modern OnBackPressedDispatcher in onCreate()
    
    public void setScrollTopAction(boolean show, View.OnClickListener listener) {
        if (fabScrollTop == null) return;
        
        // --- GUIDES ONLY TWEAK ---
        // Force hide if not on the Guides tab (Index 2)
        if (currentTabIndex != 2) {
            show = false;
        }

        if (show) {
            if (fabScrollTop.getVisibility() != View.VISIBLE) {
                fabScrollTop.setVisibility(View.VISIBLE);
                fabScrollTop.setAlpha(0f);
                fabScrollTop.setTranslationY(20f); // Slight slide-up start
                fabScrollTop.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(400)
                    .setInterpolator(new android.view.animation.OvershootInterpolator())
                    .start();
            }
            fabScrollTop.setOnClickListener(listener);
        } else {
            if (fabScrollTop.getVisibility() == View.VISIBLE) {
                fabScrollTop.animate()
                    .alpha(0f)
                    .translationY(20f)
                    .setDuration(300)
                    .withEndAction(() -> fabScrollTop.setVisibility(View.GONE))
                    .start();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // FORCE CLEANUP: VoiceManager's TTS engine must be shutdown correctly
        // to prevent "stale" voice threads when the app is reopened/restarted.
        com.example.instacare.utils.VoiceManager.getInstance(this).shutdown();
    }

    private long lastBackPressTime = 0;
}
