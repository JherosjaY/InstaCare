package com.example.instacare;

import android.os.Bundle;
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

        // Status bar setup
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
        
        setupNewsList();
    }
    
    private void setupNewsList() {
        RecyclerView rvNews = findViewById(R.id.rvNews);
        rvNews.setLayoutManager(new LinearLayoutManager(this));
        
        // Dummy News Data
        List<NewsItem> newsList = new ArrayList<>();
        newsList.add(new NewsItem("National", "Heavy Rainfall Warning", "PAGASA raised orange warning level over Metro Manila.", "Just now", R.drawable.ic_map_pin));
        newsList.add(new NewsItem("Local", "Barangay Road Repair", "Main avenue will be closed starting 8 PM tonight for emergency repairs.", "2h ago", R.drawable.ic_alert_triangle));
        newsList.add(new NewsItem("Alert", "Scheduled Power Interruption", "Expect power cut from 1 PM to 5 PM tomorrow due to NGCP maintenance.", "5h ago", R.drawable.ic_alert_triangle));
        newsList.add(new NewsItem("News", "Free Medical Mission", "City health office is offering free consultations this Sunday at the plaza.", "Yesterday", R.drawable.ic_hospital));
        newsList.add(new NewsItem("Alert", "Suspension of Classes", "Mayor announced suspension of all levels tomorrow due to continuous rains.", "Yesterday", R.drawable.ic_file_text));
        
        NewsAdapter adapter = new NewsAdapter(newsList);
        rvNews.setAdapter(adapter);
    }
}
