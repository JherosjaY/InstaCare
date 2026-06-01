package com.example.instacare;

public class NewsItem {
    private String category;
    private String title;
    private String summary;
    private String timeAgo;
    private int iconResId;

    private String url;
    private String imageUrl;
    private String fullContent;

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
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getFullContent() { return fullContent; }
    public void setFullContent(String fullContent) { this.fullContent = fullContent; }
}
