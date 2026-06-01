package com.example.instacare.utils;

import java.util.ArrayList;
import java.util.List;
import com.example.instacare.NewsItem;

public class NewsRepository {
    private static NewsRepository instance;
    private List<NewsItem> latestNews = new ArrayList<>();

    private NewsRepository() {}

    public static synchronized NewsRepository getInstance() {
        if (instance == null) {
            instance = new NewsRepository();
        }
        return instance;
    }

    public void setLatestNews(List<NewsItem> news) {
        this.latestNews = news;
    }

    public List<NewsItem> getLatestNews() {
        return latestNews;
    }
    
    public String getNewsContextForAI() {
        if (latestNews.isEmpty()) return "No recent news updates currently available.";
        
        StringBuilder sb = new StringBuilder("Here are the latest news alerts from IG (Internal Knowledge):\n");
        // Only give the top 5 to keep prompt size reasonable
        int limit = Math.min(5, latestNews.size());
        for (int i = 0; i < limit; i++) {
            NewsItem item = latestNews.get(i);
            sb.append("- [").append(item.getCategory()).append("] ").append(item.getTitle()).append(": ").append(item.getSummary()).append("\n");
        }
        return sb.toString();
    }
}
