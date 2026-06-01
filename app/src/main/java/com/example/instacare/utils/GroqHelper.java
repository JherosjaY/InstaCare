package com.example.instacare.utils;

import android.os.Handler;
import android.os.Looper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class GroqHelper {
    private static final String BASE_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";
    
    private final String apiKey;
    private final OkHttpClient client;
    private final Gson gson;
    private final Handler mainHandler;

    public GroqHelper(String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public interface GroqResponseCallback {
        void onResponse(String response);
        void onError(String message);
    }

    public void generateResponse(String systemPrompt, String userMessage, GroqResponseCallback callback) {
        java.util.List<com.example.instacare.data.local.AssistantMessage> history = new java.util.ArrayList<>();
                history.add(new com.example.instacare.data.local.AssistantMessage(0, userMessage, false, 0, System.currentTimeMillis()));
        generateChatResponse(systemPrompt, history, callback);
    }

    public void generateChatResponse(String systemPrompt, java.util.List<com.example.instacare.data.local.AssistantMessage> history, GroqResponseCallback callback) {
        JsonObject jsonBody = new JsonObject();
        jsonBody.addProperty("model", MODEL);
        
        JsonArray messages = new JsonArray();
        
        // System prompt context
        JsonObject systemMsg = new JsonObject();
        systemMsg.addProperty("role", "system");
        systemMsg.addProperty("content", systemPrompt);
        messages.add(systemMsg);
        
        // Contextual History (Last 10 text messages)
        int start = Math.max(0, history.size() - 10);
        for (int i = start; i < history.size(); i++) {
            com.example.instacare.data.local.AssistantMessage msg = history.get(i);
            // Only include text messages in AI context, skip action cards/typing indicators
            if (msg.type != 0 && msg.type != 2) continue; 

            JsonObject chatMsg = new JsonObject();
            chatMsg.addProperty("role", msg.isBot ? "assistant" : "user");
            chatMsg.addProperty("content", msg.text);
            messages.add(chatMsg);
        }
        
        jsonBody.add("messages", messages);
        jsonBody.addProperty("temperature", 0.7);

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(BASE_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    String error = response.body() != null ? response.body().string() : "Unknown error";
                    mainHandler.post(() -> callback.onError("API error: " + error));
                    return;
                }

                try {
                    String responseData = response.body().string();
                    JsonObject root = gson.fromJson(responseData, JsonObject.class);
                    JsonArray choices = root.getAsJsonArray("choices");
                    if (choices.size() > 0) {
                        String content = choices.get(0).getAsJsonObject()
                                .getAsJsonObject("message")
                                .get("content").getAsString();
                        mainHandler.post(() -> callback.onResponse(content));
                    } else {
                        mainHandler.post(() -> callback.onError("Empty response from AI"));
                    }
                } catch (Exception e) {
                    mainHandler.post(() -> callback.onError("Parsing error: " + e.getMessage()));
                }
            }
        });
    }
}
