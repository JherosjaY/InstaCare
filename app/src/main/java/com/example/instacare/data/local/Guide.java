package com.example.instacare.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "guides")
public class Guide {
    @PrimaryKey
    @NonNull
    public String id; // Use String ID from React data
    public String title;
    public String category;
    public String description;
    public String difficulty;
    public String duration; // e.g., "5 mins"
    public int views;
    public String videoUrl;
    public String stepsJson;
    public boolean isBookmarked;

    // Constructor
    public Guide(@NonNull String id, String title, String category, String description,
                 String difficulty, String duration, int views, String videoUrl, String stepsJson, boolean isBookmarked) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.description = description;
        this.difficulty = difficulty;
        this.duration = duration;
        this.views = views;
        this.videoUrl = videoUrl;
        this.stepsJson = stepsJson;
        this.isBookmarked = isBookmarked;
    }
}
