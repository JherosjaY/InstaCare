package com.example.instacare.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Tier 3 — Feature E: Audit log for status changes on EvacuationEndorsement.
 * Mirrors EndorsementStatusLog.java exactly.
 */
@Entity(tableName = "evacuation_endorsement_status_log")
public class EvacuationEndorsementStatusLog {
    @PrimaryKey(autoGenerate = true)
    public int logId;

    public int    endorsementId;
    public String oldStatus;
    public String newStatus;
    public String changedBy;   // admin username or "system"
    public long   timestamp;

    public EvacuationEndorsementStatusLog(int endorsementId, String oldStatus,
                                           String newStatus, String changedBy, long timestamp) {
        this.endorsementId = endorsementId;
        this.oldStatus     = oldStatus;
        this.newStatus     = newStatus;
        this.changedBy     = changedBy;
        this.timestamp     = timestamp;
    }
}
