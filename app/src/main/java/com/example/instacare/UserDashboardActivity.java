package com.example.instacare;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton; // Actually simple button in XML
import androidx.appcompat.widget.AppCompatButton;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.File;

public class UserDashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private AppCompatButton fabSOS;
    private int currentTabIndex = 0; // 0: Home, 1: Hospitals, 2: Guides, 3: Profile
    public boolean isFirstLoad = true; // Tracks if this is the initial session load

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_dashboard);

        // Status bar setup
        android.view.Window window = getWindow();
        int colorBg = androidx.core.content.ContextCompat.getColor(this, R.color.dashboard_background);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
        // Dynamic Navigation Bar Color
        boolean isDark = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        
        // Use Surface Color for Bottom Nav Bar blending
        int surfaceColor = androidx.core.content.ContextCompat.getColor(this, R.color.dashboard_surface);
        getWindow().setNavigationBarColor(surfaceColor);
        
        // Ensure Icons adapt
        androidx.core.view.WindowInsetsControllerCompat controller = 
            androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(!isDark); // Dark icons for light bg
            controller.setAppearanceLightNavigationBars(!isDark);
        }

        bottomNav = findViewById(R.id.bottomNav);
        fabSOS = findViewById(R.id.fabSOS);

        if (savedInstanceState == null) {
            // Load Home Fragment by default without animation
            loadFragment(new HomeFragment(), 0, false);
        } else {
            // Restore Tab Index
            currentTabIndex = savedInstanceState.getInt("CURRENT_TAB_INDEX", 0);
        }

        setupBottomNavigation();
        setupFAB();
        loadProfileImage();
    }

    @Override
    protected void onSaveInstanceState(@androidx.annotation.NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("CURRENT_TAB_INDEX", currentTabIndex);
    }

    private void setupBottomNavigation() {
        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_home) {
                if (currentTabIndex != 0) loadFragment(new HomeFragment(), 0, true);
                return true;
            } else if (itemId == R.id.nav_hospitals) {
                if (currentTabIndex != 1) loadFragment(new HospitalsFragment(), 1, true);
                return true;
            } else if (itemId == R.id.nav_placeholder) {
                return false;
            } else if (itemId == R.id.nav_guides) {
                if (currentTabIndex != 2) loadFragment(new GuidesFragment(), 2, true);
                return true;
            } else if (itemId == R.id.nav_profile) {
                if (currentTabIndex != 3) loadFragment(new ProfileFragment(), 3, true);
                return true;
            }
            return false;
        });
    }

    private void setupFAB() {
        fabSOS.setOnClickListener(v -> {
            Intent intent = new Intent(this, SOSActivity.class);
            startActivity(intent);
        });
    }

    private void loadFragment(Fragment fragment, int newIndex, boolean animate) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        if (animate) {
            // Directional Animation
            if (newIndex > currentTabIndex) {
                // Sliding Right to Left (Forward)
                transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left);
            } else {
                // Sliding Left to Right (Backward)
                transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);
            }
        }

        // Debug Toast
        // Toast.makeText(this, "Loading Fragment: " + newIndex, Toast.LENGTH_SHORT).show();

        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.commit();

        currentTabIndex = newIndex;
    }
    
    // Helper for Fragments to navigate
    public void navigateToFragment(String tag) {
        int navId = R.id.nav_home;
        
        if (tag.equals("Hospitals")) {
            navId = R.id.nav_hospitals;
        } else if (tag.equals("Guides")) {
            navId = R.id.nav_guides;
        } else if (tag.equals("Profile")) {
            navId = R.id.nav_profile;
        }
        
        bottomNav.setSelectedItemId(navId); // This triggers the listener which calls loadFragment
    }

    public void hideBottomNav() {
        View container = findViewById(R.id.bottomNavContainer);
        if (container != null) {
            container.animate().translationY(container.getHeight() + 100).setDuration(300).start();
        }
        fabSOS.animate().translationY(fabSOS.getHeight() + 100).setDuration(300).start();
    }

    public void showBottomNav() {
        View container = findViewById(R.id.bottomNavContainer);
        if (container != null) {
            container.animate().translationY(0).setDuration(300).start();
        }
        fabSOS.animate().translationY(0).setDuration(300).start();
    }

    private void loadProfileImage() {
        try {
            SharedPreferences prefs = getSharedPreferences("InstaCarePrefs", MODE_PRIVATE);
            String avatarEmail = prefs.getString("USER_EMAIL", "");
            File file = new File(getFilesDir(), "profile_avatar_" + avatarEmail + ".png");
            Bitmap bitmap = null;

            if (file.exists()) {
                bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            } else {
                // Fallback: check avatar URI from setup (per-user)
                String email = prefs.getString("USER_EMAIL", "");
                String avatarUri = prefs.getString("USER_AVATAR_" + email, null);
                if (avatarUri != null) {
                    android.graphics.ImageDecoder.Source source = 
                        android.graphics.ImageDecoder.createSource(getContentResolver(), Uri.parse(avatarUri));
                    bitmap = android.graphics.ImageDecoder.decodeBitmap(source);
                }
            }

            if (bitmap != null) {
                Bitmap squareBitmap = cropToSquare(bitmap);
                androidx.core.graphics.drawable.RoundedBitmapDrawable circularBitmapDrawable =
                    androidx.core.graphics.drawable.RoundedBitmapDrawableFactory.create(getResources(), squareBitmap);
                circularBitmapDrawable.setCircular(true);
                bottomNav.getMenu().findItem(R.id.nav_profile).setIcon(circularBitmapDrawable);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private Bitmap cropToSquare(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width == height) return bitmap;
        int newWidth = (height > width) ? width : height;
        int newHeight = (height > width) ? height - (height - width) : height;
        int cropW = (width - height) / 2;
        cropW = (cropW < 0) ? 0 : cropW;
        int cropH = (height - width) / 2;
        cropH = (cropH < 0) ? 0 : cropH;
        return Bitmap.createBitmap(bitmap, cropW, cropH, newWidth, newWidth);
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Reload image when returning to dashboard (e.g. after profile edit)
        loadProfileImage();
    }

    // ── Double-tap to exit ──
    private long lastBackPressTime = 0;
    private Toast exitToast;

    @Override
    public void onBackPressed() {
        if (currentTabIndex != 0) {
            // Navigate back to Home from any tab
            loadFragment(new HomeFragment(), 0, true);
            bottomNav.setSelectedItemId(R.id.nav_home);
        } else {
            long now = System.currentTimeMillis();
            if (now - lastBackPressTime < 2000) {
                if (exitToast != null) exitToast.cancel();
                super.onBackPressed();
            } else {
                lastBackPressTime = now;
                // Vibrate
                android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(VIBRATOR_SERVICE);
                if (vibrator != null) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        vibrator.vibrate(android.os.VibrationEffect.createOneShot(50, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        vibrator.vibrate(50);
                    }
                }
                exitToast = Toast.makeText(this, "Double tap to exit", Toast.LENGTH_SHORT);
                exitToast.show();
            }
        }
    }
}
