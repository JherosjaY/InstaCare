package com.example.instacare;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class NewsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);

        // Immersive UI setup
        android.view.Window window = getWindow();
        int colorBg = androidx.core.content.ContextCompat.getColor(this, R.color.dashboard_background);

        // Dynamic Navigation Bar Color
        boolean isDark = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        int surfaceColor = androidx.core.content.ContextCompat.getColor(this, R.color.dashboard_surface);

        // Edge-to-Edge immersive UI
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

        // --- Premium Branding: Highlight "&" in Red ---
        TextView tvTitle = findViewById(R.id.tvToolbarTitle);
        if (tvTitle != null) {
            String titleText = "News <font color='#E53935'>&</font> Alerts";
            tvTitle.setText(android.text.Html.fromHtml(titleText, android.text.Html.FROM_HTML_MODE_LEGACY));
        }
        
        loadNewsData();
    }

    private void loadNewsData() {
        RecyclerView rvNews = findViewById(R.id.rvNews);
        android.view.View loaderContainer = findViewById(R.id.newsLoaderContainer);
        android.view.View emptyState = findViewById(R.id.emptyStateContainer);
        
        rvNews.setLayoutManager(new LinearLayoutManager(this));
        rvNews.setVisibility(android.view.View.GONE);
        emptyState.setVisibility(android.view.View.GONE);
        
        // 1. Show Loading State (Skeleton) immediately
        if (loaderContainer != null) {
            loaderContainer.setVisibility(android.view.View.VISIBLE);
            android.view.animation.Animation pulse = android.view.animation.AnimationUtils.loadAnimation(this, R.anim.pulse_shimmer);
            loaderContainer.startAnimation(pulse);
        }

        // 2. Start Data Fetching & Sync with 7s Animation
        long startTime = System.currentTimeMillis();
        com.example.instacare.utils.NewsHelper newsHelper = new com.example.instacare.utils.NewsHelper();
        
        newsHelper.fetchLatestNews(new com.example.instacare.utils.NewsHelper.NewsCallback() {
            @Override
            public void onSuccess(java.util.List<NewsItem> newsList) {
                syncUI(() -> {
                    if (loaderContainer != null) {
                        loaderContainer.clearAnimation();
                        loaderContainer.setVisibility(android.view.View.GONE);
                    }
                    rvNews.setVisibility(android.view.View.VISIBLE);
                    rvNews.setAdapter(new NewsAdapter(newsList));
                }, startTime);
            }

            @Override
            public void onError(String error) {
                syncUI(() -> {
                    // Apply Blur to Loader (Maintain premium transition)
                    if (loaderContainer != null) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            android.graphics.RenderEffect blur = android.graphics.RenderEffect.createBlurEffect(
                                20f, 20f, android.graphics.Shader.TileMode.CLAMP
                            );
                            loaderContainer.setRenderEffect(blur);
                        } else {
                            loaderContainer.setAlpha(0.5f);
                        }
                    }
                    // Show "No Internet" Empty State Overlay
                    if (emptyState != null) {
                        emptyState.setVisibility(android.view.View.VISIBLE);
                    }
                }, startTime);
            }
        });
    }

    private void syncUI(Runnable task, long startTime) {
        long elapsed = System.currentTimeMillis() - startTime;
        long remainingDelay = Math.max(0, 7000 - elapsed);
        
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinishing()) {
                task.run();
            }
        }, remainingDelay);
    }
}
