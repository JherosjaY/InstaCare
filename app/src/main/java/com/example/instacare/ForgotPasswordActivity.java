package com.example.instacare;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.instacare.databinding.ActivityForgotPasswordBinding;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ActivityForgotPasswordBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Enable Edge-to-Edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        getWindow().setNavigationBarColor(android.graphics.Color.TRANSPARENT);

        // System Bar Controllers (White Icons for Red Background)
        androidx.core.view.WindowInsetsControllerCompat controller = 
            androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(false); // White icons
            controller.setAppearanceLightNavigationBars(false); // White icons
        }

        // Adjust for system bars (Allow background to bleed, only padding the insets if necessary)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            // We don't apply padding to the root anymore so the background bleeds fully.
            // If the card hits the top/bottom on small screens, we could add padding to the container.
            return windowInsets;
        });

        setupFocusIconTint(binding.emailInputLayout);

        binding.backToLoginLink.setOnClickListener(v -> finish());
        binding.resetButton.setOnClickListener(v -> handleReset());
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

    private void handleReset() {
        String email = binding.emailInput.getText().toString();
        if (email.isEmpty()) {
            binding.emailInput.setError("Email is required");
            return;
        }

        binding.resetButton.setEnabled(false);
        binding.resetButton.setText("Sending...");

        new Handler().postDelayed(() -> {
            binding.resetButton.setEnabled(true);
            binding.resetButton.setText("Send Reset Link");
            Toast.makeText(this, "Reset link sent to " + email, Toast.LENGTH_LONG).show();
            // Optional: Finish or stay? Usually stay or go explicitly to a confirmation screen.
            // For now, let's just show the toast.
        }, 1500);
    }
}
