package com.example.instacare;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import com.example.instacare.databinding.ActivityVerifyEmailBinding;

public class VerifyEmailActivity extends AppCompatActivity {

    private ActivityVerifyEmailBinding binding;

    private boolean isCodeSent = false;
    private boolean isVerified = false;
    private android.os.CountDownTimer resendTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVerifyEmailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Enable Edge-to-Edge
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        
        // Dynamic Navigation Bar Color — match red background
        getWindow().setNavigationBarColor(android.graphics.Color.parseColor("#EF4444"));

        // System Bar Controllers
        androidx.core.view.WindowInsetsControllerCompat controller = 
            androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(false);
            controller.setAppearanceLightNavigationBars(false);
        }

        // Adjust for system bars
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, insets.top, 0, 0);
            return androidx.core.view.WindowInsetsCompat.CONSUMED;
        });

        setupInputs();
        setupListeners();
        
        // Initial State: Inputs Disabled and Blurred
        setInputState(false);
        binding.verifyButton.setText("Send Code");
        binding.resendCode.setVisibility(android.view.View.INVISIBLE); // Hide initially

        String email = getIntent().getStringExtra("email");
        if (email != null) {
            String fullText = "We've sent a code to " + email;
            android.text.SpannableString spannable = new android.text.SpannableString(fullText);
            int start = fullText.indexOf(email);
            int end = start + email.length();
            spannable.setSpan(new android.text.style.ForegroundColorSpan(androidx.core.content.ContextCompat.getColor(this, R.color.highlight_yellow)), start, end, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            binding.emailText.setText(spannable);
        }

        // Start mail wiggle animation (loops via Handler since sequential sets can't repeatCount)
        startMailWiggle();
    }

    private void startMailWiggle() {
        if (isVerified) return;
        android.graphics.drawable.Drawable d = binding.verificationIcon.getDrawable();
        if (d instanceof android.graphics.drawable.Animatable) {
            ((android.graphics.drawable.Animatable) d).start();
        }
        // Replay every 2.3s (800ms animation + 1500ms pause)
        binding.verificationIcon.postDelayed(this::startMailWiggle, 2300);
    }

    // Removed manual Handler animation logic
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (resendTimer != null) {
            resendTimer.cancel();
        }
    }

    private void setInputState(boolean enabled) {
        float alpha = enabled ? 1.0f : 0.5f;
        binding.code1.setEnabled(enabled); binding.code1.setAlpha(alpha);
        binding.code2.setEnabled(enabled); binding.code2.setAlpha(alpha);
        binding.code3.setEnabled(enabled); binding.code3.setAlpha(alpha);
        binding.code4.setEnabled(enabled); binding.code4.setAlpha(alpha);
        
        if (enabled) binding.code1.requestFocus();
    }

    private void setupInputs() {
        setupOTPInput(binding.code1, binding.code2);
        setupOTPInput(binding.code2, binding.code3);
        setupOTPInput(binding.code3, binding.code4);
        setupOTPInput(binding.code4, null);
    }

    private void setupOTPInput(final android.widget.EditText current, final android.widget.EditText next) {
        current.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1 && next != null) next.requestFocus();
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void setupListeners() {
        binding.verifyButton.setOnClickListener(v -> handleMainAction());
        binding.resendCode.setOnClickListener(v -> handleResend());
    }

    private void handleMainAction() {
        if (!isCodeSent) {
            // "Send Code" Action
            android.widget.Toast.makeText(this, "Code sent to email!", android.widget.Toast.LENGTH_SHORT).show();
            
            isCodeSent = true;
            binding.verifyButton.setText("Verify Email");
            setInputState(true);
            
            startResendTimer();
        } else {
            // "Verify Email" Action
            handleVerifyLogic();
        }
    }

    private void handleVerifyLogic() {
        String code = binding.code1.getText().toString() + binding.code2.getText().toString() +
                      binding.code3.getText().toString() + binding.code4.getText().toString();

        if (code.length() < 4) {
            android.widget.Toast.makeText(this, "Please enter the complete 4-digit code", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        // Only "1234" is valid
        if (!code.equals("1234")) {
            // Shake all OTP boxes
            android.view.animation.Animation shake = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.shake);
            binding.otpRow.startAnimation(shake);

            // Flash boxes red briefly
            android.widget.EditText[] boxes = {binding.code1, binding.code2, binding.code3, binding.code4};
            for (android.widget.EditText box : boxes) {
                box.setBackgroundResource(R.drawable.bg_otp_box_error);
                box.postDelayed(() -> {
                    box.setBackgroundResource(R.drawable.bg_otp_box);
                    box.setText("");
                }, 1000);
            }
            binding.code1.postDelayed(() -> binding.code1.requestFocus(), 1050);

            android.widget.Toast.makeText(this, "Invalid code. Please try again.", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        isVerified = true;
        // Switch to Animated Checkmark
        binding.verificationIcon.setImageResource(R.drawable.avd_check_circle);
        binding.verificationIcon.setImageTintList(null); // Clear tint so AVD colors show (Green/White)
        
        android.graphics.drawable.Drawable drawable = binding.verificationIcon.getDrawable();
        if (drawable instanceof android.graphics.drawable.Animatable) {
            ((android.graphics.drawable.Animatable) drawable).start();
        }

        // Spinner Animation
        binding.verifyButton.setText("");
        binding.verifyButton.setIcon(null);
        binding.verifyButton.setClickable(false);
        binding.verifyProgressBar.setVisibility(android.view.View.VISIBLE);

        // Mark user as verified in DB
        String email = getIntent().getStringExtra("email");
        new Thread(() -> {
            if (email != null) {
                com.example.instacare.data.local.AppDatabase.getDatabase(this).userDao().verifyUser(email);
            }
            
            // Sync to SharedPreferences for the rest of the app Flow
            android.content.SharedPreferences prefs = getSharedPreferences("InstaCarePrefs", MODE_PRIVATE);
            prefs.edit().putBoolean("EMAIL_BOUND", true).apply();
            
            runOnUiThread(() -> {
                new Handler().postDelayed(() -> {
                    // Go directly to Setup Dashboard
                    Intent intent = new Intent(VerifyEmailActivity.this, SetupDashboardActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }, 1500);
            });
        }).start();
    }

    private void startResendTimer() {
        binding.resendCode.setVisibility(android.view.View.VISIBLE);
        binding.resendCode.setEnabled(false);
        binding.resendCode.setAlpha(0.7f);

        if (resendTimer != null) resendTimer.cancel();

        resendTimer = new android.os.CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                binding.resendCode.setText("Resend code in " + (millisUntilFinished / 1000) + "s");
            }

            @Override
            public void onFinish() {
                binding.resendCode.setText("Resend Code");
                binding.resendCode.setEnabled(true);
                binding.resendCode.setAlpha(1.0f);
            }
        }.start();
    }

    private void handleResend() {
        android.widget.Toast.makeText(this, "Code resent!", android.widget.Toast.LENGTH_SHORT).show();
        startResendTimer();
    }
}
