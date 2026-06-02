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

public class EditProfileBottomSheet extends BaseBlurredBottomSheet {

    private ImageView editProfileImage;
    private TextInputEditText inputName, inputPhone, inputEmail, inputAddress;
    private SessionManager sessionManager;
    private boolean isAvatarChanged = false;

    // Image Handling
    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                com.example.instacare.utils.BlurUtils.unblurActivityRoot(getActivity());
                if (uri != null) {
                    startCrop(uri);
                }
            }
    );

    private final ActivityResultLauncher<Intent> cropImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                com.example.instacare.utils.BlurUtils.unblurActivityRoot(getActivity());
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
        sessionManager = SessionManager.getInstance(requireContext());

        // Initialize Views
        editProfileImage = view.findViewById(R.id.editProfileImage);
        inputName = view.findViewById(R.id.inputName);
        inputPhone = view.findViewById(R.id.inputPhone);
        inputEmail = view.findViewById(R.id.inputEmail);
        inputAddress = view.findViewById(R.id.inputAddress);
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
                String currentSavedEmail = sessionManager.getString("USER_EMAIL", "");
                String newEmail = s.toString().trim();
                boolean isEmailBound = sessionManager.getBoolean("EMAIL_BOUND", false);
                View currentView = getView();
                if (currentView != null) {
                    com.google.android.material.textfield.TextInputLayout inputLayoutEmail = currentView.findViewById(R.id.inputLayoutEmail);
                    if (isEmailBound && newEmail.equals(currentSavedEmail)) {
                        inputLayoutEmail.setEndIconVisible(true);
                        inputEmail.setTextColor(android.graphics.Color.parseColor("#4CAF50")); // Verified Green
                    } else {
                        inputLayoutEmail.setEndIconVisible(false);
                        // Use default theme-aware color for input text
                        int defaultTextColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_primary);
                        inputEmail.setTextColor(defaultTextColor);
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
        inputAddress.addTextChangedListener(genericWatcher);
        
        // Custom TextWatcher for Phone UI animation (Progress Bar)
        com.google.android.material.progressindicator.LinearProgressIndicator phoneProgress = view.findViewById(R.id.phoneProgress);
        inputPhone.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(android.text.Editable s) {
                checkIfModified(); // Still trigger save button logic
                int len = s.toString().trim().length();
                if (len > 0 && len < 11) {
                    phoneProgress.setVisibility(View.VISIBLE);
                    phoneProgress.setProgressCompat(len, true);
                } else {
                    phoneProgress.setVisibility(View.GONE);
                    phoneProgress.setProgressCompat(0, false);
                }
            }
        });

        // Listeners
        editProfileImage.setOnClickListener(v -> {
            com.example.instacare.utils.BlurUtils.blurActivityRoot(getActivity(), 15f);
            pickImageLauncher.launch("image/*");
        });
        saveButton.setOnClickListener(v -> handleSave());
        
        // Initial state check
        checkIfModified();
    }

    private void checkIfModified() {
        if (getView() == null) return;
        
        String currentName = inputName.getText() != null ? inputName.getText().toString().trim() : "";
        String currentPhone = inputPhone.getText() != null ? inputPhone.getText().toString().trim() : "";
        String currentEmail = inputEmail.getText() != null ? inputEmail.getText().toString().trim() : "";
        String currentAddress = inputAddress.getText() != null ? inputAddress.getText().toString().trim() : "";

        String savedName = sessionManager.getString("USER_NAME", "");
        String savedPhone = sessionManager.getString("USER_PHONE", "");
        String savedEmail = sessionManager.getString("USER_EMAIL", "");
        String savedAddress = sessionManager.getString("USER_ADDRESS", "");

        boolean isModified = isAvatarChanged || 
                            !currentName.equals(savedName) || 
                            (!currentPhone.isEmpty() && !currentPhone.equals(savedPhone)) || 
                            !currentEmail.equals(savedEmail) ||
                            !currentAddress.equals(savedAddress);

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
        if (getContext() != null) {
            int userId = sessionManager.getCurrentUserUid();
            new Thread(() -> {
                com.example.instacare.data.local.User user = 
                    com.example.instacare.data.local.AppDatabase.getDatabase(requireContext()).userDao().getUserById(userId);
                if (user != null && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (inputName != null) inputName.setText(user.fullName != null ? user.fullName : "");
                        if (inputEmail != null) inputEmail.setText(user.email != null ? user.email : "");
                        if (inputPhone != null) inputPhone.setText(user.phone != null ? user.phone : "");
                        if (inputAddress != null) inputAddress.setText(sessionManager.getString("USER_ADDRESS", ""));
                        
                        // SYNC: Update verification status from DB to Session immediately
                        sessionManager.putBoolean("EMAIL_BOUND", user.isVerified);
                        updateEmailVerifiedUI(user.isVerified);
                        
                        // Sync session
                        sessionManager.putString("USER_NAME", user.fullName);
                        sessionManager.putString("USER_EMAIL", user.email);
                        sessionManager.putString("USER_PHONE", user.phone);
                    });
                } else if (getActivity() != null) {
                    // Fallback to session
                    getActivity().runOnUiThread(() -> {
                        String email = sessionManager.getString("USER_EMAIL", "");
                        if (inputName != null) inputName.setText(sessionManager.getString("USER_NAME", ""));
                        if (inputEmail != null) inputEmail.setText(email);
                        if (inputPhone != null) inputPhone.setText(sessionManager.getString("USER_PHONE", ""));
                        if (inputAddress != null) inputAddress.setText(sessionManager.getString("USER_ADDRESS", ""));
                    });
                }
            }).start();
        }

        boolean isEmailBound = sessionManager.getBoolean("EMAIL_BOUND", false);
        updateEmailVerifiedUI(isEmailBound);
    }

    private void updateEmailVerifiedUI(boolean isVerified) {
        View view = getView();
        if (view != null && isAdded()) {
            com.google.android.material.textfield.TextInputLayout inputLayoutEmail = view.findViewById(R.id.inputLayoutEmail);
            if (inputLayoutEmail != null) {
                inputLayoutEmail.setEndIconVisible(isVerified);
                if (isVerified) {
                    inputEmail.setTextColor(android.graphics.Color.parseColor("#4CAF50")); // success_green
                } else {
                    int defaultTextColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.text_primary);
                    inputEmail.setTextColor(defaultTextColor);
                }
            }
        }
    }
    
    private void loadProfileImageFromFile() {
        int avatarUid = sessionManager.getCurrentUserUid();
        File file = new File(requireContext().getFilesDir(), "profile_avatar_" + avatarUid + ".png");
        if (file.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            editProfileImage.setImageBitmap(bitmap);
        } else {
            // Fallback: check avatar URI from setup (isolated)
            String avatarUri = sessionManager.getString("USER_AVATAR", null);
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
        String currentSavedEmail = sessionManager.getString("USER_EMAIL", "");
        String newEmail = inputEmail.getText() != null ? inputEmail.getText().toString().trim() : "";

        // Only trigger verification if the email address itself was changed
        if (!newEmail.equals(currentSavedEmail)) {
            showVerifyEmailDialog();
        } else {
            saveUserData();
        }
    }

    private void saveUserData() {
        String name = inputName.getText() != null ? inputName.getText().toString().trim() : "";
        String phone = inputPhone.getText() != null ? inputPhone.getText().toString().trim() : "";
        String email = inputEmail.getText() != null ? inputEmail.getText().toString().trim() : "";
        String address = inputAddress.getText() != null ? inputAddress.getText().toString().trim() : "";
        String oldEmail = sessionManager.getString("USER_EMAIL", "");
        String username = sessionManager.getString("USER_USERNAME", "");

        if (name.isEmpty()) {
            inputName.setError("Name is required");
            return;
        }

        // Real-time Database Activity: Update DB (Hardened via UID)
        int userId = sessionManager.getCurrentUserUid();
        if (userId != -1 && getContext() != null) {
            new Thread(() -> {
                com.example.instacare.data.local.AppDatabase.getDatabase(requireContext()).userDao()
                    .updateProfileById(userId, name, email, phone, address);
            }).start();
        }

        if (!oldEmail.isEmpty() && !oldEmail.equals(email)) {
            // Identity is now UID-based, so the avatar filename "profile_avatar_UID.png" 
            // remains valid even if the email changes. No renaming logic needed.
            String oldAvatarUri = sessionManager.getString("USER_AVATAR", null);
            if (oldAvatarUri != null) {
                sessionManager.putString("USER_AVATAR", oldAvatarUri);
            }
        }

        sessionManager.putString("USER_NAME", name);
        sessionManager.putString("USER_PHONE", phone);
        sessionManager.putString("USER_EMAIL", email);
        sessionManager.putString("USER_ADDRESS", address);

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
        com.example.instacare.utils.BlurUtils.applyBlur(dialog);
        
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
                    sessionManager.putBoolean("EMAIL_BOUND", true);
                    updateEmailVerifiedUI(true);
                    Toast.makeText(requireContext(), "Email verified successfully!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    saveUserData();
                } else {
                    android.view.animation.Animation shake = android.view.animation.AnimationUtils.loadAnimation(requireContext(), R.anim.shake);
                    com.google.android.material.textfield.TextInputLayout layoutOtp = view.findViewById(R.id.layoutOtp);
                    layoutOtp.setError("Invalid Code");
                    layoutOtp.startAnimation(shake);
                    layoutOtp.postDelayed(() -> {
                        layoutOtp.setError(null);
                        layoutOtp.setErrorEnabled(false);
                        inputOtp.setText("");
                    }, 1000);
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
        
        // Theme Colors - ADAPTIVE Fix
        boolean isDarkMode = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        
        int toolbarColor = isDarkMode ? Color.parseColor("#0F172A") : Color.WHITE; // Navy for dark mode
        int textColor = isDarkMode ? Color.WHITE : Color.BLACK;
        int bgColor = isDarkMode ? Color.BLACK : Color.parseColor("#F9FAFB");

        options.setToolbarColor(toolbarColor);
        options.setStatusBarColor(toolbarColor); // Consistency
        options.setToolbarWidgetColor(textColor);
        options.setActiveControlsWidgetColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.primary));
        options.setRootViewBackgroundColor(bgColor);
        
        // --- UNIVERSAL FIX: Use Window Insets if available ---
        options.setToolbarTitle("Edit Profile Photo");

        UCrop uCrop = UCrop.of(uri, destinationUri)
                .withAspectRatio(1, 1)
                .withMaxResultSize(1000, 1000)
                .withOptions(options);

        com.example.instacare.utils.BlurUtils.blurActivityRoot(getActivity(), 15f);
        cropImageLauncher.launch(uCrop.getIntent(requireContext()));
    }

    private void saveProfileImage(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            int saveUid = sessionManager.getCurrentUserUid();
            File file = new File(requireContext().getFilesDir(), "profile_avatar_" + saveUid + ".png");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }
}
