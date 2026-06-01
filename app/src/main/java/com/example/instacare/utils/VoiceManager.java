package com.example.instacare.utils;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

/**
 * Singleton VoiceManager to warm up the TextToSpeech engine early.
 * This prevents the "First Speak Delay" when Cara starts talking in the tutorial.
 */
public class VoiceManager implements TextToSpeech.OnInitListener {
    private static VoiceManager instance;
    private TextToSpeech tts;
    private boolean isReady = false;

    private VoiceManager(Context context) {
        // Use Application context to avoid leaks
        tts = new TextToSpeech(context.getApplicationContext(), this);
    }

    public static synchronized VoiceManager getInstance(Context context) {
        if (instance == null) {
            instance = new VoiceManager(context);
        }
        return instance;
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.US);
            tts.setPitch(1.0f);
            tts.setSpeechRate(0.95f);
            isReady = true;
        }
    }

    public TextToSpeech getTts() {
        return tts;
    }

    public boolean isReady() {
        return isReady;
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        instance = null;
    }
}
