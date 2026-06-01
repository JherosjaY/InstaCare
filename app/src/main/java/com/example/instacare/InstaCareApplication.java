package com.example.instacare;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

public class InstaCareApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Load and Apply Theme immediately on app start using SessionManager (UID-isolated)
        SessionManager sessionManager = SessionManager.getInstance(this);
        AppCompatDelegate.setDefaultNightMode(sessionManager.getTheme());

        NotificationHelper.createNotificationChannel(this);
    }
}
