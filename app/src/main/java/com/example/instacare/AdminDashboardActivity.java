package com.example.instacare;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;
import com.example.instacare.databinding.ActivityAdminDashboardBinding;
import com.google.android.material.navigation.NavigationView;

public class AdminDashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ActivityAdminDashboardBinding binding;
    private int currentNavId = R.id.nav_overview;
    private long lastBackPressTime = 0;
    private android.widget.Toast exitToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Force Light Theme — Admin is excluded from user theme preferences
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);

        // Status bar matching gradient top — white icons/text
        android.view.Window window = getWindow();
        window.setStatusBarColor(android.graphics.Color.parseColor("#DC2626"));
        window.getDecorView().setSystemUiVisibility(0); // white icons on colored bar
        // Navy blue navigation bar (bottom system bar)
        window.setNavigationBarColor(android.graphics.Color.parseColor("#0F172A"));

        binding = ActivityAdminDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.adminToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);
        }
        binding.adminToolbar.setNavigationOnClickListener(v ->
                binding.drawerLayout.openDrawer(GravityCompat.START));

        binding.adminNavView.setNavigationItemSelectedListener(this);

        // Logout button at bottom of drawer
        binding.getRoot().findViewById(R.id.btnLogoutDrawer).setOnClickListener(v -> {
            getSharedPreferences("InstaCarePrefs", MODE_PRIVATE).edit()
                    .remove("IS_ADMIN")
                    .remove("USER_NAME")
                    .remove("USER_EMAIL")
                    .apply();
            Intent logoutIntent = new Intent(this, LoginActivity.class);
            logoutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(logoutIntent);
            finish();
        });

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new AdminOverviewFragment(), "Overview");
            binding.adminNavView.setCheckedItem(R.id.nav_overview);
        }

        // Ensure nav drawer doesn't use default purple highlights
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{-android.R.attr.state_checked}
        };
        int[] colors = new int[]{
                android.graphics.Color.parseColor("#DC2626"),
                android.graphics.Color.parseColor("#94A3B8")
        };
        android.content.res.ColorStateList navList = new android.content.res.ColorStateList(states, colors);
        binding.adminNavView.setItemIconTintList(navList);
        binding.adminNavView.setItemTextColor(navList);

        // Selection highlight background
        binding.adminNavView.setItemBackgroundResource(R.color.nav_bg_admin);
    }
    
    public void openDrawer() {
        if (binding != null && binding.drawerLayout != null) {
            binding.drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();



        // Avoid reloading if same tab
        if (id == currentNavId) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }
        currentNavId = id;

        Fragment fragment;
        String title;

        if (id == R.id.nav_content) {
            fragment = new AdminContentFragment();
            title = "Content Management";
        } else if (id == R.id.nav_hospitals) {
            fragment = new AdminHospitalsFragment();
            title = "Hospital Management";
        } else if (id == R.id.nav_users) {
            fragment = new AdminUsersFragment();
            title = "User Management";
        } else if (id == R.id.nav_monitoring) {
            fragment = new AdminMonitoringFragment();
            title = "Monitoring & Audit";
        } else if (id == R.id.nav_endorsements) {
            fragment = new AdminEndorsementsFragment();
            title = "Endorsements";
        } else {
            fragment = new AdminOverviewFragment();
            title = "Overview";
        }

        loadFragment(fragment, title);
        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    /** Called by AdminOverviewFragment quick-action buttons */
    public void navigateToSection(int navItemId) {
        binding.adminNavView.setCheckedItem(navItemId);
        onNavigationItemSelected(binding.adminNavView.getMenu().findItem(navItemId));
    }

    private void loadFragment(Fragment fragment, String title) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.adminFragmentContainer, fragment)
                .commit();
        if (binding.adminToolbar != null) {
            binding.adminToolbar.setTitle(title);
            if (fragment instanceof AdminOverviewFragment) {
                binding.adminToolbar.setVisibility(android.view.View.GONE);
            } else {
                binding.adminToolbar.setVisibility(android.view.View.VISIBLE);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        } else if (currentNavId != R.id.nav_overview) {
            // Navigate back to Overview from any sub-screen
            loadFragment(new AdminOverviewFragment(), "Overview");
            binding.adminNavView.setCheckedItem(R.id.nav_overview);
            currentNavId = R.id.nav_overview;
        } else {
            // Double-tap to exit
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
                exitToast = android.widget.Toast.makeText(this, "Double tap to exit", android.widget.Toast.LENGTH_SHORT);
                exitToast.show();
            }
        }
    }
}
