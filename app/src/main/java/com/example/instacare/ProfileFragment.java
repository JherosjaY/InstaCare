package com.example.instacare;

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
import android.widget.ImageView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import com.yalantis.ucrop.UCrop; // Add import
import java.io.File;

public class ProfileFragment extends Fragment {

    private SwitchMaterial darkModeSwitch;
    private MaterialButton logoutButton;
    private TextView profileNameText;
    private TextView profileEmailText;
    private ImageView profileImage;
    private View editProfileImageBtn;
    
    // No local image picker needed as it's handled in BottomSheet
    // Removed unused ActivityResultLaunchers

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        darkModeSwitch = view.findViewById(R.id.darkModeSwitch);
        logoutButton = view.findViewById(R.id.logoutButton);
        profileNameText = view.findViewById(R.id.profileNameText);
        profileEmailText = view.findViewById(R.id.profileEmailText);
        profileImage = view.findViewById(R.id.profileImage);
        editProfileImageBtn = view.findViewById(R.id.editProfileImageBtn);
        MaterialButton deleteAccountButton = view.findViewById(R.id.deleteAccountButton);
        ImageView btnEditMedicalInfo = view.findViewById(R.id.btnEditMedicalInfo);

        // Edge-to-Edge inset handling for the header container
        View headerContainer = view.findViewById(R.id.headerContainer);
        if (headerContainer != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(headerContainer, (v, windowInsets) -> {
                androidx.core.graphics.Insets insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
                int padding20 = (int)(20 * getResources().getDisplayMetrics().density);
                v.setPadding(padding20, insets.top + padding20, padding20, padding20);
                return windowInsets;
            });
        }

