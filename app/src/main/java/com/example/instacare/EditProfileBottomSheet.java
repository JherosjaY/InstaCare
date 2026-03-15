package com.example.instacare;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentResultListener;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class EditProfileBottomSheet extends BottomSheetDialogFragment {

    private ImageView editProfileImage;
    private TextInputEditText inputName, inputPhone, inputEmail;
    private SharedPreferences prefs;
    private boolean isAvatarChanged = false;

    // Image Handling
    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    startCrop(uri);
                }
            }
    );

    private final ActivityResultLauncher<Intent> cropImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri resultUri = UCrop.getOutput(result.getData());
                    if (resultUri != null) {
                        saveProfileImage(resultUri);
                        if (editProfileImage != null) {
                            editProfileImage.setImageURI(resultUri);
                            isAvatarChanged = true;
                            checkIfModified();
                        }
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        if (getContext() == null) return;
        prefs = requireContext().getSharedPreferences("InstaCarePrefs", Context.MODE_PRIVATE);

        // Initialize Views
        editProfileImage = view.findViewById(R.id.editProfileImage);
        inputName = view.findViewById(R.id.inputName);
        inputPhone = view.findViewById(R.id.inputPhone);
        inputEmail = view.findViewById(R.id.inputEmail);
        MaterialButton saveButton = view.findViewById(R.id.saveButton);

        // Load Data
        loadUserData();
        loadProfileImageFromFile();
        
        // Add TextWatcher to dynamically toggle verified state when email matches saved
        inputEmail.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                String currentSavedEmail = prefs.getString("USER_EMAIL", "");
                String newEmail = s.toString().trim();
                boolean isEmailBound = prefs.getBoolean("EMAIL_BOUND", false);
                View currentView = getView();
                if (currentView != null) {
                    com.google.android.material.textfield.TextInputLayout inputLayoutEmail = currentView.findViewById(R.id.inputLayoutEmail);
                    if (isEmailBound && newEmail.equals(currentSavedEmail)) {
                        inputLayoutEmail.setEndIconVisible(true);
                        inputEmail.setTextColor(Color.parseColor("#4CAF50"));
                    } else {
                        inputLayoutEmail.setEndIconVisible(false);
                        inputEmail.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_primary));
                    }
                }
                checkIfModified();
            }
        });
        
        android.text.TextWatcher genericWatcher = new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                checkIfModified();
            }
        };
        inputName.addTextChangedListener(genericWatcher);
        inputPhone.addTextChangedListener(genericWatcher);

        // Listeners
        editProfileImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        saveButton.setOnClickListener(v -> handleSave());
        
        // Initial state check
        checkIfModified();
    }

    private void checkIfModified() {
        if (getView() == null) return;
        
        String currentName = inputName.getText() != null ? inputName.getText().toString().trim() : "";
        String currentPhone = inputPhone.getText() != null ? inputPhone.getText().toString().trim() : "";
        String currentEmail = inputEmail.getText() != null ? inputEmail.getText().toString().trim() : "";

        String savedName = prefs.getString("USER_NAME", "");
        String savedPhone = prefs.getString("USER_PHONE", "");
        String savedEmail = prefs.getString("USER_EMAIL", "");

        boolean isModified = isAvatarChanged || 
                            !currentName.equals(savedName) || 
                            (!currentPhone.isEmpty() && !currentPhone.equals(savedPhone)) || 
                            !currentEmail.equals(savedEmail);

        MaterialButton saveButton = getView().findViewById(R.id.saveButton);
        if (saveButton != null) {
            if (isModified) {
                saveButton.setEnabled(true);
                saveButton.setAlpha(1.0f);
            } else {
                saveButton.setEnabled(false);
                saveButton.setAlpha(0.5f);
            }
        }
    }

    @Override
    public int getTheme() {
        return R.style.BottomSheetDialogTheme;
    }

    // Transparent background for bottom sheet corners
    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void loadUserData() {
        String username = prefs.getString("USER_USERNAME", "");
        if (!username.isEmpty() && getContext() != null) {
            new Thread(() -> {
                com.example.instacare.data.local.User user = 
                    com.example.instacare.data.local.AppDatabase.getDatabase(requireContext()).userDao().getUserByUsername(username);
                if (user != null && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (inputName != null) inputName.setText(user.fullName != null ? user.fullName : "");
                        if (inputEmail != null) inputEmail.setText(user.email != null ? user.email : "");
                        if (inputPhone != null) inputPhone.setText(user.phone != null ? user.phone : "");
                        
                        // Sync prefs
                        prefs.edit()
                            .putString("USER_NAME", user.fullName)
                            .putString("USER_EMAIL", user.email)
                            .putString("USER_PHONE", user.phone)
                            .apply();
                    });
                } else if (getActivity() != null) {
                    // Fallback to prefs
                    getActivity().runOnUiThread(() -> {
                        if (inputName != null) inputName.setText(prefs.getString("USER_NAME", ""));
                        if (inputEmail != null) inputEmail.setText(prefs.getString("USER_EMAIL", ""));
                        if (inputPhone != null) inputPhone.setText(prefs.getString("USER_PHONE", ""));
                    });
                }
            }).start();
        }

        boolean isEmailBound = prefs.getBoolean("EMAIL_BOUND", false);
        View view = getView();
        if (view != null) {
            com.google.android.material.textfield.TextInputLayout inputLayoutEmail = view.findViewById(R.id.inputLayoutEmail);
            if (isEmailBound) {
                inputLayoutEmail.setEndIconVisible(true);
                inputEmail.setTextColor(Color.parseColor("#4CAF50")); // success_green
            } else {
                inputLayoutEmail.setEndIconVisible(false);
                inputEmail.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_primary));
            }
        }
    }
    
    private void loadProfileImageFromFile() {
        String avatarEmail = prefs.getString("USER_EMAIL", "");
        File file = new File(requireContext().getFilesDir(), "profile_avatar_" + avatarEmail + ".png");
        if (file.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            editProfileImage.setImageBitmap(bitmap);
        } else {
            // Fallback: check avatar URI from setup
            String email = prefs.getString("USER_EMAIL", "");
            String avatarUri = prefs.getString("USER_AVATAR_" + email, null);
            if (avatarUri != null) {
                try {
                    android.graphics.ImageDecoder.Source source = 
                        android.graphics.ImageDecoder.createSource(requireContext().getContentResolver(), Uri.parse(avatarUri));
                    Bitmap bitmap = android.graphics.ImageDecoder.decodeBitmap(source);
                    editProfileImage.setImageBitmap(bitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handleSave() {
        boolean isEmailBound = prefs.getBoolean("EMAIL_BOUND", false);
        String currentSavedEmail = prefs.getString("USER_EMAIL", "");
        String newEmail = inputEmail.getText() != null ? inputEmail.getText().toString().trim() : "";

        if (!isEmailBound || !newEmail.equals(currentSavedEmail)) {
            showVerifyEmailDialog();
        } else {
            saveUserData();
        }
    }

    private void saveUserData() {
        String name = inputName.getText() != null ? inputName.getText().toString().trim() : "";
        String phone = inputPhone.getText() != null ? inputPhone.getText().toString().trim() : "";
        String email = inputEmail.getText() != null ? inputEmail.getText().toString().trim() : "";
        String oldEmail = prefs.getString("USER_EMAIL", "");
        String username = prefs.getString("USER_USERNAME", "");

        if (name.isEmpty()) {
            inputName.setError("Name is required");
            return;
        }

        // Real-time Database Activity: Update DB
        if (!username.isEmpty() && getContext() != null) {
            new Thread(() -> {
                com.example.instacare.data.local.AppDatabase.getDatabase(requireContext()).userDao()
                    .updateProfile(username, name, email, phone);
            }).start();
        }

        if (!oldEmail.isEmpty() && !oldEmail.equals(email)) {
            File oldFile = new File(requireContext().getFilesDir(), "profile_avatar_" + oldEmail + ".png");
            if (oldFile.exists()) {
                File newFile = new File(requireContext().getFilesDir(), "profile_avatar_" + email + ".png");
                oldFile.renameTo(newFile);
            }
            String oldAvatarUri = prefs.getString("USER_AVATAR_" + oldEmail, null);
            if (oldAvatarUri != null) {
                prefs.edit().putString("USER_AVATAR_" + email, oldAvatarUri).apply();
            }
        }

        prefs.edit()
             .putString("USER_NAME", name)
             .putString("USER_PHONE", phone)
             .putString("USER_EMAIL", email)
             .apply();

        // Notify parent fragment
        Bundle result = new Bundle();
        result.putBoolean("refresh_data", true);
        getParentFragmentManager().setFragmentResult("profile_update_request", result);

        Toast.makeText(requireContext(), "Profile Updated!", Toast.LENGTH_SHORT).show();
        dismiss();
    }

    private void showVerifyEmailDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme);
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_verify_email, null);
        dialog.setContentView(view);
        
        TextView subtitle = view.findViewById(R.id.textVerifyEmailSubtitle);
        String userEmail = inputEmail.getText() != null ? inputEmail.getText().toString() : "";
        // Highlight email in yellow and put on new line as requested
        String htmlText = "Enter the 4-digit code sent to<br/><font color='#FACC15'>" + userEmail + "</font>";
        subtitle.setText(android.text.Html.fromHtml(htmlText, android.text.Html.FROM_HTML_MODE_LEGACY));
        
        TextInputEditText inputOtp = view.findViewById(R.id.inputOtp);
        MaterialButton btnVerifyOtp = view.findViewById(R.id.btnVerifyOtp);
        MaterialButton btnCancelVerify = view.findViewById(R.id.btnCancelVerify);
        
        // Initial Dialog State
        inputOtp.setEnabled(false);
        inputOtp.setAlpha(0.5f);
        btnVerifyOtp.setText("Send");
        final boolean[] isCodeSent = {false};
        
        btnVerifyOtp.setOnClickListener(v -> {
            if (!isCodeSent[0]) {
                // Send action
                Toast.makeText(requireContext(), "Code sent to email!", Toast.LENGTH_SHORT).show();
                btnVerifyOtp.setText("Verify Code");
                inputOtp.setEnabled(true);
                inputOtp.setAlpha(1.0f);
                inputOtp.requestFocus();
                isCodeSent[0] = true;
            } else {
                // Verify action
                String otp = inputOtp.getText() != null ? inputOtp.getText().toString() : "";
                if (otp.equals("1234")) {
                    prefs.edit().putBoolean("EMAIL_BOUND", true).apply();
                    Toast.makeText(requireContext(), "Email verified successfully!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    saveUserData();
                } else {
                    android.view.animation.Animation shake = android.view.animation.AnimationUtils.loadAnimation(requireContext(), R.anim.shake);
                    inputOtp.startAnimation(shake);
                    inputOtp.postDelayed(() -> inputOtp.setText(""), 300);
                }
            }
        });
        
        btnCancelVerify.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void startCrop(Uri uri) {
        String destinationFileName = "cropped_profile_edit.png";
        Uri destinationUri = Uri.fromFile(new File(requireContext().getCacheDir(), destinationFileName));
        
        UCrop.Options options = new UCrop.Options();
        options.setCircleDimmedLayer(true);
        options.setShowCropGrid(false);
        options.setCompressionFormat(Bitmap.CompressFormat.PNG);
        
        // Theme Colors
        options.setToolbarColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.dashboard_surface));
        options.setStatusBarColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.dashboard_background));
        options.setToolbarWidgetColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_primary));
        options.setActiveControlsWidgetColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary));
        options.setRootViewBackgroundColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.dashboard_background));

        UCrop uCrop = UCrop.of(uri, destinationUri)
                .withAspectRatio(1, 1)
                .withMaxResultSize(1000, 1000)
                .withOptions(options);

        cropImageLauncher.launch(uCrop.getIntent(requireContext()));
    }

    private void saveProfileImage(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            String saveEmail = prefs.getString("USER_EMAIL", "");
            File file = new File(requireContext().getFilesDir(), "profile_avatar_" + saveEmail + ".png");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }
}
