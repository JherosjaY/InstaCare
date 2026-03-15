package com.example.instacare.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "endorsements")
public class Endorsement {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String username;
    public String patientName;
    public String address;
    public String purpose;       // "Medical Assistance", "Hospital Referral", "Lab/Check-up", "Other"
    public String hospitalName;
    public String reason;
    public String status;        // "PENDING", "APPROVED", "DECLINED"
    public String adminRemarks;  // nullable — reason for decline
    public long createdAt;
    public long updatedAt;

    public Endorsement(String username, String patientName, String address,
                       String purpose, String hospitalName, String reason,
                       String status, long createdAt, long updatedAt) {
        this.username = username;
        this.patientName = patientName;
        this.address = address;
        this.purpose = purpose;
        this.hospitalName = hospitalName;
        this.reason = reason;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
