package com.example.instacare.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "activity_logs")
public class ActivityLog {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String type; // e.g., "LOGIN", "SOS", "PROFILE_UPDATE"
    public String description;
    public String userEmail; // Track the user account
    public String category;  // Track category (e.g., Police, Fire)
    public long timestamp;

    public ActivityLog(String type, String description, String userEmail, String category, long timestamp) {
        this.type = type;
        this.description = description;
        this.userEmail = userEmail;
        this.category = category;
        this.timestamp = timestamp;
    }
}
