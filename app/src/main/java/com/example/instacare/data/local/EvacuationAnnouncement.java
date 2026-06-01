package com.example.instacare.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Tier 2 — Feature C: Evacuation Announcement Board
 * Admin posts center-specific announcements visible to users.
 */
@Entity(tableName = "evacuation_announcements")
public class EvacuationAnnouncement {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String centerId;     // FK → evacuation_centers.id (null = broadcast to all)
    public String centerName;   // Denormalized display name
    public String title;        // Short headline
    public String message;      // Full announcement body
    public String adminId;      // Who posted it
    public long timestamp;      // epoch millis
    public int isActive;        // 1 = visible, 0 = archived
    public String priority;     // "INFO","WARNING","URGENT"

    public EvacuationAnnouncement(String centerId, String centerName, String title,
                                   String message, String adminId, long timestamp,
                                   int isActive, String priority) {
        this.centerId   = centerId;
        this.centerName = centerName;
        this.title      = title;
        this.message    = message;
        this.adminId    = adminId;
        this.timestamp  = timestamp;
        this.isActive   = isActive;
        this.priority   = priority;
    }
}
