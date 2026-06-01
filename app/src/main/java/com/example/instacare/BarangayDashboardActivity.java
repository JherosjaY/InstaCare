package com.example.instacare;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.view.GravityCompat;
import androidx.core.view.WindowCompat;
import androidx.fragment.app.Fragment;

import com.example.instacare.databinding.ActivityBarangayDashboardBinding;
import com.google.android.material.navigation.NavigationView;

public class BarangayDashboardActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ActivityBarangayDashboardBinding binding;
    private int currentNavId = R.id.nav_brgy_overview;
    private long lastBackPressTime = 0;
    private Toast exitToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Force Light Theme locally — Barangay is excluded from user theme preferences
        getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);

        binding = ActivityBarangayDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Restore current nav state to handle recreation smoothly
        if (savedInstanceState != null) {
            currentNavId = savedInstanceState.getInt("nav_id", R.id.nav_brgy_overview);
        }

        // --- Restore Traditional Spacing ---
        android.view.Window window = getWindow();
        window.setStatusBarColor(android.graphics.Color.parseColor("#2563EB"));
        window.getDecorView().setSystemUiVisibility(0);
        window.setNavigationBarColor(android.graphics.Color.parseColor("#F8FAFC"));

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);
        }

        // Hide toolbar if current fragment is Overview (prevents double header on recreation)
        if (currentNavId == R.id.nav_brgy_overview && binding.toolbar != null) {
            binding.toolbar.setVisibility(android.view.View.GONE);
        }

        binding.toolbar.setNavigationOnClickListener(v ->
                binding.drawerLayout.openDrawer(GravityCompat.START));

        binding.navView.setNavigationItemSelectedListener(this);

        // Logout
        binding.btnLogoutDrawer.setOnClickListener(v -> {
            SessionManager.getInstance(this).endSession();
            Intent logoutIntent = new Intent(this, LoginActivity.class);
            logoutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(logoutIntent);
            finish();
        });

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new BarangayOverviewFragment(), "Overview");
            binding.navView.setCheckedItem(R.id.nav_brgy_overview);
        } else {
            binding.navView.setCheckedItem(currentNavId);
        }

        // Ensure nav drawer doesn't use default purple highlights
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{-android.R.attr.state_checked}
        };
        int[] colors = new int[]{
                android.graphics.Color.parseColor("#2563EB"),
                android.graphics.Color.parseColor("#475569")
        };
        android.content.res.ColorStateList navList = new android.content.res.ColorStateList(states, colors);
        binding.navView.setItemIconTintList(navList);
        binding.navView.setItemTextColor(navList);
        
        // Use blue highlight for selected item
        binding.navView.setItemBackgroundResource(R.color.nav_bg_barangay);
    }

    public void openDrawer() {
        if (binding != null) {
            binding.drawerLayout.openDrawer(GravityCompat.START);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == currentNavId) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        }
        currentNavId = id;

        Fragment fragment;
        String title;

        if (id == R.id.nav_brgy_emergencies) {
            fragment = new EmergenciesFragment();
            title = "Local SOS Monitoring";
        } else if (id == R.id.nav_brgy_endorsements) {
            fragment = new BarangayEndorsementsFragment();
            Bundle args = new Bundle();
            args.putString("FILTER", "PENDING");
            fragment.setArguments(args);
            title = "Review Requests";
        } else if (id == R.id.nav_brgy_history) {
            fragment = new BarangayEndorsementsFragment();
            Bundle args = new Bundle();
            args.putString("FILTER", "HISTORY");
            fragment.setArguments(args);
            title = "Review History";
        } else if (id == R.id.nav_brgy_logs) {
            fragment = new BarangayMonitoringFragment();
            title = "Activity Logs";
        } else {
            fragment = new BarangayOverviewFragment();
            title = "Overview";
        }

        loadFragment(fragment, title);
        binding.drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    public void navigateToSection(int navItemId) {
        binding.navView.setCheckedItem(navItemId);
        onNavigationItemSelected(binding.navView.getMenu().findItem(navItemId));
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("nav_id", currentNavId);
    }

    private void loadFragment(Fragment fragment, String title) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit();
        if (binding.toolbar != null) {
            binding.toolbar.setTitle(title);
            // Hide toolbar only for Overview (which has its own hero header)
            if (fragment instanceof BarangayOverviewFragment) {
                binding.toolbar.setVisibility(View.GONE);
            } else {
                binding.toolbar.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START);
        } else if (currentNavId != R.id.nav_brgy_overview) {
            loadFragment(new BarangayOverviewFragment(), "Overview");
            binding.navView.setCheckedItem(R.id.nav_brgy_overview);
            currentNavId = R.id.nav_brgy_overview;
        } else {
            long now = System.currentTimeMillis();
            if (now - lastBackPressTime < 2000) {
                if (exitToast != null) exitToast.cancel();
                super.onBackPressed();
            } else {
                lastBackPressTime = now;
                exitToast = Toast.makeText(this, "Double tap to exit", Toast.LENGTH_SHORT);
                exitToast.show();
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
}
