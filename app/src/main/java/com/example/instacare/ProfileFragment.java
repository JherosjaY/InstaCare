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
import android.widget.Toast;
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
    private SwitchMaterial locationAccessSwitch;
    private SwitchMaterial notificationSwitch;
    private MaterialButton logoutButton;
    private TextView profileNameText;
    private TextView profileEmailText;
    private TextView profileAddressText;
    private ImageView profileImage;
    private View editProfileImageBtn;
    private androidx.core.widget.NestedScrollView profileScrollView;
    
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
        locationAccessSwitch = view.findViewById(R.id.locationAccessSwitch);
        notificationSwitch = view.findViewById(R.id.notificationSwitch);
        logoutButton = view.findViewById(R.id.logoutButton);
        profileNameText = view.findViewById(R.id.profileNameText);
        profileEmailText = view.findViewById(R.id.profileEmailText);
        profileAddressText = view.findViewById(R.id.profileAddressText);
        profileImage = view.findViewById(R.id.profileImage);
        editProfileImageBtn = view.findViewById(R.id.editProfileImageBtn);
        profileScrollView = view.findViewById(R.id.profileScrollView);
        MaterialButton deleteAccountButton = view.findViewById(R.id.deleteAccountButton);
        ImageView btnEditMedicalInfo = view.findViewById(R.id.btnEditMedicalInfo);

        // RESTORE SCROLL POSITION: After theme switch (Activity Recreate) using SessionManager
        SessionManager sessionManager = SessionManager.getInstance(requireContext());
        int savedScrollY = sessionManager.getInt("profile_scroll_y", 0);
        if (savedScrollY > 0 && profileScrollView != null) {
            profileScrollView.post(() -> {
                profileScrollView.scrollTo(0, savedScrollY);
                // Clear after restore so it doesn't scroll on fresh launches
                sessionManager.remove("profile_scroll_y");
            });
        }

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

        // Dynamic Header Accent
        TextView tvTitle = view.findViewById(R.id.tvTitleProfile);
        if (tvTitle != null) {
            String fullText = "My Profile";
            android.text.SpannableStringBuilder spannable = new android.text.SpannableStringBuilder(fullText);
            int start = fullText.indexOf("Profile");
            if (start != -1) {
                int color = androidx.core.content.ContextCompat.getColor(view.getContext(), R.color.emergency_red);
                spannable.setSpan(
                    new android.text.style.ForegroundColorSpan(color),
                    start, 
                    start + "Profile".length(), 
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
            tvTitle.setText(spannable);
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
        setupLocationAccessSwitch();
        setupNotificationSwitch();
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
            // SECURE WIPE: SessionManager handles all partitioning and theme reset to Light Mode
            SessionManager.getInstance(requireContext()).endSession();
            Intent logoutIntent = new Intent(getActivity(), LoginActivity.class);
            logoutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(logoutIntent);
            if (getActivity() != null) getActivity().finish();
        });

        // Delete Account
        deleteAccountButton.setOnClickListener(v -> showDeleteAccountDialog());
    }

    private void showDeleteAccountDialog() {
        if (getContext() == null || getParentFragmentManager() == null) return;
        
        DeleteAccountBottomSheet bottomSheet = new DeleteAccountBottomSheet();
        bottomSheet.show(getParentFragmentManager(), "DeleteAccountBottomSheet");
    }

    private void loadUserData() {
        if (getContext() == null) return;
        
        SessionManager sessionManager = SessionManager.getInstance(requireContext());
        String username = sessionManager.getString("USER_USERNAME", "");
        
        // Initial load from isolated sessionManager for immediate display
        profileNameText.setText(sessionManager.getString("USER_NAME", ""));
        profileEmailText.setText(sessionManager.getCurrentUserEmail());
        TextView phoneText = getView().findViewById(R.id.profilePhoneText);
        if (phoneText != null) {
            phoneText.setText(sessionManager.getString("USER_PHONE", ""));
        }
        if (profileAddressText != null) {
            String savedAddr = sessionManager.getString("USER_ADDRESS", "");
            profileAddressText.setText(savedAddr.isEmpty() ? "No address set" : savedAddr);
            profileAddressText.setVisibility(View.VISIBLE);
        }
        
        // Real-time Database Activity: Fetch from DB for backup sync
        if (!username.isEmpty() && getContext() != null) {
            int userId = sessionManager.getCurrentUserUid();
            new Thread(() -> {
                com.example.instacare.data.local.User user = 
                    com.example.instacare.data.local.AppDatabase.getDatabase(requireContext()).userDao().getUserById(userId);
                if (user != null && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (profileNameText != null) profileNameText.setText(user.fullName != null ? user.fullName : "");
                        if (profileEmailText != null) profileEmailText.setText(user.email != null ? user.email : "");
                        if (phoneText != null) phoneText.setText(user.phone != null ? user.phone : "");
                        if (profileAddressText != null) profileAddressText.setText(sessionManager.getString("USER_ADDRESS", ""));

                        // Isolated Sync using SessionManager (Keep current active session data as master)
                        if (user.fullName != null) sessionManager.putString("USER_NAME", user.fullName);
                        if (user.email != null) sessionManager.putString("USER_EMAIL", user.email);
                        if (user.phone != null) sessionManager.putString("USER_PHONE", user.phone);
                        
                        // Address Sync: If Session already has data, keep it
                        String sessionAddr = sessionManager.getString("USER_ADDRESS", "");
                        if (user.address != null && sessionAddr.isEmpty()) {
                            sessionManager.putString("USER_ADDRESS", user.address);
                            if (profileAddressText != null) {
                                profileAddressText.setText(user.address);
                            }
                        }
                    });
                }
            }).start();
        }
        
        // Update Medical ID if present
        View fragView = getView();
        if (fragView != null) {
            
            TextView bloodText = fragView.findViewById(R.id.textBloodType);
            TextView sexText = fragView.findViewById(R.id.textSex);
            TextView condText = fragView.findViewById(R.id.textMedicalConditions);
            TextView medsText = fragView.findViewById(R.id.textMedications);
            TextView allergiesText = fragView.findViewById(R.id.textAllergies);
            TextView weightText = fragView.findViewById(R.id.textWeight);
            TextView heightText = fragView.findViewById(R.id.textHeight);
            
            if (bloodText != null) bloodText.setText(sessionManager.getString("USER_BLOOD_TYPE", "--"));
            if (sexText != null) sexText.setText(sessionManager.getString("MED_SEX", "--"));
            if (condText != null) {
                String val = sessionManager.getString("MED_COND", "");
                condText.setText(val.isEmpty() ? "None recorded" : val);
            }
            if (medsText != null) {
                String val = sessionManager.getString("MED_MEDS", "");
                medsText.setText(val.isEmpty() ? "None recorded" : val);
            }
            if (allergiesText != null) {
                String val = sessionManager.getString("USER_ALLERGIES", "");
                allergiesText.setText(val.isEmpty() ? "None recorded" : val);
            }
            if (weightText != null) {
                String val = sessionManager.getString("MED_WEIGHT", "");
                weightText.setText(val.isEmpty() ? "-- kg" : val + " kg");
            }
            if (heightText != null) {
                String val = sessionManager.getString("MED_HEIGHT", "");
                heightText.setText(val.isEmpty() ? "-- cm" : val + " cm");
            }
        }
    }
    
    private void loadProfileImage() {
        try {
            if (profileImage == null) {
                if (getView() != null) {
                    profileImage = getView().findViewById(R.id.profileImage);
                }
            }
            if (profileImage == null) return;

            SessionManager sessionManager = SessionManager.getInstance(requireContext());
            int avatarUid = sessionManager.getCurrentUserUid();
            java.io.File file = new java.io.File(requireContext().getFilesDir(), "profile_avatar_" + avatarUid + ".png");
            
            if (file.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                profileImage.setImageBitmap(bitmap);
                profileImage.invalidate();
                
                // Also Update Bottom Nav icon if on Dashboard
                if (getActivity() instanceof UserDashboardActivity) {
                    ((UserDashboardActivity) getActivity()).loadProfileImage();
                }
            } else {
                // Fallback: check avatar URI from setup (isolated via SessionManager)
                String avatarUri = sessionManager.getString("USER_AVATAR", null);
                if (avatarUri != null) {
                    profileImage.setImageURI(Uri.parse(avatarUri));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupNotificationSwitch() {
        if (notificationSwitch == null || getContext() == null) return;
        
        SessionManager sessionManager = SessionManager.getInstance(requireContext());
        
        // SYNC: Check Actual System Permission status
        boolean systemEnabled = androidx.core.app.NotificationManagerCompat.from(requireContext()).areNotificationsEnabled();
        boolean prefsEnabled = sessionManager.getBoolean("NOTIFICATIONS_ENABLED", true);
        
        // The switch should be OFF if system disabled it, even if prefs says otherwise
        notificationSwitch.setOnCheckedChangeListener(null);
        notificationSwitch.setChecked(systemEnabled && prefsEnabled);
        
        notificationSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            boolean currentSystemEnabled = androidx.core.app.NotificationManagerCompat.from(requireContext()).areNotificationsEnabled();
            
            if (isChecked && !currentSystemEnabled) {
                // If turning ON but system blocked it, guide user to Settings
                notificationSwitch.setChecked(false);
                Toast.makeText(getContext(), "Please enable notifications in System Settings", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());
                startActivity(intent);
                return;
            }

            sessionManager.putBoolean("NOTIFICATIONS_ENABLED", isChecked);
            
            // Immediate feedback
            String msg = isChecked ? "Notifications Enabled" : "Notifications Disabled";
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            
            // Trigger a count refresh on re-selection (via FragmentResult)
            Bundle result = new Bundle();
            result.putBoolean("refresh_notification_count", true);
            getParentFragmentManager().setFragmentResult("notification_sync", result);
        });
    }

    private void setupDarkModeSwitch() {
        if (getContext() == null || darkModeSwitch == null) return;
        SessionManager sessionManager = SessionManager.getInstance(requireContext());
        
        int currentMode = sessionManager.getTheme();
        boolean isDarkMode = currentMode == AppCompatDelegate.MODE_NIGHT_YES;

        darkModeSwitch.setOnCheckedChangeListener(null);
        darkModeSwitch.setChecked(isDarkMode);

        darkModeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            int newMode = isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;
            
            UserDashboardActivity.isToDarkMode = isChecked;
            
            // SAVE STATE FOR PREMIUM REVEAL: Capture Switch Location & Screenshot
            if (getActivity() != null) {
                // 1. Get Switch Center Coordinates (Absolute Screen Position)
                int[] location = new int[2];
                darkModeSwitch.getLocationOnScreen(location);
                UserDashboardActivity.revealX = location[0] + (darkModeSwitch.getWidth() / 2);
                UserDashboardActivity.revealY = location[1] + (darkModeSwitch.getHeight() / 2);
                
                // 2. Capture Current Screen (Old Theme) before system-wide change
                View rootView = getActivity().getWindow().getDecorView();
                Bitmap bitmap = Bitmap.createBitmap(rootView.getWidth(), rootView.getHeight(), Bitmap.Config.ARGB_8888);
                android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
                
                // Draw the decor view (body)
                rootView.draw(canvas);
                
                // FIX "CLOSED EYE" GLITCH: DecorView native draw() oftens misses the hardware Status Bar and Nav Bar!
                // We must manually paint the current system bar colors onto the screenshot so the Top/Bottom bars don't turn black before the wave.
                android.graphics.Paint paint = new android.graphics.Paint();
                android.view.Window window = getActivity().getWindow();
                
                // 1. Manually paint Status Bar on the bitmap
                int statusBarHeight = 0;
                int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
                if (resId > 0) statusBarHeight = getResources().getDimensionPixelSize(resId);
                paint.setColor(window.getStatusBarColor());
                canvas.drawRect(0, 0, rootView.getWidth(), statusBarHeight, paint);
                
                UserDashboardActivity.themeTransitionBitmap = bitmap;
            }

            // SAVE SCROLL POSITION: Capture current Y before recreation (UID-isolated)
            if (profileScrollView != null) {
                sessionManager.putInt("profile_scroll_y", profileScrollView.getScrollY());
            }
            
            // SAVE CURRENT TAB: Preserve which tab we're on so recreate doesn't jump to Home
            if (getActivity() instanceof UserDashboardActivity) {
                sessionManager.putInt("CURRENT_TAB_INDEX", 3); // Profile tab
            }

            // Remove window animations temporarily before theme change
            if (getActivity() != null) {
                getActivity().overridePendingTransition(0, 0);
            }
            
            sessionManager.setTheme(newMode);
            // Do NOT call recreate() manually because setDefaultNightMode() (inside setTheme) already automatically recreates the Activity.

        });
    }

    private void setupLocationAccessSwitch() {
        if (getContext() == null || locationAccessSwitch == null) return;
        checkPermissionsAndGPS();
    }

    private void checkPermissionsAndGPS() {
        if (locationAccessSwitch == null || getContext() == null) return;
        
        final boolean hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) 
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
        
        android.location.LocationManager lm = (android.location.LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        final boolean gpsEnabled = (lm != null) && lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
        
        locationAccessSwitch.setOnCheckedChangeListener(null);
        // Switch is ON if both permission AND GPS are ready
        locationAccessSwitch.setChecked(hasPermission && gpsEnabled);
        
        // Re-setup the listener
        locationAccessSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!hasPermission) {
                    Intent intent = new Intent(requireContext(), PermissionsActivity.class);
                    intent.putExtra(PermissionsActivity.EXTRA_FROM_SETTINGS, true);
                    startActivity(intent);
                } else if (!gpsEnabled) {
                    startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                }
            } else {
                Toast.makeText(getContext(), "To revoke access, please use System Settings", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserData();
        loadProfileImage();
        checkPermissionsAndGPS();
    }
}
