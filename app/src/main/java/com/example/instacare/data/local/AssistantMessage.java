package com.example.instacare.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "assistant_messages")
public class AssistantMessage {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public int userId;
    public String text;
    public boolean isBot;
    public long timestamp;
    
    // Types: 0 = Text, 1 = Profile Card, 2 = Confirmation Buttons
    public int type;
    public String metadata; // For storing JSON action data if needed

    public AssistantMessage(int userId, String text, boolean isBot, int type, long timestamp) {
        this.userId = userId;
        this.text = text;
        this.isBot = isBot;
        this.type = type;
        this.timestamp = timestamp;
    }
}
