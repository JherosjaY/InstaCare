package com.example.instacare.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "favorite_centers")
public class FavoriteCenter {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String centerName;
    public double latitude;
    public double longitude;
    public long timestamp;

    public FavoriteCenter(String centerName, double latitude, double longitude) {
        this.centerName = centerName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = System.currentTimeMillis();
    }
}
