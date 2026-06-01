package com.example.instacare.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "disaster_guides")
public class DisasterGuide {
    @PrimaryKey
    @NonNull
    public String id; // UUID string
    public String title;
    public String disasterType; // e.g. Flood, Earthquake, Typhoon, Fire, Landslide
    public String description;
    public String difficulty;
    public String duration; 
    public int views;
    public String videoUrl;
    public String stepsJson; // The "Before", "During", "After" phases formatted as JSON
    public boolean isBookmarked;

    // Constructor
    public DisasterGuide(@NonNull String id, String title, String disasterType, String description,
                 String difficulty, String duration, int views, String videoUrl, String stepsJson, boolean isBookmarked) {
        this.id = id;
        this.title = title;
        this.disasterType = disasterType;
        this.description = description;
        this.difficulty = difficulty;
        this.duration = duration;
        this.views = views;
        this.videoUrl = videoUrl;
        this.stepsJson = stepsJson;
        this.isBookmarked = isBookmarked;
    }
}
