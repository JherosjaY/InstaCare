package com.example.instacare.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "barangay_endorsements")
public class Endorsement {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int userId;
    public String patientName;
    public String address;
    public String purpose;       // "Medical Assistance", "Hospital Referral", "Lab/Check-up", "Other"
    public String hospitalName;
    public String reason;
    public String status;        // "PENDING", "REVIEWED", "FORWARDED", "RESOLVED"
    public String adminRemarks;  // nullable — reason for decline
    public String caseRef;       // e.g., "IC-2025-XXXX"
    public String barangayZone;  // Matched zone
    public int alertId;          // Link to emergency_alerts table
    public long createdAt;
    public long updatedAt;

    public Endorsement(int userId, String patientName, String address,
                       String purpose, String hospitalName, String reason,
                       String status, String caseRef, String barangayZone, int alertId,
                       long createdAt, long updatedAt) {
        this.userId = userId;
        this.patientName = patientName;
        this.address = address;
        this.purpose = purpose;
        this.hospitalName = hospitalName;
        this.reason = reason;
        this.status = status;
        this.caseRef = caseRef;
        this.barangayZone = barangayZone;
        this.alertId = alertId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
