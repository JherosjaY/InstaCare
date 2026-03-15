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

        // Get Data from Intent
        String title = getIntent().getStringExtra("NEWS_TITLE");
        String category = getIntent().getStringExtra("NEWS_CATEGORY");
        String summary = getIntent().getStringExtra("NEWS_SUMMARY");
        String timeAgo = getIntent().getStringExtra("NEWS_TIME");
        int iconResId = getIntent().getIntExtra("NEWS_ICON", R.drawable.ic_map_pin);

        // Bind Views
        TextView tvTitle = findViewById(R.id.tvDetailTitle);
        TextView tvCategory = findViewById(R.id.tvDetailCategory);
        TextView tvTime = findViewById(R.id.tvDetailTime);
        TextView tvContent = findViewById(R.id.tvDetailContent);
        ImageView ivIcon = findViewById(R.id.ivDetailIcon);

        if (title != null) tvTitle.setText(title);
        if (category != null) tvCategory.setText(category);
        if (timeAgo != null) tvTime.setText(timeAgo);
        
        // Using summary as prefix for detailed content for MVP
        if (summary != null) {
            tvContent.setText(summary + "\n\nThis is a detailed view of the news. In a production application, this text would extend much further and include comprehensive details provided by the API regarding the emergency or news event.");
        }
        
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
