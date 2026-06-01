package com.example.instacare.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Tier 2 — Feature A: Evacuation Supply/Resource Tracker
 * Stores supply inventory per evacuation center.
 */
@Entity(tableName = "evacuation_resources")
public class EvacuationResource {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String centerId;       // FK → evacuation_centers.id
    public String resourceType;   // "Food Packs","Water","Blankets","Medicine Kit","Sleeping Area","Others"
    public int quantity;          // Current stock count
    public String unit;           // "pcs", "liters", "sets", "beds"
    public long lastUpdated;      // epoch millis
    public int isAvailable;       // 1 = available, 0 = depleted

    public EvacuationResource(String centerId, String resourceType,
                               int quantity, String unit, long lastUpdated, int isAvailable) {
        this.centerId     = centerId;
        this.resourceType = resourceType;
        this.quantity     = quantity;
        this.unit         = unit;
        this.lastUpdated  = lastUpdated;
        this.isAvailable  = isAvailable;
    }
}
