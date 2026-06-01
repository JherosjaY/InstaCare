package com.example.instacare;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class DeleteAccountBottomSheet extends BaseBlurredBottomSheet {

    private TextInputLayout emailLayout;
    private TextInputEditText emailInput;
    private MaterialButton confirmBtn, cancelBtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_delete_account, container, false);
    }

    @Override
    public int getTheme() {
        return R.style.BottomSheetDialogTheme;
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emailLayout = view.findViewById(R.id.deleteEmailLayout);
        emailInput = view.findViewById(R.id.deleteEmailInput);
        confirmBtn = view.findViewById(R.id.confirmDeleteButton);
        cancelBtn = view.findViewById(R.id.cancelDeleteButton);

        SessionManager sessionManager = SessionManager.getInstance(requireContext());
        String userEmail = sessionManager.getCurrentUserEmail();

        // Initial Tint
        emailLayout.setStartIconTintList(android.content.res.ColorStateList.valueOf(
            getResources().getColor(R.color.text_secondary, null)));

        // Focus Logic for Visual Feedback + Focus Mode
        if (emailInput != null) {
            emailInput.setOnFocusChangeListener((v, hasFocus) -> {
                int color = hasFocus ?
                    getResources().getColor(R.color.emergency_red, null) :
                    getResources().getColor(R.color.text_secondary, null);
                emailLayout.setStartIconTintList(android.content.res.ColorStateList.valueOf(color));

                // Focus Mode: Hide buttons when typing to clear space
                if (hasFocus) {
                    if (confirmBtn != null) confirmBtn.setVisibility(View.GONE);
                    if (cancelBtn != null) cancelBtn.setVisibility(View.GONE);
                }
            });
        }

        // Global Layout Listener to detect keyboard dismissal (e.g. via Back button)
        // and restore buttons even if the field is still "focused"
        view.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            if (isAdded() && getDialog() != null) {
                android.graphics.Rect r = new android.graphics.Rect();
                view.getWindowVisibleDisplayFrame(r);
                int screenHeight = view.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;

                // If keypad is less than 15% of screen height, it's likely hidden
                if (keypadHeight < screenHeight * 0.15) {
                    if (confirmBtn != null) confirmBtn.setVisibility(View.VISIBLE);
                    if (cancelBtn != null) cancelBtn.setVisibility(View.VISIBLE);
                }
            }
        });

        cancelBtn.setOnClickListener(v -> dismiss());

        confirmBtn.setOnClickListener(v -> {
            String inputEmail = emailInput.getText() != null ? emailInput.getText().toString().trim() : "";

            if (inputEmail.isEmpty()) {
                emailLayout.setError("Please enter your email");
                return;
            }

            if (!inputEmail.equals(userEmail)) {
                emailLayout.setError("Email doesn't match your account");
                return;
            }

            // Execute deletion
            performAccountDeletion(userEmail);
        });
    }

    private void performAccountDeletion(String userEmail) {
        new Thread(() -> {
            com.example.instacare.data.local.AppDatabase db = 
                com.example.instacare.data.local.AppDatabase.getDatabase(requireContext());
            
            // 1. Get current user details via SessionManager (Hardened source)
            SessionManager sessionManager = SessionManager.getInstance(requireContext());
            int userId = sessionManager.getCurrentUserUid();

            if (userId == -1) {
                getActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Error: Session expired", Toast.LENGTH_SHORT).show());
                return;
            }

            // 2. Cleanup Alert Notifications (linked to Alerts via logic)
            java.util.List<Integer> alertIds = db.emergencyAlertDao().getAlertIdsByUser(userId);
            if (alertIds != null) {
                for (Integer id : alertIds) {
                    db.alertNotificationDao().deleteByAlertId(id);
                }
            }

            // 3. Cleanup User-linked records (Hardened via UID)
            db.emergencyAlertDao().deleteByUserId(userId);
            db.activityLogDao().deleteByUserId(userId);
            db.chatMessageDao().deleteByUserId(userId);
            db.emergencyContactDao().deleteAllByUser(userId);
            db.endorsementDao().deleteByUserId(userId);
            db.notificationDao().deleteByUserId(userId);
            db.assistantMessageDao().deleteAllMessagesForUser(userId);

            // 4. Delete User account (Final step)
            db.userDao().deleteUserById(userId);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    dismiss();
                    
                    // THEME HYGIENE: sessionManager already resets to Light Mode in clearAllDataForCurrentUser()
                    sessionManager.clearAllDataForCurrentUser();
                    sessionManager.endSession();
                   
                    // Ensure any other cached flags are gone
                    requireContext().getSharedPreferences("OnboardingPrefs", Context.MODE_PRIVATE).edit().clear().apply();
                        
                    Toast.makeText(requireContext(), "Account Deleted", Toast.LENGTH_SHORT).show();

                    // Return to the very first screen (WelcomeActivity)
                    Intent intent = new Intent(getActivity(), WelcomeActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    getActivity().finish();
                });
            }
        }).start();
    }
}
