package com.example.instacare.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Tier 3 — Feature E: Evacuation Endorsement System
 * Mirrors Endorsement.java (medical) but for evacuation assistance requests.
 *
 * Workflow: PENDING → ASSIGNED → ARRIVED → RESOLVED
 */
@Entity(tableName = "evacuation_endorsements")
public class EvacuationEndorsement {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public int    userId;
    public String patientName;       // Requester's full name
    public String address;           // Current address / pickup location
    public String disasterType;      // "flood","earthquake","fire","typhoon","other"
    public String specialNeeds;      // "none","elderly","PWD","infant","pregnant","medical"
    public String reason;            // Free-text: why they need evacuation assistance
    public String status;            // "PENDING","ASSIGNED","ARRIVED","RESOLVED"
    public String assignedCenterId;  // FK → evacuation_centers.id (set by admin)
    public String assignedCenterName;// Denormalized display name
    public String adminRemarks;      // Admin's response / instructions
    public String caseRef;           // e.g. "EV-2025-0042"
    public String barangayZone;      // Requester's barangay zone
    public long   createdAt;
    public long   updatedAt;

    public EvacuationEndorsement(int userId, String patientName, String address,
                                  String disasterType, String specialNeeds, String reason,
                                  String status, String caseRef, String barangayZone,
                                  long createdAt, long updatedAt) {
        this.userId       = userId;
        this.patientName  = patientName;
        this.address      = address;
        this.disasterType = disasterType;
        this.specialNeeds = specialNeeds;
        this.reason       = reason;
        this.status       = status;
        this.caseRef      = caseRef;
        this.barangayZone = barangayZone;
        this.createdAt    = createdAt;
        this.updatedAt    = updatedAt;
    }
}
