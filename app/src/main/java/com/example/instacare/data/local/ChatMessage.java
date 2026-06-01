package com.example.instacare.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "chat_messages")
public class ChatMessage {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public int endorsementId; // Linked to a specific request
    public int senderId;
    public int receiverId;
    public String message;
    public long timestamp;
    public boolean isRead;
    
    // Reply functionality
    public Integer replyToId;
    public String replyToMessage;
    public int replyToSenderId;
    public String replyToImagePath;
    
    // Role separation & optimization
    public String conversationRole; // "ADMIN" or "BARANGAY"
    public String senderRole;       // "ADMIN", "BARANGAY", or "USER"
    public String imagePath;        // Path to shared image

    @androidx.room.Ignore
    public boolean isTyping;

    public ChatMessage(int endorsementId, int senderId, int receiverId, String message, long timestamp, boolean isRead) {
        this.endorsementId = endorsementId;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.message = message;
        this.timestamp = timestamp;
        this.isRead = isRead;
    }
}
