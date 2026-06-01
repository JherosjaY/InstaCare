package com.example.instacare;

public class BotMessage {
    private String text;
    private boolean isBot;
    private long timestamp;

    public BotMessage(String text, boolean isBot) {
        this.text = text;
        this.isBot = isBot;
        this.timestamp = System.currentTimeMillis();
    }

    public String getText() { return text; }
    public boolean isBot() { return isBot; }
    public long getTimestamp() { return timestamp; }
}
