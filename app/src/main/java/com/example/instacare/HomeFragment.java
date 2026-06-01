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
import android.os.Handler;
import android.os.Looper;

public class HomeFragment extends Fragment {

    private List<ContactItem> contacts;
    private ContactsAdapter adapter;
    private RecyclerView contactsRecyclerView;
    // Views
    private FrameLayout notificationCard;
    private MaterialCardView cardWeather;
    private MaterialCardView cardNews;
    private MaterialCardView cardFirstAid;
    private MaterialCardView cardDisasterGuides;
    private MaterialCardView cardHospitals;
    private MaterialCardView cardEvacuation;
    private FrameLayout btnAddContact;
    // Motivational Carousel
    private androidx.viewpager2.widget.ViewPager2 motivationalViewPager;
    private android.widget.LinearLayout dotsContainer;
    private TextView tvMotivationalTitle;
    private TextView tvMotivationalSubtitle;
    private final Handler autoScrollHandler = new Handler(Looper.getMainLooper());
    private Runnable autoScrollRunnable;
    private androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback onPageChangeCallback;
    private boolean isAutomatedSetupActive = false;
    private com.example.instacare.utils.TutorialOverlayView tutorialOverlay;
    private int tutorialStep = 0;
    private boolean isTutorialActive = false;
    
    private final String[] motivationalTitles = {
        "Stay Motivated",
        "Be Prepared",
        "Stay Safe"
    };
    private final String[] motivationalSubtitles = {
        "A new day brings new opportunities to be safe.",
        "Know first aid — it can save a life.",
        "Find nearby hospitals when you need them most."
    };

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
        
        // --- PRE-WARM AI VOICE ENGINE ---
        // Initialize early to eliminate startup lag when tutorial begins
        tutorialOverlay = new com.example.instacare.utils.TutorialOverlayView(getContext());
        
        contactsRecyclerView = view.findViewById(R.id.contactsRecyclerView);
        notificationCard = view.findViewById(R.id.notificationCard);
        // Initial load of notification count
        loadNotificationCount(view);
        
        cardWeather = view.findViewById(R.id.cardWeather);
        cardNews = view.findViewById(R.id.cardNews);
        cardFirstAid = view.findViewById(R.id.cardFirstAid);
        cardDisasterGuides = view.findViewById(R.id.cardDisasterGuides);
        cardHospitals = view.findViewById(R.id.cardHospitals);
        btnAddContact = view.findViewById(R.id.btnAddContact);
        MaterialCardView cardEndorsement = view.findViewById(R.id.cardEndorsement);
        cardEvacuation = view.findViewById(R.id.cardEvacuation);

        // Motivational Carousel Setup
        motivationalViewPager = view.findViewById(R.id.motivationalViewPager);
        dotsContainer = view.findViewById(R.id.dotsContainer);
        tvMotivationalTitle = view.findViewById(R.id.tvMotivationalTitle);
        tvMotivationalSubtitle = view.findViewById(R.id.tvMotivationalSubtitle);
        setupMotivationalCarousel();


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

        // Endorsement card → open My Endorsements
        if (cardEndorsement != null) {
            cardEndorsement.setOnClickListener(v -> {
                if (getContext() == null) return;
                Intent intent = new Intent(getContext(), MyEndorsementsActivity.class);
                startActivity(intent);
            });
        }
        
        // Set Real User Name (First Name Only)
        SessionManager sessionManager = SessionManager.getInstance(requireContext());
        String username = sessionManager.getString("USER_USERNAME", "");
        
        // Initial load from session (supports legacy migration)
        updateGreeting(sessionManager.getString("USER_NAME", ""), view);
        
