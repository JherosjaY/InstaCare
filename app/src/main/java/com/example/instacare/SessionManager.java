package com.example.instacare;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Enhanced SessionManager with UID-based isolation (immutable) 
 * instead of Email-based (volatile/bindable).
 */
public class SessionManager {
    private static final String PREF_NAME = "InstaCarePrefs";
    private static final String KEY_CURRENT_USER_UID = "current_session_uid";
    private static final String KEY_CURRENT_USER_EMAIL_LEGACY = "current_session_email"; // For migration
    private static final String KEY_LAST_LOGGED_IN_USER = "last_logged_in_username";
    
    private static SessionManager instance;
    private final SharedPreferences prefs;
    private final SharedPreferences.Editor editor;

    private SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    // --- Session Control ---

    public void startSession(int uid, String email, String username) {
        editor.putInt(KEY_CURRENT_USER_UID, uid);
        editor.putString(KEY_CURRENT_USER_EMAIL_LEGACY, email); // Keep legacy email for one-time migration
        editor.putString(KEY_LAST_LOGGED_IN_USER, username);
        editor.apply();
    }

    public void endSession() {
        // Reset system theme to Light Mode (default) before ending session
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        });
        
        editor.remove(KEY_CURRENT_USER_UID);
        editor.remove(KEY_CURRENT_USER_EMAIL_LEGACY);
        editor.apply();
    }

    public int getCurrentUserUid() {
        return prefs.getInt(KEY_CURRENT_USER_UID, -1);
    }

    public String getCurrentUserEmail() {
        return prefs.getString(KEY_CURRENT_USER_EMAIL_LEGACY, "");
    }

    public String getLastLoggedInUsername() {
        return prefs.getString(KEY_LAST_LOGGED_IN_USER, "");
    }

    public boolean isLoggedIn() {
        return getCurrentUserUid() != -1;
    }

    // --- Isolated Data Access with Legacy Migration ---

    private String getPartitionedKey(String key) {
        int uid = getCurrentUserUid();
        if (uid == -1) return key;
        return key + "_" + uid;
    }

    private String getLegacyPartitionedKey(String key) {
        String email = getCurrentUserEmail();
        if (email.isEmpty()) return key;
        return key + "_" + email;
    }

    public void putString(String key, String value) {
        editor.putString(getPartitionedKey(key), value);
        editor.apply();
    }

    public String getString(String key, String defValue) {
        String currentKey = getPartitionedKey(key);
        String value = prefs.getString(currentKey, null);
        
        // --- 🚀 LEGACY MIGRATION LOGIC ---
        if (value == null && isLoggedIn()) {
            String legacyKey = getLegacyPartitionedKey(key);
            String legacyValue = prefs.getString(legacyKey, null);
            if (legacyValue != null) {
                // Migrate from Email-indexed to UID-indexed
                putString(key, legacyValue);
                // Optional: editor.remove(legacyKey).apply(); // Cleanup legacy
                return legacyValue;
            }
        }
        
        return value != null ? value : defValue;
    }

    public void putBoolean(String key, boolean value) {
        editor.putBoolean(getPartitionedKey(key), value);
        editor.apply();
    }

    public boolean getBoolean(String key, boolean defValue) {
        String currentKey = getPartitionedKey(key);
        if (!prefs.contains(currentKey) && isLoggedIn()) {
             // 1. Migration check: Email-suffixed (Legacy v1)
             String legacyKey = getLegacyPartitionedKey(key);
             if (prefs.contains(legacyKey)) {
                 boolean legacyVal = prefs.getBoolean(legacyKey, defValue);
                 putBoolean(key, legacyVal);
                 return legacyVal;
             }
             // 2. Migration check: Un-suffixed (Legacy v0)
             if (prefs.contains(key)) {
                 boolean legacyVal = prefs.getBoolean(key, defValue);
                 putBoolean(key, legacyVal);
                 return legacyVal;
             }
        }
        return prefs.getBoolean(currentKey, defValue);
    }

    public void putInt(String key, int value) {
        editor.putInt(getPartitionedKey(key), value);
        editor.apply();
    }

    public int getInt(String key, int defValue) {
        String currentKey = getPartitionedKey(key);
        if (!prefs.contains(currentKey) && isLoggedIn()) {
            String legacyKey = getLegacyPartitionedKey(key);
            if (prefs.contains(legacyKey)) {
                int legacyVal = prefs.getInt(legacyKey, defValue);
                putInt(key, legacyVal);
                return legacyVal;
            }
        }
        return prefs.getInt(currentKey, defValue);
    }
    
    public void putGlobalString(String key, String value) {
        editor.putString(key, value);
        editor.apply();
    }

    public String getGlobalString(String key, String defValue) {
        return prefs.getString(key, defValue);
    }

    public void putGlobalBoolean(String key, boolean value) {
        editor.putBoolean(key, value);
        editor.apply();
    }

    public boolean getGlobalBoolean(String key, boolean defValue) {
        return prefs.getBoolean(key, defValue);
    }

    public void remove(String key) {
        editor.remove(getPartitionedKey(key));
        String legacyKey = getLegacyPartitionedKey(key);
        if (prefs.contains(legacyKey)) {
            editor.remove(legacyKey);
        }
        editor.apply();
    }

    public void clearAllDataForCurrentUser() {
        int uid = getCurrentUserUid();
        String email = getCurrentUserEmail();
        if (uid == -1) return;
        
        // Reset system theme to Light Mode before clearing
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        });

        java.util.Map<String, ?> allEntries = prefs.getAll();
        for (java.util.Map.Entry<String, ?> entry : allEntries.entrySet()) {
            String k = entry.getKey();
            if (k.endsWith("_" + uid) || (!email.isEmpty() && k.endsWith("_" + email))) {
                editor.remove(k);
            }
        }
        editor.apply();
    }

    public void setTheme(int mode) {
        putInt("USER_THEME_MODE", mode);
        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(mode);
        });
    }

    public int getTheme() {
        // Default to Light Mode (MODE_NIGHT_NO) as requested
        return getInt("USER_THEME_MODE", androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
    }
}
