package com.example.instacare.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notifications")
public class Notification {
    
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private String title;
    private String message;
    private long timestamp;
    private boolean isRead;
    private String type;
    private int referenceId;
    private String referenceRole;
    private int userId; // Associated user (0 for system-wide/broadcast)

    public Notification(String title, String message, long timestamp, boolean isRead, String type, int referenceId, String referenceRole, int userId) {
        this.title = title;
        this.message = message;
        this.timestamp = timestamp;
        this.isRead = isRead;
        this.type = type;
        this.referenceId = referenceId;
        this.referenceRole = referenceRole;
        this.userId = userId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getReferenceId() { return referenceId; }
    public void setReferenceId(int referenceId) { this.referenceId = referenceId; }

    public String getReferenceRole() { return referenceRole; }
    public void setReferenceRole(String referenceRole) { this.referenceRole = referenceRole; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
}
