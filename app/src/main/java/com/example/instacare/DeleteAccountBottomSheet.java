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

public class DeleteAccountBottomSheet extends BottomSheetDialogFragment {

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
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emailLayout = view.findViewById(R.id.deleteEmailLayout);
        emailInput = view.findViewById(R.id.deleteEmailInput);
        confirmBtn = view.findViewById(R.id.confirmDeleteButton);
        cancelBtn = view.findViewById(R.id.cancelDeleteButton);

        SharedPreferences prefs = requireContext().getSharedPreferences("InstaCarePrefs", Context.MODE_PRIVATE);
        String userEmail = prefs.getString("USER_EMAIL", "");

        // Initial Tint
        emailLayout.setStartIconTintList(android.content.res.ColorStateList.valueOf(
            getResources().getColor(R.color.text_secondary, null)));

        // Focus Logic for Visual Feedback
        if (emailInput != null) {
            emailInput.setOnFocusChangeListener((v, hasFocus) -> {
                int color = hasFocus ?
                    getResources().getColor(R.color.emergency_red, null) :
                    getResources().getColor(R.color.text_secondary, null);
                emailLayout.setStartIconTintList(android.content.res.ColorStateList.valueOf(color));
            });
        }

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
            
            db.userDao().deleteUserByEmail(userEmail);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    dismiss();
                    
                    // Clear session + per-user data
                    SharedPreferences.Editor ed = requireContext().getSharedPreferences("InstaCarePrefs", Context.MODE_PRIVATE).edit();
                    ed.remove("USER_NAME").remove("USER_EMAIL").remove("USER_PHONE")
                      .remove("USER_FULL_NAME").remove("SETUP_COMPLETE")
                      .remove("USER_AVATAR_" + userEmail)
                      .remove("CONTACTS_SETUP_" + userEmail).apply();
                        
                    Toast.makeText(requireContext(), "Account deleted", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    getActivity().finish();
                });
            }
        }).start();
    }
}
