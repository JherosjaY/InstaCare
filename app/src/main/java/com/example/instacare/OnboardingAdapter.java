package com.example.instacare;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder> {

    private List<OnboardingItem> onboardingItems;

    public OnboardingAdapter(List<OnboardingItem> onboardingItems) {
        this.onboardingItems = onboardingItems;
    }

    @NonNull
    @Override
    public OnboardingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new OnboardingViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.item_onboarding_slide, parent, false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull OnboardingViewHolder holder, int position) {
        holder.setData(onboardingItems.get(position));
    }

    @Override
    public int getItemCount() {
        return onboardingItems.size();
    }

    static class OnboardingViewHolder extends RecyclerView.ViewHolder {
        private TextView textTitle;
        private TextView textDescription;
        private ImageView imageOnboarding;

        OnboardingViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.textTitle);
            textDescription = itemView.findViewById(R.id.textDescription);
            imageOnboarding = itemView.findViewById(R.id.imageOnboarding);
        }

        void setData(OnboardingItem item) {
            android.text.Spannable spannable = (android.text.Spannable) android.text.Html.fromHtml(item.getTitle(), android.text.Html.FROM_HTML_MODE_LEGACY);
            
            // Find all red colored spans and upgrade them to premium Blurred Highlights
            android.text.style.ForegroundColorSpan[] colorSpans = spannable.getSpans(0, spannable.length(), android.text.style.ForegroundColorSpan.class);
            for (android.text.style.ForegroundColorSpan colorSpan : colorSpans) {
                int start = spannable.getSpanStart(colorSpan);
                int end = spannable.getSpanEnd(colorSpan);
                
                // Color: #E53935 (InstaCare Red)
                int redColor = 0xFFE53935;
                
                // Add the Premium Blurred Highlight Span
                // This custom span handles Background Blur + Text Color + Drop Shadow and padding
                spannable.setSpan(new BlurredHighlightSpan(
                    0x20E53935, // Very subtle red glow background
                    redColor,    // The actual text color
                    12f,         // Blur radius
                    8f           // Corner radius
                ), start, end, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                
                // Remove the old span to avoid double-drawing/coloring conflicts
                spannable.removeSpan(colorSpan);
            }
            
            textTitle.setText(spannable);
            textDescription.setText(item.getDescription());
            imageOnboarding.setImageResource(item.getImage());
        }
    }

    /**
     * Premium ReplacementSpan that draws a blurred glow background behind text
     */
    static class BlurredHighlightSpan extends android.text.style.ReplacementSpan {
        private final int bgColor;
        private final int textColor;
        private final float blurRadius;
        private final float cornerRadius;

        public BlurredHighlightSpan(int bgColor, int textColor, float blurRadius, float cornerRadius) {
            this.bgColor = bgColor;
            this.textColor = textColor;
            this.blurRadius = blurRadius;
            this.cornerRadius = cornerRadius;
        }

        @Override
        public int getSize(@NonNull android.graphics.Paint paint, CharSequence text, int start, int end, @androidx.annotation.Nullable android.graphics.Paint.FontMetricsInt fm) {
            // Add horizontal padding for the glow
            return Math.round(paint.measureText(text, start, end) + blurRadius * 2);
        }

        @Override
        public void draw(@NonNull android.graphics.Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, @NonNull android.graphics.Paint paint) {
            float width = paint.measureText(text, start, end);
            
            // We use a multi-pass drawing strategy to get a true 'Neon Glow' look
            // without using a separate background box.
            
            // Pass 1: Draw the SOFT GLOW (Ambient Red Aura)
            // Large radius, no offset, same red color
            paint.setShadowLayer(18f, 0f, 0f, textColor);
            paint.setColor(textColor);
            canvas.drawText(text, start, end, x + blurRadius, y, paint);

            // Pass 2: Draw the SHARP DROP SHADOW (Depth)
            // Smaller radius, offset for lift
            paint.setShadowLayer(6f, 3f, 3f, 0x60000000);
            canvas.drawText(text, start, end, x + blurRadius, y, paint);
            
            // Pass 3: Draw the SOLID TEXT (Core)
            // Ensures the text remains sharp and perfectly legible on top of the glows
            paint.clearShadowLayer();
            paint.setColor(textColor);
            canvas.drawText(text, start, end, x + blurRadius, y, paint);
        }
    }

    /**
     * Custom Span to apply ShadowLayer to specific characters within a TextView
     */
    static class ShadowSpan extends android.text.style.CharacterStyle implements android.text.style.UpdateAppearance {
        private final float radius, dx, dy;
        private final int shadowColor;

        public ShadowSpan(float radius, float dx, float dy, int shadowColor) {
            this.radius = radius;
            this.dx = dx;
            this.dy = dy;
            this.shadowColor = shadowColor;
        }

        @Override
        public void updateDrawState(android.text.TextPaint tp) {
            tp.setShadowLayer(radius, dx, dy, shadowColor);
        }
    }
}
