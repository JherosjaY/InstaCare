package com.example.instacare;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.example.instacare.data.local.ActivityLog;
import com.example.instacare.data.local.AppDatabase;

/**
 * Shared utility to render categorized emergency hotlines
 * into a LinearLayout container. Used by SOSActivity and GuideDetailActivity.
 */
public class HotlineHelper {

    public interface OnHotlineClickListener {
        void onCall(String number);
    }

    // Categories with their hotlines: { name, number, category, iconResName }
    private static final String[][] HOTLINES = {
        // Police
        {"Valencia CPS", "0917-718-9191", "Police", "ic_shield"},
        {"Police (alternate)", "0998-967-4022", "Police", "ic_shield"},
        // Fire
        {"Valencia BFP", "0926-190-3020", "Fire Department", "ic_siren"},
        // Disaster Response
        {"DRRMO – Valencia", "0956-135-2663", "Disaster Response", "ic_alert_triangle"},
        {"Task Force – Valencia", "0936-442-0788", "Disaster Response", "ic_alert_triangle"},
        // Medical
        {"City Health Office", "(088) 222-2489", "Medical", "ic_heart_pulse"},
        {"Nearby Hospital (ER)", "(088) 844-2660", "Medical", "ic_heart_pulse"},
    };

    // Category order
    private static final String[] CATEGORIES = {"Police", "Fire Department", "Disaster Response", "Medical"};

    // Category colors (light-mode-safe tints)
    private static int getCategoryColor(Context ctx, String category) {
        switch (category) {
            case "Police":            return 0xFF3B82F6; // Blue-500
            case "Fire Department":   return 0xFFF97316; // Orange-500
            case "Disaster Response": return 0xFFEAB308; // Yellow-500
            case "Medical":           return 0xFFEF4444; // Red-500
            default:                  return 0xFF6B7280; // Gray-500
        }
    }

    private static int getCategoryBgColor(Context ctx, String category) {
        switch (category) {
            case "Police":            return 0x1A3B82F6; // Blue-500 10%
            case "Fire Department":   return 0x1AF97316; // Orange-500 10%
            case "Disaster Response": return 0x1AEAB308; // Yellow-500 10%
            case "Medical":           return 0x1AEF4444; // Red-500 10%
            default:                  return 0x1A6B7280;
        }
    }

    public static void populateCategorized(Context ctx, LinearLayout container, OnHotlineClickListener listener) {
        container.removeAllViews();

        float density = ctx.getResources().getDisplayMetrics().density;

        for (String category : CATEGORIES) {
            // Section Header
            TextView header = new TextView(ctx);
            header.setText(category);
            header.setTextSize(14);
            header.setTypeface(null, Typeface.BOLD);
            header.setTextColor(getCategoryColor(ctx, category));
            LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            headerParams.setMargins(0, (int)(20 * density), 0, (int)(8 * density));
            header.setLayoutParams(headerParams);
            container.addView(header);

            // Hotlines in this category
            for (String[] hotline : HOTLINES) {
                if (!hotline[2].equals(category)) continue;

                MaterialCardView card = new MaterialCardView(ctx);
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                cardParams.setMargins(0, 0, 0, (int)(10 * density));
                card.setLayoutParams(cardParams);
                card.setRadius(16 * density);
                card.setCardElevation(0);
                card.setCardBackgroundColor(ContextCompat.getColor(ctx, R.color.sos_card_bg));
                card.setStrokeColor(ContextCompat.getColor(ctx, R.color.sos_card_stroke));
                card.setStrokeWidth((int)(1 * density));
                card.setClickable(true);
                card.setFocusable(true);

                LinearLayout row = new LinearLayout(ctx);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding((int)(16 * density), (int)(14 * density), (int)(16 * density), (int)(14 * density));

                // Category-colored icon circle
                android.widget.FrameLayout iconFrame = new android.widget.FrameLayout(ctx);
                LinearLayout.LayoutParams iconFrameParams = new LinearLayout.LayoutParams(
                        (int)(42 * density), (int)(42 * density));
                iconFrameParams.setMarginEnd((int)(14 * density));
                iconFrame.setLayoutParams(iconFrameParams);

                android.graphics.drawable.GradientDrawable iconBg = new android.graphics.drawable.GradientDrawable();
                iconBg.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                iconBg.setColor(getCategoryBgColor(ctx, category));
                iconFrame.setBackground(iconBg);

                ImageView icon = new ImageView(ctx);
                int iconResId = ctx.getResources().getIdentifier(hotline[3], "drawable", ctx.getPackageName());
                if (iconResId != 0) icon.setImageResource(iconResId);
                else icon.setImageResource(R.drawable.ic_phone);
                icon.setColorFilter(getCategoryColor(ctx, category));
                android.widget.FrameLayout.LayoutParams iconParams = new android.widget.FrameLayout.LayoutParams(
                        (int)(20 * density), (int)(20 * density), Gravity.CENTER);
                icon.setLayoutParams(iconParams);
                iconFrame.addView(icon);
                row.addView(iconFrame);

                // Text content
                LinearLayout textCol = new LinearLayout(ctx);
                textCol.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
                textCol.setLayoutParams(textParams);

                TextView name = new TextView(ctx);
                name.setText(hotline[0]);
                name.setTextColor(ContextCompat.getColor(ctx, R.color.sos_card_text));
                name.setTextSize(14);
                name.setTypeface(null, Typeface.BOLD);

                TextView number = new TextView(ctx);
                number.setText(hotline[1]);
                number.setTextColor(ContextCompat.getColor(ctx, R.color.sos_subheader));
                number.setTextSize(12);

                textCol.addView(name);
                textCol.addView(number);
                row.addView(textCol);

                // Trailing phone icon
                ImageView callIcon = new ImageView(ctx);
                callIcon.setImageResource(R.drawable.ic_phone);
                callIcon.setColorFilter(getCategoryColor(ctx, category));
                LinearLayout.LayoutParams callParams = new LinearLayout.LayoutParams(
                        (int)(20 * density), (int)(20 * density));
                callIcon.setLayoutParams(callParams);
                row.addView(callIcon);

                card.addView(row);

                card.setOnClickListener(v -> {
                    // Log the Emergency Call
                    String userEmail = ctx.getSharedPreferences("InstaCarePrefs", Context.MODE_PRIVATE)
                            .getString("USER_EMAIL", "Unknown");
                    String desc = "Emergency call to " + hotline[0] + " (" + hotline[1] + ")";
                    
                    new Thread(() -> {
                        ActivityLog log = new ActivityLog("SOS", desc, userEmail, category, System.currentTimeMillis());
                        AppDatabase.getDatabase(ctx).activityLogDao().insert(log);
                    }).start();

                    if (listener != null) {
                        listener.onCall(hotline[1].replaceAll("[^0-9]", ""));
                    }
                });

                container.addView(card);
            }
        }
    }
}
