package com.example.instacare.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.example.instacare.NewsItem;
import com.example.instacare.R;

public class NewsHelper {
    private static final String API_KEY = com.example.instacare.BuildConfig.CURRENTS_API_KEY;
    private static final String BASE_URL = "https://api.currentsapi.services/v1/latest-news";
    
    private final OkHttpClient client;
    private final Gson gson;
    private final Handler mainHandler;

    public NewsHelper() {
        this.client = new OkHttpClient();
        this.gson = new Gson();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public interface NewsCallback {
        void onSuccess(List<NewsItem> newsList);
        void onError(String error);
    }

    public void fetchLatestNews(NewsCallback callback) {
        // Query params: language=en, country=PH (optional), apiKey
        HttpUrl url = HttpUrl.parse(BASE_URL).newBuilder()
                .addQueryParameter("language", "en")
                .addQueryParameter("country", "PH")
                .addQueryParameter("apiKey", API_KEY)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Network failure: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> callback.onError("Server error: " + response.code()));
                    return;
                }

                try {
                    String jsonData = response.body().string();
                    JsonObject root = gson.fromJson(jsonData, JsonObject.class);
                    JsonArray newsArray = root.getAsJsonArray("news");
                    
                    List<NewsItem> items = new ArrayList<>();
                    if (newsArray != null) {
                        for (int i = 0; i < newsArray.size(); i++) {
                            JsonObject obj = newsArray.get(i).getAsJsonObject();
                            
                            String title = obj.has("title") ? obj.get("title").getAsString() : "No Title";
                            String description = obj.has("description") ? obj.get("description").getAsString() : "No Content";
                            String category = "News"; 
                            JsonArray categories = obj.getAsJsonArray("category");
                            if (categories != null && categories.size() > 0) {
                                category = categories.get(0).getAsString();
                                // Capitalize first letter
                                category = category.substring(0,1).toUpperCase() + category.substring(1);
                            }
                            
                            String published = obj.has("published") ? obj.get("published").getAsString() : "";
                            String friendlyTime = formatPublishedDate(published);
                            String articleUrl = obj.has("url") ? obj.get("url").getAsString() : "";
                            String imgUrl = obj.has("image") && !obj.get("image").isJsonNull() ? obj.get("image").getAsString() : "none";

                            // Icon logic based on category
                            int iconRes = R.drawable.ic_file_text;
                            if (category.toLowerCase().contains("health")) iconRes = R.drawable.ic_hospital;
                            else if (category.toLowerCase().contains("world")) iconRes = R.drawable.ic_map_pin;

                            NewsItem item = new NewsItem(category, title, description, friendlyTime, iconRes);
                            item.setUrl(articleUrl);
                            item.setImageUrl(imgUrl);
                            item.setFullContent(description); // In Currents API, description contains the snippet/body
                            items.add(item);
                        }
                    }
                    mainHandler.post(() -> callback.onSuccess(items));
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError("Parsing error: " + e.getMessage()));
                }
            }
        });
    }

    private String formatPublishedDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "Just now";
        try {
            // Currents API usually returns: 2024-03-12 10:30:00 +0000
            // For now, let's keep it simple or return the date part
            if (dateStr.length() > 10) return dateStr.substring(0, 10);
            return dateStr;
        } catch (Exception e) {
            return "Recently";
        }
    }
}
