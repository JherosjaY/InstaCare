package com.example.instacare.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "endorsement_status_log")
public class EndorsementStatusLog {
    @PrimaryKey(autoGenerate = true)
    public int logId;

    public int endorsementId;
    public String oldStatus;
    public String newStatus;
    public String changedBy;
    public long timestamp;

    public EndorsementStatusLog(int endorsementId, String oldStatus, String newStatus, String changedBy, long timestamp) {
        this.endorsementId = endorsementId;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
        this.timestamp = timestamp;
    }
}
