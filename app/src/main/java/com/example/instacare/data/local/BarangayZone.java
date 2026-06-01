package com.example.instacare.data.local;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "barangay_zones")
public class BarangayZone {
    @PrimaryKey(autoGenerate = true)
    public int zoneId;

    public String name;
    public String boundaryCoords; // Stored as JSON or simple string representation
    public String accessPin; // 4-digit PIN for barangay isolation

    @Ignore
    public BarangayZone(String name, String boundaryCoords) {
        this.name = name;
        this.boundaryCoords = boundaryCoords;
        this.accessPin = "";
    }

    public BarangayZone(String name, String boundaryCoords, String accessPin) {
        this.name = name;
        this.boundaryCoords = boundaryCoords;
        this.accessPin = accessPin;
    }
}
