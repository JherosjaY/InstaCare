package com.example.instacare.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "hospitals")
public class Hospital {
    @PrimaryKey
    @NonNull
    public String id;
    public String name;
    public String distance; // e.g., "0.8 miles"
    public String address;
    public String phone;
    public boolean isOpen;
    public String type; // "Hospital" or "Clinic"
    public String servicesJson; // e.g., "ER, Trauma"
    public double latitude; // For Map
    public double longitude; // For Map
    public String imageUrl; // Hospital photo URL
    public String capacityStatus; // e.g., "Available", "Full", "Moderate"

    public Hospital(@NonNull String id, String name, String distance, String address, String phone,
                    boolean isOpen, String type, String servicesJson, double latitude, double longitude, String imageUrl, String capacityStatus) {
        this.id = id;
        this.name = name;
        this.distance = distance;
        this.address = address;
        this.phone = phone;
        this.isOpen = isOpen;
        this.type = type;
        this.servicesJson = servicesJson;
        this.latitude = latitude;
        this.longitude = longitude;
        this.imageUrl = imageUrl;
        this.capacityStatus = capacityStatus;
    }
}
