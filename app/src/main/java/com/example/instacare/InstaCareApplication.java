package com.example.instacare;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

public class InstaCareApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Load and Apply Theme immediately on app start
        android.content.SharedPreferences prefs = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        int savedTheme = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_NO);
        AppCompatDelegate.setDefaultNightMode(savedTheme);

        NotificationHelper.createNotificationChannel(this);
    }
}
