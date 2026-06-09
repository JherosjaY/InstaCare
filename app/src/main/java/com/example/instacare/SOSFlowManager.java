package com.example.instacare;

import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.example.instacare.utils.BlurUtils;

public class SOSFlowManager {

    public static void startFlow(Context context) {
        if (context == null) return;
        showCautionDialog(context);
    }

    private static void showCautionDialog(Context context) {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_sos_caution, null);
        builder.setView(view);
        builder.setCancelable(false);
        androidx.appcompat.app.AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setWindowAnimations(R.style.DialogPopAnimation);
        }

        view.findViewById(R.id.btnProceedSOS).setOnClickListener(v -> {
            dialog.dismiss();
            proceedWithSOS(context);
        });

        view.findViewById(R.id.btnCancelSOS).setOnClickListener(v -> {
            dialog.dismiss();
        });

        com.example.instacare.utils.BlurUtils.applyBlur(dialog);
        dialog.show();
    }

    private static void proceedWithSOS(Context context) {
        SessionManager sessionManager = SessionManager.getInstance(context);
        int userId = sessionManager.getCurrentUserUid();

        new Thread(() -> {
            int count = com.example.instacare.data.local.AppDatabase.getDatabase(context).emergencyContactDao().getCountByUser(userId);

            if (context instanceof android.app.Activity) {
                ((android.app.Activity) context).runOnUiThread(() -> {
                    if (count > 0) {
                        showCountdown(context, "Preparing Emergency Signal", "PREPARE");
                    } else {
                        showMandatorySetupDialog(context);
                    }
                });
            }
        }).start();
    }

    private static void showEmergencySelector(Context context) {
        BottomSheetDialog selectorDialog = new BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_sos_selector, null);
        selectorDialog.setContentView(view);
        BlurUtils.applyBlur(selectorDialog, 40); // Slightly more blur for urgency

        view.findViewById(R.id.btnCrime).setOnClickListener(v -> {
            selectorDialog.dismiss();
            launchSOSActivity(context, "Crime Emergency", "Police");
        });

        view.findViewById(R.id.btnFire).setOnClickListener(v -> {
            selectorDialog.dismiss();
            launchSOSActivity(context, "Fire Emergency", "Fire Department");
        });

        view.findViewById(R.id.btnMedical).setOnClickListener(v -> {
            selectorDialog.dismiss();
            launchSOSActivity(context, "Medical Emergency", "Medical");
        });

        view.findViewById(R.id.btnDisaster).setOnClickListener(v -> {
            selectorDialog.dismiss();
            launchSOSActivity(context, "Disaster Response", "Disaster Response");
        });

        selectorDialog.show();
    }

    private static void showMandatorySetupDialog(Context context) {
        android.view.View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_mandatory_setup, null);
        
        // Highlight "Required" in Red
        android.widget.TextView tvTitle = dialogView.findViewById(R.id.tvEmergencyTitle);
        if (tvTitle != null) {
            String titleText = "Emergency Setup <font color='#E53935'>Required</font>";
            tvTitle.setText(android.text.Html.fromHtml(titleText, android.text.Html.FROM_HTML_MODE_LEGACY));
        }

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(context)
            .setView(dialogView)
            .setCancelable(false)
            .create();
        
        // --- BACK BUTTON LOCK: Prevent dismissal via system navigation ---
        dialog.setOnKeyListener((dialogInterface, keyCode, event) -> {
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK) {
                // Return true to consume the event and do nothing
                return true;
            }
            return false;
        });

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        
        dialogView.findViewById(R.id.btnAddContactNow).setOnClickListener(v -> {
            dialog.dismiss();
            if (context instanceof UserDashboardActivity) {
                ((UserDashboardActivity) context).dispatchAddContact();
            }
        });
        
        // --- PREMIUM AESTHETIC: Deep Dim + Frosted Glass Blur ---
        com.example.instacare.utils.BlurUtils.applyBlur(dialog);
        
        dialog.show();
    }

    private static void showCountdown(Context context, String label, String category) {
        BottomSheetDialog countdownDialog = new BottomSheetDialog(context, R.style.BottomSheetDialogTheme);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_sos_countdown, null);
        countdownDialog.setContentView(view);
        BlurUtils.applyBlur(countdownDialog, 60); // Maximum blur for extreme focus state
        
        // Prevent dismissal on click outside or back press during emergency countdown
        countdownDialog.setCancelable(false);

        TextView tvSelectedType = view.findViewById(R.id.tvSelectedType);
        TextView tvCountdown = view.findViewById(R.id.tvCountdown);
        View btnCancel = view.findViewById(R.id.btnCancel);

        tvSelectedType.setText(label);

        android.widget.ImageView ivWarning = view.findViewById(R.id.ivWarningIcon);
        
        // Add a continuous glowing pulse animation to the warning icon
        if (ivWarning != null) {
            android.animation.ObjectAnimator scaleX = android.animation.ObjectAnimator.ofFloat(ivWarning, "scaleX", 1f, 1.3f, 1f);
            android.animation.ObjectAnimator scaleY = android.animation.ObjectAnimator.ofFloat(ivWarning, "scaleY", 1f, 1.3f, 1f);
            scaleX.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            scaleY.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            scaleX.setDuration(800);
            scaleY.setDuration(800);
            android.animation.AnimatorSet pulse = new android.animation.AnimatorSet();
            pulse.playTogether(scaleX, scaleY);
            pulse.start();
        }

        CountDownTimer timer = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int seconds = (int) (millisUntilFinished / 1000) + 1; // display 5, 4, 3, 2, 1
                tvCountdown.setText(String.valueOf(seconds));
            }

            @Override
            public void onFinish() {
                if (countdownDialog.isShowing()) {
                    countdownDialog.dismiss();
                    if ("PREPARE".equals(category)) {
                        // Phase 2: Show Emergency Type Selector after verification
                        showEmergencySelector(context);
                    } else {
                        launchSOSActivity(context, label, category);
                    }
                }
            }
        };

        btnCancel.setOnClickListener(v -> {
            timer.cancel();
            countdownDialog.dismiss();
        });

        countdownDialog.show();
        timer.start();
    }

    private static void launchSOSActivity(Context context, String label, String category) {
        Intent intent = new Intent(context, SOSActivity.class);
        intent.putExtra("EMERGENCY_LABEL", label);
        intent.putExtra("EMERGENCY_CATEGORY", category);
        context.startActivity(intent);
    }
}
