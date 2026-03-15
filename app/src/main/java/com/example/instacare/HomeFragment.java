package com.example.instacare;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private List<ContactItem> contacts;
    private ContactsAdapter adapter;
    private RecyclerView contactsRecyclerView;
    private View sosSpinner;
    // Views
    private FrameLayout sosButton;
    private FrameLayout notificationCard;
    private MaterialCardView cardWeather;
    private MaterialCardView cardNews;
    private MaterialCardView cardFirstAid;
    private MaterialCardView cardHospitals;
    private FrameLayout btnAddContact;

    private final androidx.activity.result.ActivityResultLauncher<String[]> requestPermissionsLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions(), result -> {
                Boolean readingContacts = result.getOrDefault(android.Manifest.permission.READ_CONTACTS, false);
                if (Boolean.TRUE.equals(readingContacts)) {
                    launchContactPicker();
                } else {
                    // Check if other SOS permissions were granted
                    boolean sosPermissionsGranted = Boolean.TRUE.equals(result.get(android.Manifest.permission.SEND_SMS)) &&
                                                  Boolean.TRUE.equals(result.get(android.Manifest.permission.ACCESS_FINE_LOCATION));
                    
                    if (sosPermissionsGranted) {
                        Toast.makeText(getContext(), "SOS Permissions Granted!", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private final androidx.activity.result.ActivityResultLauncher<Intent> contactPickerLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && result.getData() != null) {
                    Uri contactUri = result.getData().getData();
                    addContactFromUri(contactUri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        contactsRecyclerView = view.findViewById(R.id.contactsRecyclerView);
        sosSpinner = view.findViewById(R.id.sosSpinner);
        sosButton = view.findViewById(R.id.sosButton);
        notificationCard = view.findViewById(R.id.notificationCard);
        // Hide indicator by default (No notifications)
        View indicator = view.findViewById(R.id.notificationIndicator);
        if (indicator != null) {
            indicator.setVisibility(View.GONE);
        }
        
        cardWeather = view.findViewById(R.id.cardWeather);
        cardNews = view.findViewById(R.id.cardNews);
        cardFirstAid = view.findViewById(R.id.cardFirstAid);
        cardHospitals = view.findViewById(R.id.cardHospitals);
        btnAddContact = view.findViewById(R.id.btnAddContact);
        MaterialCardView cardEndorsement = view.findViewById(R.id.cardEndorsement);

        // Edge-to-Edge inset handling for the top bar
        View topBar = view.findViewById(R.id.topBar);
        if (topBar != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, windowInsets) -> {
                androidx.core.graphics.Insets insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
                int padding20 = (int)(20 * getResources().getDisplayMetrics().density);
                v.setPadding(padding20, insets.top + padding20, padding20, padding20);
                return windowInsets;
            });
        }

        setupListeners();
        setupContacts();
        startSpinAnimation();

        // Endorsement card → open My Endorsements
        if (cardEndorsement != null) {
            cardEndorsement.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), MyEndorsementsActivity.class);
                startActivity(intent);
            });
        }
        
        // Set Real User Name (First Name Only)
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("InstaCarePrefs", android.content.Context.MODE_PRIVATE);
        String username = prefs.getString("USER_USERNAME", "");
        
        // Initial load from prefs
        updateGreeting(prefs.getString("USER_NAME", ""), view);
        
        // Real-time Database Activity: Fetch from DB
        if (!username.isEmpty()) {
            new Thread(() -> {
                com.example.instacare.data.local.User user = 
                    com.example.instacare.data.local.AppDatabase.getDatabase(requireContext()).userDao().getUserByUsername(username);
                if (user != null && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateGreeting(user.fullName, view);
                        // Sync prefs
                        prefs.edit().putString("USER_NAME", user.fullName).apply();
                    });
                }
            }).start();
        }

        animateDashboardCategories();
    }

    private void updateGreeting(String fullName, View view) {
        if (fullName == null || fullName.isEmpty()) return;
        
        String firstName = fullName;
        if (fullName.contains(" ")) {
            firstName = fullName.split(" ")[0];
        }
        
        TextView tvGreeting = view.findViewById(R.id.tvGreeting);
        if (tvGreeting != null) {
            tvGreeting.setText("Hello, " + firstName);
        }
    }

    private void setupListeners() {
        sosButton.setOnClickListener(v -> handleSOS());

        notificationCard.setOnClickListener(v -> {
            NotificationBottomSheetFragment bottomSheet = new NotificationBottomSheetFragment();
            bottomSheet.show(getParentFragmentManager(), "notifications");
        });

        if(cardWeather != null) {
            cardWeather.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), WeatherActivity.class);
                startActivity(intent);
            });
        }

        if(cardNews != null) {
            cardNews.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), NewsActivity.class);
                startActivity(intent);
            });
        }

        cardFirstAid.setOnClickListener(v -> {
            // Navigate to Guides Fragment
            if (getActivity() instanceof UserDashboardActivity) {
                ((UserDashboardActivity) getActivity()).navigateToFragment("Guides");
            }
        });

        cardHospitals.setOnClickListener(v -> {
            // Navigate to Hospitals Fragment
            if (getActivity() instanceof UserDashboardActivity) {
                ((UserDashboardActivity) getActivity()).navigateToFragment("Hospitals");
            }
        });

        btnAddContact.setOnClickListener(v -> {
            String[] permissions = {
                android.Manifest.permission.READ_CONTACTS,
                android.Manifest.permission.SEND_SMS,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            };
            requestPermissionsLauncher.launch(permissions);
        });
    }

    private void setupContacts() {
        contacts = new ArrayList<>();
        adapter = new ContactsAdapter(contacts, number -> {
            if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CALL_PHONE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.CALL_PHONE}, 1);
            } else {
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:" + number));
                startActivity(intent);
            }
        });

        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        contactsRecyclerView.setAdapter(adapter);
        
        loadContactsFromDB();
    }

    private void loadContactsFromDB() {
        // Use Application Context for DB to avoid detachment crashes
        if (getContext() == null) return;
        android.content.Context appContext = getContext().getApplicationContext();
        
        // Get current user's username for stable account isolation
        String username = requireContext().getSharedPreferences("InstaCarePrefs", android.content.Context.MODE_PRIVATE)
            .getString("USER_USERNAME", "");

        new Thread(() -> {
            List<com.example.instacare.data.local.EmergencyContact> dbContacts = 
                com.example.instacare.data.local.AppDatabase.getDatabase(appContext).emergencyContactDao().getContactsByUser(username);
            
            if (getActivity() == null) return;
            
            getActivity().runOnUiThread(() -> {
                if (!isAdded() || getContext() == null) return;
                
                contacts.clear();
                for (com.example.instacare.data.local.EmergencyContact c : dbContacts) {
                    contacts.add(new ContactItem(c.name, c.relation, c.phoneNumber));
                }
                adapter.notifyDataSetChanged();
                
                checkMandatoryContacts();
            });
        }).start();
    }
    
    private void checkMandatoryContacts() {
        if (!isAdded() || getContext() == null) return;
        
        // Per-account check: only show dialog if this account hasn't set up contacts yet
        android.content.SharedPreferences prefs = requireContext().getSharedPreferences("InstaCarePrefs", android.content.Context.MODE_PRIVATE);
        String username = prefs.getString("USER_USERNAME", "");
        boolean contactsSetupDone = prefs.getBoolean("CONTACTS_SETUP_" + username, false);
        
        if (contactsSetupDone) return; // Already set up or dismissed for this account
        
        if (contacts.isEmpty()) {
            // Show Mandatory Setup Dialog (Custom Dark Theme)
            android.view.View dialogView = android.view.LayoutInflater.from(requireContext()).inflate(R.layout.dialog_mandatory_setup, null);
            
            androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();
            
            // Set Transparent Background for CardView
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            }
            
            dialogView.findViewById(R.id.btnAddContactNow).setOnClickListener(v -> {
                 if (isAdded() && getContext() != null) {
                    // Mark as done for this account (stable username)
                    requireContext().getSharedPreferences("InstaCarePrefs", android.content.Context.MODE_PRIVATE)
                        .edit().putBoolean("CONTACTS_SETUP_" + username, true).apply();
                    
                    String[] permissions = {
                        android.Manifest.permission.READ_CONTACTS,
                        android.Manifest.permission.SEND_SMS,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    };
                    requestPermissionsLauncher.launch(permissions);
                    dialog.dismiss();
                 }
            });
            
            // Animate Dialog Appearance
            android.view.animation.Animation scaleUp = android.view.animation.AnimationUtils.loadAnimation(requireContext(), R.anim.scale_up);
            dialogView.startAnimation(scaleUp);
            
            dialog.show();
        } else {
            // Contacts already exist — mark as done for this account
            prefs.edit().putBoolean("CONTACTS_SETUP_" + username, true).apply();
        }
    }

    private void animateDashboardCategories() {
        android.view.animation.Animation slideUp = android.view.animation.AnimationUtils.loadAnimation(requireContext(), android.R.anim.slide_in_left);
        slideUp.setDuration(500);
        
        notificationCard.startAnimation(slideUp);
        
        android.view.animation.Animation slideUp2 = android.view.animation.AnimationUtils.loadAnimation(requireContext(), android.R.anim.slide_in_left);
        slideUp2.setStartOffset(100);
        slideUp2.setDuration(500);
        cardFirstAid.startAnimation(slideUp2);
        
        android.view.animation.Animation slideUp3 = android.view.animation.AnimationUtils.loadAnimation(requireContext(), android.R.anim.slide_in_left);
        slideUp3.setStartOffset(200);
        slideUp3.setDuration(500);
        cardHospitals.startAnimation(slideUp3);
    }

    private void launchContactPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        contactPickerLauncher.launch(intent);
    }

    private void addContactFromUri(Uri contactUri) {
        String[] projection = {android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER};
        try (android.database.Cursor cursor = requireContext().getContentResolver().query(contactUri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int numberIndex = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER);
                
                String name = cursor.getString(nameIndex);
                String number = cursor.getString(numberIndex);
                
                // Get current user's username for stable account isolation
                String username = requireContext().getSharedPreferences("InstaCarePrefs", android.content.Context.MODE_PRIVATE)
                    .getString("USER_USERNAME", "");
                
                // Save to DB with user association (Stable identifier)
                new Thread(() -> {
                    com.example.instacare.data.local.EmergencyContact newContact = 
                        new com.example.instacare.data.local.EmergencyContact(name, number, "Emergency");
                    newContact.username = username;
                    com.example.instacare.data.local.AppDatabase.getDatabase(requireContext()).emergencyContactDao().insert(newContact);
                    
                    // Mark contacts setup as done for this account
                    requireContext().getSharedPreferences("InstaCarePrefs", android.content.Context.MODE_PRIVATE)
                        .edit().putBoolean("CONTACTS_SETUP_" + username, true).apply();
                    
                    getActivity().runOnUiThread(() -> loadContactsFromDB()); // Reload to update UI and satisfy check
                }).start();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to retrieve contact", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleSOS() {
        if (contacts.isEmpty()) {
            Toast.makeText(getContext(), "⚠️ Please add an Emergency Contact first!", Toast.LENGTH_LONG).show();
            checkMandatoryContacts(); // Show dialog again
            return;
        }
        
        Intent intent = new Intent(getContext(), SOSActivity.class);
        startActivity(intent);
    }

    private void startSpinAnimation() {
        android.animation.ObjectAnimator rotate = android.animation.ObjectAnimator.ofFloat(sosSpinner, "rotation", 0f, 360f);
        rotate.setDuration(2000);
        rotate.setRepeatCount(android.animation.ObjectAnimator.INFINITE);
        rotate.setInterpolator(new android.view.animation.LinearInterpolator());
        rotate.start();
    }
}
