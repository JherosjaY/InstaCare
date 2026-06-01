package com.example.instacare;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class NewsDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_detail);
        
        // Edge-to-Edge immersive UI
        android.view.Window window = getWindow();
        int surfaceColor = androidx.core.content.ContextCompat.getColor(this, R.color.dashboard_surface);
        boolean isDark = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
        window.setNavigationBarColor(surfaceColor);

        androidx.core.view.WindowInsetsControllerCompat controller = androidx.core.view.WindowCompat.getInsetsController(window, window.getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(!isDark);
            controller.setAppearanceLightNavigationBars(!isDark);
        }

        android.view.View topBar = findViewById(R.id.topBar);
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(topBar, (v, windowInsets) -> {
            androidx.core.graphics.Insets insets = windowInsets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            int padding12 = (int)(12 * getResources().getDisplayMetrics().density);
            int padding16 = (int)(16 * getResources().getDisplayMetrics().density);
            v.setPadding(padding16, insets.top + padding12, padding16, padding12);
            return windowInsets;
        });

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // --- Premium Branding: Highlight "Details" in Red ---
        TextView tvHeaderTitle = findViewById(R.id.tvToolbarTitle);
        if (tvHeaderTitle != null) {
            String titleHtml = "News <font color='#E53935'>Details</font>";
            tvHeaderTitle.setText(android.text.Html.fromHtml(titleHtml, android.text.Html.FROM_HTML_MODE_LEGACY));
        }

        // Get Data from Intent
        String title = getIntent().getStringExtra("NEWS_TITLE");
        String category = getIntent().getStringExtra("NEWS_CATEGORY");
        String summary = getIntent().getStringExtra("NEWS_SUMMARY");
        String timeAgo = getIntent().getStringExtra("NEWS_TIME");
        int iconResId = getIntent().getIntExtra("NEWS_ICON", R.drawable.ic_map_pin);
        String imageUrl = getIntent().getStringExtra("NEWS_IMAGE_URL");
        String fullContent = getIntent().getStringExtra("NEWS_FULL_CONTENT");

        // Bind Views
        TextView tvTitle = findViewById(R.id.tvDetailTitle);
        TextView tvCategory = findViewById(R.id.tvDetailCategory);
        TextView tvTime = findViewById(R.id.tvDetailTime);
        TextView tvContent = findViewById(R.id.tvDetailContent);
        ImageView ivIcon = findViewById(R.id.ivDetailIcon);

        if (title != null) tvTitle.setText(title);
        if (category != null) tvCategory.setText(category);
        if (timeAgo != null) tvTime.setText(timeAgo);
        
        // --- High Fidelity Content: Live Body from API ---
        if (fullContent != null && !fullContent.isEmpty()) {
            tvContent.setText(fullContent);
        } else if (summary != null) {
            tvContent.setText(summary);
        }
        
        // --- High Fidelity Visuals: Reverted to branded icons ---
        ivIcon.setImageResource(iconResId);

        // Dynamic category colors
        if ("Alert".equals(category) || "National".equals(category)) {
            tvCategory.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.emergency_red));
            ivIcon.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.emergency_red));
        } else {
            tvCategory.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.primary));
            ivIcon.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.primary));
        }
    }
}
