package com.example.instacare;

public class NewsItem {
    private String category;
    private String title;
    private String summary;
    private String timeAgo;
    private int iconResId;

    public NewsItem(String category, String title, String summary, String timeAgo, int iconResId) {
        this.category = category;
        this.title = title;
        this.summary = summary;
        this.timeAgo = timeAgo;
        this.iconResId = iconResId;
    }

    public String getCategory() { return category; }
    public String getTitle() { return title; }
    public String getSummary() { return summary; }
    public String getTimeAgo() { return timeAgo; }
    public int getIconResId() { return iconResId; }
}