        // --- AUTOMATED SETUP TRIGGER ---
        Bundle args = getArguments();
        if (args != null && args.getBoolean("TRIGGER_ADD_CONTACT", false)) {
            // --- SUPPRESS SECOND DIALOG ---
            isAutomatedSetupActive = true;
            // Remove flag immediately to prevent repeat on rotation
            args.remove("TRIGGER_ADD_CONTACT");
            // Use handler to ensure UI is ready
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (isAdded()) triggerAddContact();
            }, 300);
        }
        
        // Real-time Database Activity: Fetch from DB if name is missing in session
        if (sessionManager.getString("USER_NAME", "").isEmpty() && !username.isEmpty()) {
            new Thread(() -> {
                if (getContext() == null || !isAdded()) return;
                com.example.instacare.data.local.User user = 
                    com.example.instacare.data.local.AppDatabase.getDatabase(getContext().getApplicationContext()).userDao().getUserByUsername(username);
                if (user != null && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateGreeting(user.fullName, view);
                        // Sync session
                        sessionManager.putString("USER_NAME", user.fullName);
                    });
                }
            }).start();
        }

        // Use handler to ensure UI is ready for tutorial check
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (isAdded() && !SessionManager.getInstance(requireContext()).getBoolean("HAS_SEEN_TUTORIAL_v2", false)) {
                startCaraTutorial();
            }
        }, 1000);

        // Sync Notification Count when BottomSheet is dismissed or swiped
        getParentFragmentManager().setFragmentResultListener("notification_sync", getViewLifecycleOwner(), (requestKey, bundle) -> {
            if (isAdded() && getView() != null) {
                loadNotificationCount(getView());
            }
        });

        // animateDashboardCategories(); // User requested removal
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // CLEANUP: Stop all carousel tasks to prevent crashes after logout/deletion
        if (autoScrollHandler != null) {
            autoScrollHandler.removeCallbacksAndMessages(null);
        }
        
        // Unregister and nullify callbacks to prevent Zombie executions during theme switch
        if (motivationalViewPager != null) {
            if (onPageChangeCallback != null) {
                motivationalViewPager.unregisterOnPageChangeCallback(onPageChangeCallback);
            }
            motivationalViewPager.setAdapter(null);
        }
        onPageChangeCallback = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) {
            loadNotificationCount(getView());
        }
    }

    private void updateGreeting(String fullName, View view) {
        if (fullName == null || fullName.isEmpty() || view == null) return;
        
        String firstName = fullName;
        if (fullName.contains(" ")) {
            firstName = fullName.split(" ")[0];
        }

        TextView tvGreeting = view.findViewById(R.id.tvGreeting);
        if (tvGreeting != null) {
            String baseText = "Hello, ";
            // Ensure first name for that flagship personal feel
            String fullText = baseText + firstName;
            
            android.text.SpannableStringBuilder spannable = new android.text.SpannableStringBuilder(fullText);
            int start = baseText.length();
            int end = fullText.length();
            
            android.content.Context context = view.getContext();
            // Use Branding Red for the user's name
            int color = (context != null) ? androidx.core.content.ContextCompat.getColor(context, R.color.emergency_red) : android.graphics.Color.RED;
            
            spannable.setSpan(
                new android.text.style.ForegroundColorSpan(color),
                start, 
                end, 
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            
            // Bold the name part for extra emphasis
            spannable.setSpan(
                new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                start,
                end,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            
            tvGreeting.setText(spannable);
        }
    }

    private void setupListeners() {

        notificationCard.setOnClickListener(v -> {
            if (isAdded() && getContext() != null) {
                NotificationBottomSheetFragment bottomSheet = new NotificationBottomSheetFragment();
                bottomSheet.show(getParentFragmentManager(), "notifications");
            }
        });

        if(cardWeather != null) {
            cardWeather.setOnClickListener(v -> {
                if (getContext() == null) return;
                Intent intent = new Intent(getContext(), WeatherActivity.class);
                startActivity(intent);
            });
        }

        if(cardNews != null) {
            cardNews.setOnClickListener(v -> {
                if (getContext() == null) return;
                Intent intent = new Intent(getContext(), NewsActivity.class);
                startActivity(intent);
            });
        }

        cardFirstAid.setOnClickListener(v -> {
            // Navigate to Guides Fragment
            if (getActivity() instanceof UserDashboardActivity) {
                ((UserDashboardActivity) getActivity()).navigateToFragment("Guides");
            }
        });

        if (cardDisasterGuides != null) {
            cardDisasterGuides.setOnClickListener(v -> {
                // Navigate to Guides Fragment with Disaster selected
                if (getActivity() instanceof UserDashboardActivity) {
                    android.os.Bundle args = new android.os.Bundle();
                    args.putBoolean("SELECT_DISASTER", true);
                    ((UserDashboardActivity) getActivity()).navigateToFragment("Guides", args);
                }
            });
        }

        cardHospitals.setOnClickListener(v -> {
            // Navigate to Hospitals Fragment
            if (getActivity() instanceof UserDashboardActivity) {
                ((UserDashboardActivity) getActivity()).navigateToFragment("Hospitals");
            }
        });

        if (cardEvacuation != null) {
            cardEvacuation.setOnClickListener(v -> {
                if (getActivity() instanceof UserDashboardActivity) {
                    android.os.Bundle args = new android.os.Bundle();
                    args.putBoolean("SELECT_EVACUATION", true);
                    ((UserDashboardActivity) getActivity()).navigateToFragment("Hospitals", args);
                }
            });
        }

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
        adapter = new ContactsAdapter(contacts, new ContactsAdapter.OnContactClickListener() {
            @Override
            public void onCallClick(String number) {
                if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CALL_PHONE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{android.Manifest.permission.CALL_PHONE}, 1);
                } else {
                    Intent intent = new Intent(Intent.ACTION_CALL);
                    intent.setData(Uri.parse("tel:" + number));
                    startActivity(intent);
                }
            }

            @Override
            public void onContactLongClick(ContactItem item) {
                showDeleteConfirmationDialog(item);
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
        
        // Get current UID from session for stable account isolation
        SessionManager sessionManager = SessionManager.getInstance(requireContext());
        int currentUid = sessionManager.getCurrentUserUid();

        new Thread(() -> {
            List<com.example.instacare.data.local.EmergencyContact> dbContacts = 
                com.example.instacare.data.local.AppDatabase.getDatabase(appContext).emergencyContactDao().getContactsByUser(currentUid);
            
            if (getActivity() == null) return;
            
            getActivity().runOnUiThread(() -> {
                if (!isAdded() || getContext() == null) return;
                
                contacts.clear();
                for (com.example.instacare.data.local.EmergencyContact c : dbContacts) {
                    contacts.add(new ContactItem(c.id, c.name, c.relation, c.phoneNumber));
                }
                adapter.notifyDataSetChanged();
                

                
                // Post-tutorial check is OPTIONAL (non-mandatory)
                if (contacts.isEmpty()) {
                    sessionManager.putBoolean("CONTACTS_SETUP", false);
                    checkMandatoryContacts(false);
                } else {
                    sessionManager.putBoolean("CONTACTS_SETUP", true);
                }
            });
        }).start();
    }
    
    public void requestOptionalSetup() {
        if (isAdded()) {
            checkMandatoryContacts(false);
        }
    }
    
    private void checkMandatoryContacts(boolean isForceMandatory) {
        if (!isAdded() || getContext() == null) return;
        
        // Per-account check
        SessionManager sessionManager = SessionManager.getInstance(requireContext());
        int currentUid = sessionManager.getCurrentUserUid();
        
        if (contacts.isEmpty()) {
            boolean hasSeenTutorial = sessionManager.getBoolean("HAS_SEEN_TUTORIAL_v2", false);
            if (!hasSeenTutorial || isTutorialActive) return; // Suppress while tutorial is running or pending
            // --- IRONCLAD GUARD: Skip dialog if already in automated flow from SOS ---
            if (isAutomatedSetupActive) return;

            // Show Mandatory Setup Dialog (Custom Dark Theme)
            android.view.View dialogView = android.view.LayoutInflater.from(getContext()).inflate(R.layout.dialog_mandatory_setup, null);
            
            // Highlight "Required" in Red
            android.widget.TextView tvTitle = dialogView.findViewById(R.id.tvEmergencyTitle);
            if (tvTitle != null) {
                String titleText = isForceMandatory ? "Emergency Setup <font color='#E53935'>Required</font>" : "Safety <font color='#E53935'>Setup Recommended</font>";
                tvTitle.setText(android.text.Html.fromHtml(titleText, android.text.Html.FROM_HTML_MODE_LEGACY));
            }

            androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setCancelable(!isForceMandatory)
                .create();

            // --- SOS GUARD: Block Back Button only if Mandatory ---
            dialog.setOnKeyListener((dialogInterface, keyCode, event) -> {
                if (isForceMandatory && keyCode == android.view.KeyEvent.KEYCODE_BACK) {
                    return true; // Consume the event (stay on the dialog)
                }
                return false;
            });
            
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            }
            
            // Setup "Later" button based on context
            android.view.View btnLater = dialogView.findViewById(R.id.btnSetupLater);
            if (btnLater != null) {
                btnLater.setVisibility(isForceMandatory ? android.view.View.GONE : android.view.View.VISIBLE);
                btnLater.setOnClickListener(v -> dialog.dismiss());
            }

            dialogView.findViewById(R.id.btnAddContactNow).setOnClickListener(v -> {
                 if (isAdded() && getContext() != null) {
                    dialog.dismiss();
                    String[] permissions = {
                        android.Manifest.permission.READ_CONTACTS,
                        android.Manifest.permission.SEND_SMS,
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    };
                    requestPermissionsLauncher.launch(permissions);
                 }
            });
            
            // Animate Dialog Appearance
            android.view.animation.Animation scaleUp = android.view.animation.AnimationUtils.loadAnimation(getContext(), R.anim.scale_up);
            dialogView.startAnimation(scaleUp);
            
            // --- PREMIUM AESTHETIC: Deep Dim + Frosted Glass Blur ---
            com.example.instacare.utils.BlurUtils.applyBlur(dialog);
            
            dialog.show();
        } else {
            // Contacts already exist — mark as done for this account
            sessionManager.putBoolean("CONTACTS_SETUP", true);
        }
    }

    private void animateDashboardCategories() {
        // Disabled per user request to clean up Home UI
    }

    public void triggerAddContact() {
        if (btnAddContact != null) {
            btnAddContact.performClick();
        }
    }

    private void launchContactPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI);
        contactPickerLauncher.launch(intent);
    }

    private void addContactFromUri(Uri contactUri) {
        if (getContext() == null) return;
        String[] projection = {android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER};
        try (android.database.Cursor cursor = getContext().getContentResolver().query(contactUri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int numberIndex = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER);
                
                String name = cursor.getString(nameIndex);
                String number = cursor.getString(numberIndex);
                
                // Get current UID for stable account isolation
                SessionManager sessionManager = SessionManager.getInstance(requireContext());
                int currentUid = sessionManager.getCurrentUserUid();
                
                // Save to DB with user association (Stable identifier)
                new Thread(() -> {
                    if (getContext() == null) return;
                    com.example.instacare.data.local.EmergencyContact newContact = 
                        new com.example.instacare.data.local.EmergencyContact(currentUid, name, number, "Emergency");
                    com.example.instacare.data.local.AppDatabase.getDatabase(getContext()).emergencyContactDao().insert(newContact);
                    
                    // --- REWARD TRIGGER: Welcome Notification (Fires once upon first completed setup) ---
                    com.example.instacare.data.local.AppDatabase db = com.example.instacare.data.local.AppDatabase.getDatabase(getContext());
                    boolean wasSetup = sessionManager.getBoolean("CONTACTS_SETUP", false);
                    if (!wasSetup) {
                        String realName = sessionManager.getString("USER_NAME", "User");
                        String fName = realName.contains(" ") ? realName.split(" ")[0] : realName;
                        com.example.instacare.data.local.Notification welcomeMsg = new com.example.instacare.data.local.Notification(
                            "Emergency Setup <font color='#E53935'>Complete!</font>",
                            "Hi " + fName + "! <font color='#E53935'>Salamat</font> sa pag setup satoang Emergency Contacts. You're now fully prepared for any situation. Have a nice day! 😊",
                            System.currentTimeMillis(),
                            false,
                            "WELCOME",
                            0,
                            "USER",
                            currentUid
                        );
                        db.notificationDao().insert(welcomeMsg);
                    }

                    // Mark contacts setup as done for this account
                    sessionManager.putBoolean("CONTACTS_SETUP", true);
                    
                    getActivity().runOnUiThread(() -> {
                        loadContactsFromDB(); // Reload to update UI and satisfy check
                        loadNotificationCount(getView()); // INSTANT SYNC: Update the top-right badge immediately
                    });
                }).start();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Failed to retrieve contact", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleSOS() {
        if (getContext() == null) return;
        SessionManager sessionManager = SessionManager.getInstance(requireContext());
        int currentUid = sessionManager.getCurrentUserUid();
        
        new Thread(() -> {
            int count = com.example.instacare.data.local.AppDatabase.getDatabase(getContext()).emergencyContactDao().getCountByUser(currentUid);
            
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (count > 0) {
                    // Normal flow
                    SOSFlowManager.startFlow(getContext());
                } else {
                    // ACTION REQUIRED: User must add someone before they can use SOS
                    Toast.makeText(getContext(), "⚠️ SOS Disabled: Please add at least 1 Emergency Contact first!", Toast.LENGTH_LONG).show();
                    checkMandatoryContacts(true); 
                }
            });
        }).start();
    }

    private void loadNotificationCount(View view) {
        if (getContext() == null) return;
        android.content.Context appContext = getContext().getApplicationContext();
        
        new Thread(() -> {
            SessionManager sessionManager = SessionManager.getInstance(appContext);
            boolean notificationsEnabled = sessionManager.getBoolean("NOTIFICATIONS_ENABLED", true);
            int currentUid = sessionManager.getCurrentUserUid();
            int unreadCount = com.example.instacare.data.local.AppDatabase.getDatabase(appContext).notificationDao().getUnreadCountForUser(currentUid);
            
            int finalUnreadCount = unreadCount;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    TextView indicator = view.findViewById(R.id.notificationIndicator);
                    android.widget.ImageView ivBell = view.findViewById(R.id.ivNotificationBell);
                    
                    // SYNC: Check actual system permission
                    if (getContext() == null) return;
                    boolean systemEnabled = androidx.core.app.NotificationManagerCompat.from(getContext()).areNotificationsEnabled();
                    
                    if (indicator != null) {
                        if (notificationsEnabled && systemEnabled && finalUnreadCount > 0 && !isTutorialActive) {
                            indicator.setVisibility(View.VISIBLE);
                            indicator.setText(finalUnreadCount > 99 ? "99+" : String.valueOf(finalUnreadCount));
                            
                            // --- Assistant Center Avatar stays Static ---
                            if (ivBell != null) {
                                ivBell.setImageResource(R.drawable.cara_avatar);
                            }
                        } else {
                            indicator.setVisibility(View.GONE);
                            
                            // --- Normal Avatar (No Unread) ---
                            if (ivBell != null) {
                                ivBell.setImageResource(R.drawable.cara_avatar);
                            }
                        }
                    }
                });
            }
        }).start();
    }

    private void setupMotivationalCarousel() {
        int[] images = {
            R.drawable.motivational_stay_motivated,
            R.drawable.motivational_be_prepared,
            R.drawable.motivational_stay_safe
        };
        MotivationalCarouselAdapter carouselAdapter = new MotivationalCarouselAdapter(images);
        motivationalViewPager.setAdapter(carouselAdapter);
        
        // Revert to Horizontal Orientation (Left and Right slide animation)
        motivationalViewPager.setOrientation(androidx.viewpager2.widget.ViewPager2.ORIENTATION_HORIZONTAL);
        
        // Build indicators AFTER first layout pass to fix first-load invisibility issue
        dotsContainer.post(() -> {
            if (isAdded() && getContext() != null) {
                buildDots(images.length, 0);
            }
        });
        
        // Page change callback to update dots + text (Field-based for clean unregistration)
        onPageChangeCallback = new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (isAdded() && getContext() != null) {
                    buildDots(images.length, position);
                    tvMotivationalTitle.setText(motivationalTitles[position]);
                    tvMotivationalSubtitle.setText(motivationalSubtitles[position]);
                }
            }
        };
        motivationalViewPager.registerOnPageChangeCallback(onPageChangeCallback);
        
        // Auto Scroll every 4 seconds
        autoScrollRunnable = new Runnable() {
            @Override
            public void run() {
                if (motivationalViewPager != null && motivationalViewPager.getAdapter() != null) {
                    int current = motivationalViewPager.getCurrentItem();
                    int total = motivationalViewPager.getAdapter().getItemCount();
                    motivationalViewPager.setCurrentItem((current + 1) % total, true);
                }
                autoScrollHandler.postDelayed(this, 4000);
            }
        };
        autoScrollHandler.postDelayed(autoScrollRunnable, 4000);
    }
    
    private void buildDots(int count, int selected) {
        // ULTIMATE GUARD: Never access resources if not attached or added
        if (dotsContainer == null || !isAdded() || getContext() == null) return;
        
        dotsContainer.removeAllViews();
        dotsContainer.setWeightSum(count); // Explicitly set weight sum to fix first load calculation
        
        // Use getContext().getResources() instead of Fragment.getResources() for absolute reliability
        float density = getContext().getResources().getDisplayMetrics().density;
        
        for (int i = 0; i < count; i++) {
            View segment = new View(getContext());
            // layout_weight = 1.0f to distribute width evenly
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(0, (int)(3 * density), 1.0f);
            int margin = (int)(2 * density); // spacing between segments
            params.setMargins(margin, 0, margin, 0);
            segment.setLayoutParams(params);
            
            android.graphics.drawable.GradientDrawable shape = new android.graphics.drawable.GradientDrawable();
            shape.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            shape.setCornerRadius(1.5f * density); // slightly rounded caps
            // Full white for selected, 40% white for inactive
            shape.setColor(i == selected ? 0xFFFFFFFF : 0x66FFFFFF);
            segment.setBackground(shape);
            
            dotsContainer.addView(segment);
        }
        dotsContainer.requestLayout();
    }

    private void startCaraTutorial() {
        if (!isAdded() || getActivity() == null) return;
        
        isTutorialActive = true;
        tutorialStep = 0;
        
        // Use pre-warmed overlay or create if missing
        if (tutorialOverlay == null) {
            tutorialOverlay = new com.example.instacare.utils.TutorialOverlayView(requireContext());
        }
        
        // Add overlay to DECOR VIEW for full-screen coverage (status/nav bars)
        ViewGroup root = (ViewGroup) getActivity().getWindow().getDecorView();
        if (tutorialOverlay.getParent() != null) {
            ((ViewGroup)tutorialOverlay.getParent()).removeView(tutorialOverlay);
        }
        root.addView(tutorialOverlay, new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            
        // Force WHITE status bar icons only during the dark tutorial overlay
        androidx.core.view.WindowInsetsControllerCompat ctrl = 
            androidx.core.view.WindowCompat.getInsetsController(getActivity().getWindow(), getActivity().getWindow().getDecorView());
        if (ctrl != null) {
            ctrl.setAppearanceLightStatusBars(false); // false = white
        }    
            
        nextTutorialStep();
    }

    private void nextTutorialStep() {
        if (!isAdded() || tutorialOverlay == null) return;
        
        tutorialStep++;
        
        String fullName = SessionManager.getInstance(requireContext()).getString("USER_NAME", "User");
        String firstName = fullName.split(" ")[0];

        switch (tutorialStep) {
            case 1:
                // Intro: No target, center bubble, UNMUTED
                tutorialOverlay.setTarget(null, 
                    "Hi " + firstName + "! I'm Cara, your barkada for safety. Let me show you how InstaCare works! 😊", 
                    false, 0, false, false, this::nextTutorialStep); // isMuted = false
                break;
            case 2:
                // Weather Card: Wide shadow glow
                tutorialOverlay.setTarget(cardWeather, 
                    "Here's your real-time weather status. Stay updated on current conditions to keep yourself prepared!", 
                    false, 40, this::nextTutorialStep);
                break;
                
            case 3:
                // News Card: Wide shadow glow
                tutorialOverlay.setTarget(cardNews, 
                    "Discover the latest updates and disaster news right here.", 
                    false, 40, this::nextTutorialStep);
                break;
                
            case 4:
                // Guides Quick Access: First Aid & Disasters
                android.widget.ScrollView sc1 = getView() != null ? getView().findViewById(R.id.mainScrollView) : null;
                View rowGuides = getView() != null ? getView().findViewById(R.id.rowGuides) : null;
                
                if (sc1 != null && rowGuides != null) {
                    int viewportHeight = sc1.getHeight();
                    int targetYInViewport = (int) (viewportHeight * 0.65); 
                    // Instant jump — blur hides the background anyway
                    sc1.scrollTo(0, Math.max(0, rowGuides.getTop() - targetYInViewport));
                    sc1.post(() -> {
                        if (isAdded() && tutorialOverlay != null) {
                            tutorialOverlay.setTarget(rowGuides, 
                                "Read our comprehensive Guides for First Aid Tips and Disaster Preparedness to stay one step ahead.", 
                                false, 15, true, HomeFragment.this::nextTutorialStep);
                        }
                    });
                } else {
                    nextTutorialStep();
                }
                break;
                
            case 5:
                // Locations Quick Access: Hospitals & Evacuation
                android.widget.ScrollView sc2 = getView() != null ? getView().findViewById(R.id.mainScrollView) : null;
                View rowLocations = getView() != null ? getView().findViewById(R.id.rowLocations) : null;
                
                if (sc2 != null && rowLocations != null) {
                    int viewportHeight = sc2.getHeight();
                    int targetYInViewport = (int) (viewportHeight * 0.65);
                    sc2.scrollTo(0, Math.max(0, rowLocations.getTop() - targetYInViewport));
                    sc2.post(() -> {
                        if (isAdded() && tutorialOverlay != null) {
                            tutorialOverlay.setTarget(rowLocations, 
                                "Use this to find Nearest Hospitals and Evacuation Centers instantly.", 
                                false, 15, true, HomeFragment.this::nextTutorialStep);
                        }
                    });
                } else {
                    nextTutorialStep();
                }
                break;
                
              case 6:
                // SOS FAB
                android.widget.ScrollView scSOS = getView() != null ? getView().findViewById(R.id.mainScrollView) : null;
                android.view.View fab = getActivity().findViewById(R.id.fabSOS);
                
                if (scSOS != null) {
                    // Instant jump to bottom
                    int maxScroll = scSOS.getChildAt(0).getHeight() - scSOS.getHeight();
                    scSOS.scrollTo(0, Math.max(0, maxScroll));
                }
                
                scSOS.post(() -> {
                    if (isAdded() && tutorialOverlay != null && fab != null) {
                        tutorialOverlay.setTarget(fab, 
                            "And the most important! Press this red button for 5 seconds if you're in real danger. Responders will find you immediately.", 
                            true, 15, true, HomeFragment.this::nextTutorialStep);
                    } else if (tutorialOverlay != null) {
                        nextTutorialStep();
                    }
                });
                break;
                
            case 7:
                // End Tutorial: Clean up and reset
                isTutorialActive = false;
                
                // RESET SCROLL: Bring the user back to the top to start their session fresh
                android.widget.ScrollView scEnd = getView() != null ? getView().findViewById(R.id.mainScrollView) : null;
                if (scEnd != null) {
                    scEnd.post(() -> scEnd.smoothScrollTo(0, 0));
                }
                
                ViewGroup root = (ViewGroup) getActivity().getWindow().getDecorView();
                root.removeView(tutorialOverlay);
                tutorialOverlay = null;
                
                // RESTORE Adaptive status bar behavior
                if (getActivity() instanceof com.example.instacare.UserDashboardActivity) {
                    ((com.example.instacare.UserDashboardActivity) getActivity()).applyFinalStatusBarAppearance();
                }
                
                // Save state
                SessionManager.getInstance(requireContext()).putBoolean("HAS_SEEN_TUTORIAL_v2", true);
                
                // FINAL BRIDGE: Show Professional Optional Dialog after Tutorial
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (isAdded()) checkMandatoryContacts(false);
                }, 500);
                break;
        }
    }
    private void showDeleteConfirmationDialog(ContactItem item) {
        if (!isAdded() || getContext() == null) return;

        android.view.View dialogView = android.view.LayoutInflater.from(getContext()).inflate(R.layout.dialog_mandatory_setup, null);
        
        // Repurpose Mandatory Setup Dialog for Delete Confirmation
        android.widget.TextView tvTitle = dialogView.findViewById(R.id.tvEmergencyTitle);
        android.widget.TextView tvDesc = dialogView.findViewById(R.id.tvEmergencyDesc);
        android.widget.Button btnDelete = dialogView.findViewById(R.id.btnAddContactNow);
        android.widget.Button btnCancel = dialogView.findViewById(R.id.btnSetupLater);

        if (tvTitle != null) tvTitle.setText(android.text.Html.fromHtml("Delete <font color='#E53935'>Contact?</font>", android.text.Html.FROM_HTML_MODE_LEGACY));
        if (tvDesc != null) tvDesc.setText(android.text.Html.fromHtml("Are you sure you want to remove <font color='#E53935'>" + item.getName() + "</font> from your emergency contacts?", android.text.Html.FROM_HTML_MODE_LEGACY));
        if (btnDelete != null) btnDelete.setText("Delete Now");
        if (btnCancel != null) btnCancel.setVisibility(View.VISIBLE);

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(getContext())
            .setView(dialogView)
            .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnDelete.setOnClickListener(v -> {
            dialog.dismiss();
            deleteContact(item.getId());
        });

        // --- PREMIUM AESTHETIC: Deep Dim + Frosted Glass Blur ---
        com.example.instacare.utils.BlurUtils.applyBlur(dialog);

        // Animate Dialog Appearance (Same as Safety Setup Recommended)
        android.view.animation.Animation scaleUp = android.view.animation.AnimationUtils.loadAnimation(getContext(), R.anim.scale_up);
        dialogView.startAnimation(scaleUp);

        dialog.show();
    }

    private void deleteContact(int contactId) {
        new Thread(() -> {
            if (getContext() == null) return;
            com.example.instacare.data.local.AppDatabase.getDatabase(getContext()).emergencyContactDao().deleteById(contactId);
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(getContext(), "Contact deleted", Toast.LENGTH_SHORT).show();
                    loadContactsFromDB();
                });
            }
        }).start();
    }
}