        // Launcher for MedicalInfoActivity to refresh data on return
        ActivityResultLauncher<Intent> medicalInfoLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                    loadUserData();
                }
            }
        );

        if (btnEditMedicalInfo != null) {
            btnEditMedicalInfo.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), MedicalInfoActivity.class);
                medicalInfoLauncher.launch(intent);
            });
        }

        // Setup Edit Button for profile image
        editProfileImageBtn.setOnClickListener(v -> {
             EditProfileBottomSheet bottomSheet = new EditProfileBottomSheet();
             bottomSheet.show(getParentFragmentManager(), "EditProfileBottomSheet");
        });

        setupDarkModeSwitch();
        loadUserData();
        loadProfileImage();
        
        // Listen for updates from Bottom Sheet
        getParentFragmentManager().setFragmentResultListener("profile_update_request", this, (requestKey, result) -> {
            if (result.getBoolean("refresh_data")) {
                loadUserData();
                loadProfileImage();
            }
        });

        logoutButton.setOnClickListener(v -> {
            if (getActivity() != null) {
                android.content.SharedPreferences.Editor editor = requireContext().getSharedPreferences("InstaCarePrefs", Context.MODE_PRIVATE).edit();
                editor.remove("USER_NAME").remove("USER_EMAIL").remove("USER_PHONE").apply();
                Intent logoutIntent = new Intent(getActivity(), LoginActivity.class);
                logoutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(logoutIntent);
                getActivity().finish();
            }
        });

        // Delete Account
        deleteAccountButton.setOnClickListener(v -> showDeleteAccountDialog());
    }

    private void showDeleteAccountDialog() {
        if (getContext() == null || getParentFragmentManager() == null) return;
        
        DeleteAccountBottomSheet bottomSheet = new DeleteAccountBottomSheet();
        bottomSheet.show(getParentFragmentManager(), "DeleteAccountBottomSheet");
    }

    // Removed startCrop and saveAndSetProfileImage as they are now in BottomSheet

    private void loadUserData() {
        if (getContext() == null) return;
        
        SharedPreferences prefs = requireContext().getSharedPreferences("InstaCarePrefs", Context.MODE_PRIVATE);
        String username = prefs.getString("USER_USERNAME", "");
        
        // Initial load from prefs for immediate display
        profileNameText.setText(prefs.getString("USER_NAME", ""));
        profileEmailText.setText(prefs.getString("USER_EMAIL", ""));
        TextView phoneText = getView().findViewById(R.id.profilePhoneText);
        if (phoneText != null) {
            phoneText.setText(prefs.getString("USER_PHONE", ""));
        }
        
        // Real-time Database Activity: Fetch from DB
        if (!username.isEmpty()) {
            new Thread(() -> {
                com.example.instacare.data.local.User user = 
                    com.example.instacare.data.local.AppDatabase.getDatabase(requireContext()).userDao().getUserByUsername(username);
                if (user != null && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        profileNameText.setText(user.fullName != null ? user.fullName : "");
                        profileEmailText.setText(user.email != null ? user.email : "");
                        if (phoneText != null) {
                            phoneText.setText(user.phone != null ? user.phone : "");
                        }
                        
                        // Sync prefs
                        prefs.edit()
                            .putString("USER_NAME", user.fullName)
                            .putString("USER_EMAIL", user.email)
                            .putString("USER_PHONE", user.phone)
                            .apply();
                    });
                }
            }).start();
        }
        
        // Update Medical ID if present
        View fragView = getView();
        if (fragView != null) {
            String email = prefs.getString("USER_EMAIL", "");
            
            TextView bloodText = fragView.findViewById(R.id.textBloodType);
            TextView sexText = fragView.findViewById(R.id.textSex);
            TextView condText = fragView.findViewById(R.id.textMedicalConditions);
            TextView medsText = fragView.findViewById(R.id.textMedications);
            TextView allergiesText = fragView.findViewById(R.id.textAllergies);
            TextView weightText = fragView.findViewById(R.id.textWeight);
            TextView heightText = fragView.findViewById(R.id.textHeight);
            
            if (bloodText != null) bloodText.setText(prefs.getString("USER_BLOOD_TYPE_" + email, prefs.getString("USER_BLOOD_TYPE", "--")));
            if (sexText != null) sexText.setText(prefs.getString("MED_SEX_" + email, "--"));
            if (condText != null) {
                String val = prefs.getString("MED_COND_" + email, "");
                condText.setText(val.isEmpty() ? "None recorded" : val);
            }
            if (medsText != null) {
                String val = prefs.getString("MED_MEDS_" + email, "");
                medsText.setText(val.isEmpty() ? "None recorded" : val);
            }
            if (allergiesText != null) {
                String val = prefs.getString("USER_ALLERGIES_" + email, prefs.getString("USER_ALLERGIES", ""));
                allergiesText.setText(val.isEmpty() ? "None recorded" : val);
            }
            if (weightText != null) {
                String val = prefs.getString("MED_WEIGHT_" + email, "");
                weightText.setText(val.isEmpty() ? "-- kg" : val + " kg");
            }
            if (heightText != null) {
                String val = prefs.getString("MED_HEIGHT_" + email, "");
                heightText.setText(val.isEmpty() ? "-- cm" : val + " cm");
            }
        }
    }
    
    private Bitmap cropToSquare(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        if (width == height) return bitmap; // Optimization
        int newWidth = (height > width) ? width : height;
        int newHeight = (height > width) ? height - (height - width) : height;
        
        int cropW = (width - height) / 2;
        cropW = (cropW < 0) ? 0 : cropW;
        int cropH = (height - width) / 2;
        cropH = (cropH < 0) ? 0 : cropH;
        return Bitmap.createBitmap(bitmap, cropW, cropH, newWidth, newWidth);
    }

    private void loadProfileImage() {
        try {
            if (getView() != null) {
                profileImage = getView().findViewById(R.id.profileImage);
            }
            if (profileImage == null) return;

            String avatarEmail = requireContext().getSharedPreferences("InstaCarePrefs", android.content.Context.MODE_PRIVATE).getString("USER_EMAIL", "");
            java.io.File file = new java.io.File(requireContext().getFilesDir(), "profile_avatar_" + avatarEmail + ".png");
            if (file.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                profileImage.setImageBitmap(bitmap);
                profileImage.invalidate();
                
                // Also Update Bottom Nav
                 if (getActivity() != null) {
                     com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottomNav);
                     if (bottomNav != null) {
                          Bitmap squareBitmap = cropToSquare(bitmap);
                          
                          androidx.core.graphics.drawable.RoundedBitmapDrawable circularBitmapDrawable =
                              androidx.core.graphics.drawable.RoundedBitmapDrawableFactory.create(getResources(), squareBitmap);
                          circularBitmapDrawable.setCircular(true);
                          bottomNav.getMenu().findItem(R.id.nav_profile).setIcon(circularBitmapDrawable);
                     }
                }
            } else {
                // Fallback: check avatar URI from setup (per-user)
                android.content.SharedPreferences avatarPrefs = requireContext().getSharedPreferences("InstaCarePrefs", Context.MODE_PRIVATE);
                String email = avatarPrefs.getString("USER_EMAIL", "");
                String avatarUri = avatarPrefs.getString("USER_AVATAR_" + email, null);
                if (avatarUri != null) {
                    profileImage.setImageURI(Uri.parse(avatarUri));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupDarkModeSwitch() {
        if (getContext() == null) return;
        
        SharedPreferences prefs = requireContext().getSharedPreferences("ThemePrefs", Context.MODE_PRIVATE);
        int currentMode = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_NO);
        boolean isDarkMode = currentMode == AppCompatDelegate.MODE_NIGHT_YES;

        darkModeSwitch.setOnCheckedChangeListener(null);
        darkModeSwitch.setChecked(isDarkMode);

        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int newMode = isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
            
            prefs.edit().putInt("theme_mode", newMode).apply();
            AppCompatDelegate.setDefaultNightMode(newMode);
            
            // Recreate activity to apply theme changes immediately
            if (getActivity() != null) {
                getActivity().recreate();
            }
        });
    }
}
