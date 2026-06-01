package com.example.instacare.data.local;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "activity_logs")
public class ActivityLog {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String type; // e.g., "LOGIN", "SOS", "PROFILE_UPDATE"
    public String description;
    public int userId; // Track the user account
    public String category;  // Track category (e.g., Police, Fire)
    public String barangayZone; // Isolated zone
    public long timestamp;

    @Ignore
    public ActivityLog(String type, String description, int userId, String category, long timestamp) {
        this(type, description, userId, category, null, timestamp);
    }

    public ActivityLog(String type, String description, int userId, String category, String barangayZone, long timestamp) {
        this.type = type;
        this.description = description;
        this.userId = userId;
        this.category = category;
        this.barangayZone = barangayZone;
        this.timestamp = timestamp;
    }
}
