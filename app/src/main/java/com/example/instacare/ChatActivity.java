package com.example.instacare;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.instacare.data.local.AppDatabase;
import com.example.instacare.data.local.ChatMessage;
import com.example.instacare.databinding.ActivityChatBinding;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.net.Uri;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;
    private ChatAdapter adapter;
    private AppDatabase db;
    private int endorsementId;
    private int currentUserId;
    private int otherUserId;
    private String conversationRole; // "ADMIN" or "BARANGAY"
    private String currentUserRole;
    private ChatMessage replyingTo = null;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private String resolvedUserEmail = "";

    // Persist shown typing state across activity recreations
    private static final java.util.Set<Integer> shownTypingEndorsements = new java.util.HashSet<>();
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Load Database and Prefs EARLY
        db = AppDatabase.getDatabase(this);
        SessionManager sessionManager = SessionManager.getInstance(this);
        currentUserRole = sessionManager.getString("USER_ROLE", "USER");
        currentUserId = sessionManager.getCurrentUserUid();

        // Force Light Mode ONLY for Barangay Staff
        // Users will follow the global app theme (no local override)
        if ("BARANGAY".equals(currentUserRole)) {
            if (getDelegate().getLocalNightMode() != AppCompatDelegate.MODE_NIGHT_NO) {
                getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        }

        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Get intent data
        endorsementId = getIntent().getIntExtra("ENDORSEMENT_ID", -1);
        otherUserId = getIntent().getIntExtra("OTHER_USER_ID", 0);
        String caseRef = getIntent().getStringExtra("CASE_REF");

        // BLOCK ADMIN ACCESS
        if ("ADMIN".equals(currentUserRole)) {
            Toast.makeText(this, "Not authorized: Admin has no chat access", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Binary Flow: conversationRole is strictly BARANGAY
        conversationRole = "BARANGAY";

        if (endorsementId == -1) {
            Toast.makeText(this, "Invalid case", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        setupUI(caseRef);
        loadMessagesAndAutoWelcome();
        loadBarangayDetails();

        setupImagePicker();
    }

    private void loadBarangayDetails() {
        Executors.newSingleThreadExecutor().execute(() -> {
            com.example.instacare.data.local.Endorsement endorsement = db.endorsementDao().getEndorsementById(endorsementId);
            if (endorsement != null) {
                String barangayName = endorsement.barangayZone != null ? endorsement.barangayZone : "Bagontaas";
                
                // Demo mapping: Map "Pending" to "Bagontaas" as requested
                if ("Pending Zone".equalsIgnoreCase(barangayName) || "Pending".equalsIgnoreCase(barangayName)) {
                    barangayName = "Bagontaas";
                }

                // Format "Bagontaas Zone" -> "Barangay Bagontaas"
                if (barangayName.toLowerCase().contains("zone")) {
                    barangayName = "Barangay " + barangayName.replaceAll("(?i)\\s*zone.*", "").trim();
                } else if (!barangayName.toLowerCase().startsWith("barangay")) {
                    barangayName = "Barangay " + barangayName;
                }

                String finalBarangayName = barangayName;
                runOnUiThread(() -> {
                    binding.tvChatTitle.setText(finalBarangayName);
                    binding.tvChatSubtitle.setText("Barangay Support • " + endorsement.caseRef);
                });
            }
        });
    }

    private void setupUI(String caseRef) {
        SessionManager sessionManager = SessionManager.getInstance(this);
        int currentUserId = sessionManager.getCurrentUserUid();
        
        // Resolve the ID of the person with the "USER" role in this convo
        final int[] resolvedUserId = {currentUserId}; 
        if ("BARANGAY".equals(currentUserRole)) {
            // I am the staff, the other person is the "USER"
            Executors.newSingleThreadExecutor().execute(() -> {
                com.example.instacare.data.local.User userObj = db.userDao().getUserById(otherUserId);
                if (userObj != null) {
                    resolvedUserId[0] = userObj.uid;
                    runOnUiThread(() -> {
                        if (adapter != null) adapter.setUserId(resolvedUserId[0]);
                    });
                }
            });
        }

        adapter = new ChatAdapter(new ArrayList<>(), currentUserId, currentUserRole, message -> {
            showReplyPreview(message);
        });
        binding.rvChatMessages.setLayoutManager(new LinearLayoutManager(this));
        binding.rvChatMessages.setAdapter(adapter);

        binding.btnSendChat.setOnClickListener(v -> sendMessage());
        binding.btnCancelReply.setOnClickListener(v -> hideReplyPreview());
        binding.btnAttachImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        binding.etChatMessage.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateSendButtonState();
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

        // Apply Role-Based Theme to Header
        boolean isBarangay = "BARANGAY".equals(currentUserRole);
        int primaryColor = getResources().getColor(isBarangay ? R.color.barangay_primary : R.color.primary);
        int darkColor = getResources().getColor(isBarangay ? R.color.barangay_primary_dark : R.color.primary_dark);
        
        binding.tvChatTitle.setTextColor(primaryColor);
        binding.tvChatSubtitle.setTextColor(darkColor);
        binding.ivChatShield.setColorFilter(primaryColor);
        binding.btnAttachImage.setColorFilter(primaryColor);
        
        // Apply Back Button Tint and Reply UI colors
        binding.chatToolbar.setNavigationIconTint(primaryColor);
        binding.chatToolbar.setNavigationOnClickListener(v -> finish());
        binding.viewReplyIndicator.setBackgroundColor(primaryColor);
        
        // Use primary color for input accents (API 29+)
        binding.etChatMessage.setHighlightColor(primaryColor & 0x60FFFFFF); // ~37% Opacity
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            android.graphics.drawable.Drawable cursorDrawable = new android.graphics.drawable.ColorDrawable(primaryColor);
            binding.etChatMessage.setTextCursorDrawable(cursorDrawable);
            binding.etChatMessage.setTextSelectHandle(cursorDrawable);
            binding.etChatMessage.setTextSelectHandleLeft(cursorDrawable);
            binding.etChatMessage.setTextSelectHandleRight(cursorDrawable);
        }
        
        updateSendButtonState();
    }

    private void updateSendButtonState() {
        String text = binding.etChatMessage.getText().toString().trim();
        boolean isEnabled = !text.isEmpty();
        
        binding.btnSendChat.setEnabled(isEnabled);
        
        int color;
        if (isEnabled) {
            boolean isBarangay = "BARANGAY".equals(currentUserRole);
            color = getResources().getColor(isBarangay ? R.color.barangay_primary : R.color.primary);
        } else {
            color = android.graphics.Color.parseColor("#9CA3AF"); // Gray-400
        }
        
        binding.btnSendChat.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
    }

    private void setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    sendImageMessage(uri);
                }
            }
        );
    }

    private void sendImageMessage(Uri imageUri) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Save image to internal storage
                String fileName = "chat_" + System.currentTimeMillis() + ".jpg";
                File file = new File(getFilesDir(), fileName);
                
                try (InputStream is = getContentResolver().openInputStream(imageUri);
                     FileOutputStream fos = new FileOutputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                    }
                }

                String savedPath = file.getAbsolutePath();
                
                ChatMessage msg = new ChatMessage(
                    endorsementId,
                    currentUserId,
                    otherUserId,
                    "", // Empty text for image-only messages
                    System.currentTimeMillis(),
                    false
                );
                msg.conversationRole = conversationRole;
                msg.senderRole = currentUserRole;
                msg.imagePath = savedPath;
                
                db.chatMessageDao().insert(msg);
                
                // Notification for image
                db.notificationDao().insert(new com.example.instacare.data.local.Notification(
                    "New Image",
                    "A photo was sent in the chat",
                    System.currentTimeMillis(),
                    false,
                    "CHAT_" + (currentUserRole.equals("USER") ? "TO_BARANGAY" : "TO_USER"),
                    endorsementId,
                    conversationRole,
                    otherUserId
                ));

                loadMessages();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Failed to send image", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showReplyPreview(ChatMessage msg) {
        replyingTo = msg;
        binding.layoutReplyingToPreview.setVisibility(View.VISIBLE);
        
        String previewText = msg.message != null && !msg.message.isEmpty() ? msg.message : "";
        SpannableString spannable = new SpannableString(" | " + previewText);
        
        boolean isBarangay = "BARANGAY".equals(currentUserRole);
        int color = getResources().getColor(isBarangay ? R.color.barangay_primary : R.color.primary);
        spannable.setSpan(new ForegroundColorSpan(color), 0, 3, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        binding.tvReplyToText.setText(spannable);

        // Handle Image Preview
        if (msg.imagePath != null && !msg.imagePath.isEmpty()) {
            binding.ivReplyPreviewImage.setVisibility(View.VISIBLE);
            com.bumptech.glide.Glide.with(this)
                .load(msg.imagePath)
                .centerCrop()
                .into(binding.ivReplyPreviewImage);
        } else {
            binding.ivReplyPreviewImage.setVisibility(View.GONE);
        }
        
        // Focus input
        binding.etChatMessage.requestFocus();
    }

    private void hideReplyPreview() {
        replyingTo = null;
        binding.layoutReplyingToPreview.setVisibility(View.GONE);
    }

    private void loadMessagesAndAutoWelcome() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<ChatMessage> messages = db.chatMessageDao().getMessagesForEndorsement(endorsementId, conversationRole);
            
            if (messages.isEmpty()) {
                // Fix: Always use generic "Barangay Support" for auto-welcome messages
                // so the ChatAdapter can correctly hide it for Barangay staff.
                String senderName = "Barangay Support";
                String welcomeMsg = "Maayong adlaw! This is the Barangay Staff. Thank you for your endorsement request. We are currently reviewing your details. Feel free to message us here if you have any questions. We are happy to assist you!";
                
                ChatMessage autoMsg = new ChatMessage(
                    endorsementId,
                    0, // System/Barangay ID
                    currentUserId,
                    welcomeMsg,
                    System.currentTimeMillis(),
                    false
                );
                autoMsg.conversationRole = "BARANGAY";
                autoMsg.senderRole = "BARANGAY";
                
                db.chatMessageDao().insert(autoMsg);

                // Notify the USER when this happens
                db.notificationDao().insert(new com.example.instacare.data.local.Notification(
                    "Barangay Support",
                    "The Barangay Staff has joined the conversation.",
                    System.currentTimeMillis(),
                    false,
                    "CHAT",
                    endorsementId,
                    "BARANGAY",
                    currentUserId
                ));

                messages = db.chatMessageDao().getMessagesForEndorsement(endorsementId, conversationRole);
            }

            db.chatMessageDao().markAsRead(endorsementId, currentUserId, conversationRole);
            
            List<ChatMessage> finalMessages = messages;
            runOnUiThread(() -> {
                adapter.updateMessages(finalMessages);
                if (!finalMessages.isEmpty()) {
                    binding.rvChatMessages.smoothScrollToPosition(finalMessages.size() - 1);
                }
                
                // Simulate typing before showing the welcome message if it was just created AND NOT SHOWN YET
                if (finalMessages.size() == 1 && finalMessages.get(0).senderId == 0) {
                    if (!shownTypingEndorsements.contains(endorsementId)) {
                        shownTypingEndorsements.add(endorsementId);
                        
                        ChatMessage welcome = finalMessages.get(0);
                        finalMessages.clear();
                        adapter.notifyDataSetChanged();
                        
                        showTypingIndicator();
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            hideTypingIndicator();
                            finalMessages.add(welcome);
                            adapter.updateMessages(finalMessages);
                        }, 1500);
                    }
                }
            });
        });
    }

    private void loadMessages() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<ChatMessage> messages = db.chatMessageDao().getMessagesForEndorsement(endorsementId, conversationRole);
            db.chatMessageDao().markAsRead(endorsementId, currentUserId, conversationRole);
            
            runOnUiThread(() -> {
                adapter.updateMessages(messages);
                if (!messages.isEmpty()) {
                    binding.rvChatMessages.smoothScrollToPosition(messages.size() - 1);
                }
            });
        });
    }

    private void sendMessage() {
        String text = binding.etChatMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        binding.etChatMessage.setText("");
        
        ChatMessage finalReplyTo = replyingTo;
        hideReplyPreview();

        Executors.newSingleThreadExecutor().execute(() -> {
            ChatMessage msg = new ChatMessage(
                endorsementId,
                currentUserId,
                otherUserId,
                text,
                System.currentTimeMillis(),
                false
            );
            msg.conversationRole = conversationRole;
            msg.senderRole = currentUserRole;
            
            if (finalReplyTo != null) {
                msg.replyToId = finalReplyTo.id;
                msg.replyToMessage = finalReplyTo.message;
                msg.replyToSenderId = finalReplyTo.senderId;
                msg.replyToImagePath = finalReplyTo.imagePath;
            }

            db.chatMessageDao().insert(msg);
            
            // Create a System Notification for the receiver
            SessionManager sessionManager = SessionManager.getInstance(this);
            db.notificationDao().insert(new com.example.instacare.data.local.Notification(
                "New Message",
                text.length() > 30 ? text.substring(0, 27) + "..." : text,
                System.currentTimeMillis(),
                false,
                "CHAT_" + (currentUserRole.equals("USER") ? "TO_BARANGAY" : "TO_USER"),
                endorsementId,
                conversationRole,
                otherUserId
            ));

            loadMessages(); // Refresh UI
        });
    }

    private List<ChatMessage> chatList = new ArrayList<>();

    private void showTypingIndicator() {
        runOnUiThread(() -> {
            ChatMessage typing = new ChatMessage(endorsementId, 0, currentUserId, "", 0, true);
            typing.isTyping = true;
            // Get current list from adapter or create new
            chatList = new ArrayList<>(adapter.getMessages());
            chatList.add(typing);
            adapter.updateMessages(chatList);
            binding.rvChatMessages.smoothScrollToPosition(chatList.size() - 1);
        });
    }

    private void hideTypingIndicator() {
        runOnUiThread(() -> {
            chatList = new ArrayList<>(adapter.getMessages());
            for (int i = chatList.size() - 1; i >= 0; i--) {
                if (chatList.get(i).isTyping) {
                    chatList.remove(i);
                }
            }
            adapter.updateMessages(chatList);
        });
    }
}
