package com.example.instacare.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "evacuation_centers")
public class EvacuationCenter {
    @PrimaryKey
    @NonNull
    public String id;

    public String name;
    public String address;
    public double latitude;
    public double longitude;
    public String type;          // "School", "Gym", "Community Hall", "Church"
    public String status;        // "Open", "Full", "Closed"
    public int capacity;         // Max evacuees
    public int occupied;         // Current evacuee count
    public String amenities;     // Amenities description
    public long lastUpdated;     // Timestamp of last update
    public String disasterTypes; // Comma-separated: "flood,earthquake,fire,typhoon"
    public String contact;       // Contact number
    public String barangay;      // Barangay location
    public String centerType;    // "school", "gym", "church", "covered_court"
    public int isActive;         // 1 = active, 0 = inactive
    public String distance;      // Calculated display string e.g. "3.2 km"
    public String source;        // "local" = admin-entered, "api" = Overpass-fetched
    public String photoPath;     // Internal file path to uploaded photo

    public EvacuationCenter(@NonNull String id, String name, String address,
                            double latitude, double longitude, String type, String status) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.type = type;
        this.status = status;
        this.capacity = 0;
        this.occupied = 0;
        this.amenities = "";
        this.lastUpdated = System.currentTimeMillis();
        this.disasterTypes = "";
        this.contact = "";
        this.barangay = "";
        this.centerType = type;
        this.isActive = 1;
        this.distance = "";
        this.source = "local";
        this.photoPath = "";
    }

    @Ignore
    public EvacuationCenter(@NonNull String id, String name, String address,
                            double latitude, double longitude, String type, String status,
                            int capacity, int occupied, String amenities, long lastUpdated,
                            String disasterTypes, String contact,
                            String barangay, String centerType, int isActive,
                            String distance, String source) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.type = type;
        this.status = status;
        this.capacity = capacity;
        this.occupied = occupied;
        this.amenities = amenities;
        this.lastUpdated = lastUpdated;
        this.disasterTypes = disasterTypes;
        this.contact = contact;
        this.barangay = barangay;
        this.centerType = centerType;
        this.isActive = isActive;
        this.distance = distance;
        this.source = source;
        this.photoPath = "";
    }
}
