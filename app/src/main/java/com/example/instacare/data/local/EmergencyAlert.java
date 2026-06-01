package com.example.instacare.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "emergency_alerts")
public class EmergencyAlert {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int userId;
    public String patientName;
    public String type; // e.g., "Medical", "Fire", "Police"
    public double latitude;
    public double longitude;
    public String barangayZone;
    public String caseRef;
    public long timestamp;
    public String status; // "TRIGGERED", "HANDLED"

    public EmergencyAlert(int userId, String patientName, String type, 
                          double latitude, double longitude, String barangayZone, 
                          String caseRef, long timestamp, String status) {
        this.userId = userId;
        this.patientName = patientName;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.barangayZone = barangayZone;
        this.caseRef = caseRef;
        this.timestamp = timestamp;
        this.status = status;
    }
}
