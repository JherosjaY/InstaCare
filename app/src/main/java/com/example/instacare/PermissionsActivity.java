package com.example.instacare;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;

public class PermissionsActivity extends AppCompatActivity {

    public static final String EXTRA_FROM_SETTINGS = "from_settings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Force Light Mode
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);

        // Edge-to-edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);

        MaterialButton allowButton = findViewById(R.id.allowButton);
        TextView skipButton = findViewById(R.id.skipButton);
        View mainContainer = findViewById(R.id.mainContainer);

        // Apply insets
        ViewCompat.setOnApplyWindowInsetsListener(mainContainer, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(
                v.getPaddingLeft(), 
                systemBars.top, 
                v.getPaddingRight(), 
                systemBars.bottom
            );
            return insets;
        });

        // Ensure button text is correct
        allowButton.setText("Grant All Permissions");

        // Color 'Permissions' in Red
        TextView permissionTitle = findViewById(R.id.permissionTitle);
        if (permissionTitle != null) {
            String titleHtml = "App <font color='#E53935'>Permissions</font>";
            permissionTitle.setText(android.text.Html.fromHtml(titleHtml, android.text.Html.FROM_HTML_MODE_LEGACY));
        }

        allowButton.setOnClickListener(v -> requestPermissions());
        skipButton.setOnClickListener(v -> navigateToLogin());
    }

    private void requestPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        permissions.add(Manifest.permission.CALL_PHONE);
        permissions.add(Manifest.permission.READ_CONTACTS);
        permissions.add(Manifest.permission.SEND_SMS);
        
        permissionLauncher.launch(permissions.toArray(new String[0]));
    }

    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                // Navigate to Login after any interaction with system dialogs
                navigateToLogin();
            }
    );

    private void navigateToLogin() {
        SessionManager sessionManager = SessionManager.getInstance(this);
        
        // Mark Permissions as handled (either granted or skipped)
        sessionManager.putGlobalBoolean("PERMISSIONS_COMPLETED", true);
        
        if (getIntent().getBooleanExtra(EXTRA_FROM_SETTINGS, false)) {
            finish();
            return;
        }

        boolean welcomeDone = sessionManager.getGlobalBoolean("WELCOME_GATE_PASSED", false);
        if (welcomeDone) {
            startActivity(new Intent(this, LoginActivity.class));
        } else {
            startActivity(new Intent(this, WelcomeActivity.class));
        }
        finish();
    }
}
