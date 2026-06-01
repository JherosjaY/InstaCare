package com.example.instacare.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "alert_notifications")
public class AlertNotification {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int alertId;
    public String contactName;
    public String status; // "SENT", "FAILED"
    public long timestamp;

    public AlertNotification(int alertId, String contactName, String status, long timestamp) {
        this.alertId = alertId;
        this.contactName = contactName;
        this.status = status;
        this.timestamp = timestamp;
    }
}
