package com.example.instacare;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Random;

public class WeatherActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);

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
        
        setupDynamicWeatherData();
    }
    
    private void setupDynamicWeatherData() {
        Random random = new Random();
        int baseTemp = 74;
        
        // Hourly Arrays
        int[] hourIcons = {R.id.hour1Icon, R.id.hour2Icon, R.id.hour3Icon, R.id.hour4Icon, R.id.hour5Icon};
        int[] hourTemps = {R.id.hour1Temp, R.id.hour2Temp, R.id.hour3Temp, R.id.hour4Temp, R.id.hour5Temp};
        
        for (int i = 0; i < hourIcons.length; i++) {
            ImageView icon = findViewById(hourIcons[i]);
            TextView temp = findViewById(hourTemps[i]);
            int r = random.nextInt(10);
            int iconRes = r < 4 ? R.drawable.ic_sun_modern : (r < 7 ? R.drawable.ic_cloud : R.drawable.ic_storm);
            int tintRes = R.color.primary; // Consistent Red tint for brand identity
            
            icon.setImageResource(iconRes);
            icon.setColorFilter(androidx.core.content.ContextCompat.getColor(this, tintRes));
            temp.setText((baseTemp + random.nextInt(10) - 5) + "°");
        }
        
        // Daily Arrays
        int[] dayIcons = {R.id.day1Icon, R.id.day2Icon, R.id.day3Icon};
        int[] dayTemps = {R.id.day1Temp, R.id.day2Temp, R.id.day3Temp};
        
        for (int i = 0; i < dayIcons.length; i++) {
            ImageView icon = findViewById(dayIcons[i]);
            TextView temp = findViewById(dayTemps[i]);
            
            int r = random.nextInt(10);
            int iconRes = r < 4 ? R.drawable.ic_sun_modern : (r < 7 ? R.drawable.ic_cloud : R.drawable.ic_storm);
            int tintRes = R.color.primary; // Consistent Red tint for brand identity
            
            icon.setImageResource(iconRes);
            icon.setColorFilter(androidx.core.content.ContextCompat.getColor(this, tintRes));
            temp.setText((baseTemp + random.nextInt(14) - 4) + "°");
        }
    }
}
