package com.example.instacare;

import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import com.example.instacare.databinding.ActivityVideoPlaybackBinding;

public class VideoPlaybackActivity extends AppCompatActivity {

    private ActivityVideoPlaybackBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityVideoPlaybackBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String videoUrl = getIntent().getStringExtra("VIDEO_URL");
        // String videoTitle = getIntent().getStringExtra("VIDEO_TITLE"); // Unused for now

        if (videoUrl != null) {
            setupWebView(videoUrl);
        }

        binding.btnBack.setOnClickListener(v -> finish());
    }

    private void setupWebView(String url) {
        WebSettings webSettings = binding.webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setUseWideViewPort(true);

        // meaningful name for binding
        binding.webView.setWebViewClient(new WebViewClient());
        binding.webView.setWebChromeClient(new WebChromeClient());
        
        binding.webView.loadUrl(url);
    }
    
    @Override
    public void onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
