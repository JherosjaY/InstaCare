package com.example.instacare.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Tier 2 — Feature B: Evacuee Check-In System
 * Records when a user checks in at an evacuation center.
 */
@Entity(tableName = "evacuee_checkins")
public class EvacueeCheckIn {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int userId;            // FK → users.id
    public String centerId;       // FK → evacuation_centers.id
    public String centerName;     // Denormalized for quick display
    public long checkInTime;      // epoch millis
    public long checkOutTime;     // 0 = still checked in
    public int isActive;          // 1 = checked in, 0 = checked out / expired
    public String disasterType;   // "flood","earthquake","fire","typhoon","other"

    public EvacueeCheckIn(int userId, String centerId, String centerName,
                           long checkInTime, int isActive, String disasterType) {
        this.userId      = userId;
        this.centerId    = centerId;
        this.centerName  = centerName;
        this.checkInTime = checkInTime;
        this.checkOutTime = 0;
        this.isActive    = isActive;
        this.disasterType = disasterType;
    }
}
