package com.example.instacare;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import android.view.View;
import android.view.LayoutInflater;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class EditProfileActivity extends AppCompatActivity {

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
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri resultUri = UCrop.getOutput(result.getData());
                    if (resultUri != null) {
                        saveProfileImage(resultUri);
                        editProfileImage.setImageURI(resultUri);
                        isAvatarChanged = true;
                        checkIfModified();
                    }
                } else if (result.getResultCode() == UCrop.RESULT_ERROR) {
                    Throwable cropError = UCrop.getError(result.getData());
                    Toast.makeText(this, "Crop error: " + cropError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);
        
        prefs = getSharedPreferences("InstaCarePrefs", MODE_PRIVATE);

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        // Initialize Views
        editProfileImage = findViewById(R.id.editProfileImage);
        inputName = findViewById(R.id.inputName);
        inputPhone = findViewById(R.id.inputPhone);
        inputEmail = findViewById(R.id.inputEmail);
        MaterialButton saveButton = findViewById(R.id.saveButton);

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
                com.google.android.material.textfield.TextInputLayout inputLayoutEmail = findViewById(R.id.inputLayoutEmail);
                
                if (isEmailBound && newEmail.equals(currentSavedEmail)) {
                    inputLayoutEmail.setEndIconVisible(true);
                    inputEmail.setTextColor(Color.parseColor("#4CAF50"));
                } else {
                    inputLayoutEmail.setEndIconVisible(false);
                    inputEmail.setTextColor(androidx.core.content.ContextCompat.getColor(EditProfileActivity.this, R.color.text_primary));
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
        findViewById(R.id.editProfileImage).setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        saveButton.setOnClickListener(v -> handleSave());
        
        // Initial state check
        checkIfModified();
    }

    private void checkIfModified() {
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

        MaterialButton saveButton = findViewById(R.id.saveButton);
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

    private void loadUserData() {
        String username = prefs.getString("USER_USERNAME", "");
        if (!username.isEmpty()) {
            new Thread(() -> {
                com.example.instacare.data.local.User user = 
                    com.example.instacare.data.local.AppDatabase.getDatabase(this).userDao().getUserByUsername(username);
                if (user != null) {
                    runOnUiThread(() -> {
                        inputName.setText(user.fullName != null ? user.fullName : "");
                        inputEmail.setText(user.email != null ? user.email : "");
                        inputPhone.setText(user.phone != null ? user.phone : "");
                        
                        // Sync prefs if they're out of date (e.g. after theme switch if DB is truth)
                        prefs.edit()
                            .putString("USER_NAME", user.fullName)
                            .putString("USER_EMAIL", user.email)
                            .putString("USER_PHONE", user.phone)
                            .apply();
                    });
                } else {
                    // Fallback to prefs
                    runOnUiThread(() -> {
                        inputName.setText(prefs.getString("USER_NAME", ""));
                        inputEmail.setText(prefs.getString("USER_EMAIL", ""));
                        inputPhone.setText(prefs.getString("USER_PHONE", ""));
                    });
                }
            }).start();
        }

        boolean isEmailBound = prefs.getBoolean("EMAIL_BOUND", false);
        com.google.android.material.textfield.TextInputLayout inputLayoutEmail = findViewById(R.id.inputLayoutEmail);
        if (isEmailBound) {
            inputLayoutEmail.setEndIconVisible(true);
            inputEmail.setTextColor(Color.parseColor("#4CAF50")); // success_green
        } else {
            inputLayoutEmail.setEndIconVisible(false);
            inputEmail.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_primary));
        }
    }
    
    private void loadProfileImageFromFile() {
        String avatarEmail = prefs.getString("USER_EMAIL", "");
        File file = new File(getFilesDir(), "profile_avatar_" + avatarEmail + ".png");
        if (file.exists()) {
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
            editProfileImage.setImageBitmap(bitmap);
        }
    }

    private void handleSave() {
        boolean isEmailBound = prefs.getBoolean("EMAIL_BOUND", false);
        String currentSavedEmail = prefs.getString("USER_EMAIL", "");
        String newEmail = inputEmail.getText() != null ? inputEmail.getText().toString().trim() : "";

        if (!isEmailBound || !newEmail.equals(currentSavedEmail)) {
            showVerifyEmailDialog();
        } else {
            saveUserId();
        }
    }

    private void saveUserId() {
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
        if (!username.isEmpty()) {
            new Thread(() -> {
                com.example.instacare.data.local.AppDatabase.getDatabase(this).userDao()
                    .updateProfile(username, name, email, phone);
            }).start();
        }

        if (!oldEmail.isEmpty() && !oldEmail.equals(email)) {
            File oldFile = new File(getFilesDir(), "profile_avatar_" + oldEmail + ".png");
            if (oldFile.exists()) {
                File newFile = new File(getFilesDir(), "profile_avatar_" + email + ".png");
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

        Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void showVerifyEmailDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_verify_email, null);
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
                Toast.makeText(this, "Code sent to email!", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(this, "Email verified successfully!", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    saveUserId();
                } else {
                    android.view.animation.Animation shake = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.shake);
                    inputOtp.startAnimation(shake);
                    inputOtp.postDelayed(() -> inputOtp.setText(""), 300);
                }
            }
        });
        
        btnCancelVerify.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void startCrop(Uri uri) {
        String destinationFileName = "cropped_profile.png";
        Uri destinationUri = Uri.fromFile(new File(getCacheDir(), destinationFileName));
        
        UCrop.Options options = new UCrop.Options();
        options.setCircleDimmedLayer(true);
        options.setShowCropGrid(false);
        options.setCompressionFormat(Bitmap.CompressFormat.PNG);
        
        // Theme Colors
        options.setToolbarColor(androidx.core.content.ContextCompat.getColor(this, R.color.dashboard_surface));
        options.setStatusBarColor(androidx.core.content.ContextCompat.getColor(this, R.color.dashboard_background));
        options.setToolbarWidgetColor(androidx.core.content.ContextCompat.getColor(this, R.color.text_primary));
        options.setActiveControlsWidgetColor(androidx.core.content.ContextCompat.getColor(this, R.color.primary));
        options.setRootViewBackgroundColor(androidx.core.content.ContextCompat.getColor(this, R.color.dashboard_background));

        UCrop uCrop = UCrop.of(uri, destinationUri)
                .withAspectRatio(1, 1)
                .withMaxResultSize(1000, 1000)
                .withOptions(options);

        cropImageLauncher.launch(uCrop.getIntent(this));
    }

    private void saveProfileImage(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            String saveEmail = prefs.getString("USER_EMAIL", "");
            File file = new File(getFilesDir(), "profile_avatar_" + saveEmail + ".png");
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }
}
