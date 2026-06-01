package com.example.instacare.utils;

import android.app.Dialog;
import android.os.Build;
import android.view.Window;
import android.view.WindowManager;

/**
 * Utility to apply Glassmorphism Blur effect to Dialogs.
 * Native blur is supported on Android 12 (API 31) and above.
 */
public class BlurUtils {

    public static void applyBlur(Dialog dialog, int radius) {
        if (dialog == null) return;
        
        Window window = dialog.getWindow();
        if (window == null) return;

        // We always dim for better contrast, and blur where supported (Android 12+)
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        window.setDimAmount(0.60f); // Enhanced deep dim for better focus

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
            window.getAttributes().setBlurBehindRadius(radius);
        }
    }

    public static void applyBlur(Dialog dialog) {
        applyBlur(dialog, 65); // High-fidelity deep blur radius
    }

    /**
     * Blurs the entire Activity content. 
     * Use this before launching system activities (Photo Picker, UCrop) 
     * to maintain the glassmorphism aesthetic behind the system UI.
     */
    public static void blurActivityRoot(android.app.Activity activity, float radius) {
        if (activity == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;
        
        android.graphics.RenderEffect blur = android.graphics.RenderEffect.createBlurEffect(
            radius, radius, android.graphics.Shader.TileMode.CLAMP
        );
        activity.getWindow().getDecorView().setRenderEffect(blur);
    }

    /**
     * Removes the Activity-level blur.
     */
    public static void unblurActivityRoot(android.app.Activity activity) {
        if (activity == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return;
        activity.getWindow().getDecorView().setRenderEffect(null);
    }
}
