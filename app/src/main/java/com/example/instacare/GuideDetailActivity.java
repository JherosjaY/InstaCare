package com.example.instacare;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.instacare.data.local.AppDatabase;
import com.example.instacare.data.local.Guide;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import android.widget.ImageView;
import androidx.annotation.NonNull;

public class GuideDetailActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide_detail);
        
        String guideId = getIntent().getStringExtra("GUIDE_ID");
        String title = getIntent().getStringExtra("GUIDE_TITLE");
        String desc = getIntent().getStringExtra("GUIDE_DESC");
        String videoUrl = getIntent().getStringExtra("GUIDE_VIDEO_URL");

        if (title != null) {
            // Setup Toolbar
            com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
            toolbar.setTitle(title);
            toolbar.setNavigationOnClickListener(v -> finish());
            
            if (guideId != null) {
                // Fetch guide to get bookmark status
                new Thread(() -> {
                    Guide targetGuide = null;
                    java.util.List<Guide> allGuides = AppDatabase.getDatabase(getApplicationContext()).guideDao().getAllGuidesDirect();
                    for (Guide g : allGuides) {
                        if (g.id.equals(guideId)) {
                            targetGuide = g;
                            break;
                        }
                    }
                    
                    if (targetGuide != null) {
                        final Guide finalGuide = targetGuide;
                        runOnUiThread(() -> {
                            MenuItem bookmarkItem = toolbar.getMenu().findItem(R.id.action_bookmark);
                            if (bookmarkItem != null) {
                                bookmarkItem.setIcon(finalGuide.isBookmarked ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark);
                                int tintColor = getResources().getColor(
                                    finalGuide.isBookmarked ? R.color.highlight_yellow : R.color.text_primary, 
                                    getTheme());
                                bookmarkItem.setIconTintList(android.content.res.ColorStateList.valueOf(tintColor));
                            }
                        });

                        toolbar.setOnMenuItemClickListener(item -> {
                            if (item.getItemId() == R.id.action_bookmark) {
                                boolean newStatus = !finalGuide.isBookmarked;
                                finalGuide.isBookmarked = newStatus;
                                item.setIcon(newStatus ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark);
                                int tintColor = getResources().getColor(
                                    newStatus ? R.color.highlight_yellow : R.color.text_primary, 
                                    getTheme());
                                item.setIconTintList(android.content.res.ColorStateList.valueOf(tintColor));
                                
                                new Thread(() -> {
                                    AppDatabase.getDatabase(getApplicationContext())
                                        .guideDao().updateBookmarkStatus(guideId, newStatus);
                                }).start();
                                
                                Toast.makeText(this, 
                                    newStatus ? "Bookmarked!" : "Removed Bookmark", 
                                    Toast.LENGTH_SHORT).show();
                                return true;
                            }
                            return false;
                        });
                    }
                }).start();
            }
            
            TextView titleTv = findViewById(R.id.guideTitle);
            if (titleTv != null) titleTv.setText(title);

            TextView descTv = findViewById(R.id.guideDescription);
            if (descTv != null && desc != null) descTv.setText(desc);
        }

        // Embedded YouTube Player Setup
        YouTubePlayerView youtubePlayerView = findViewById(R.id.youtube_player_view);
        ImageView placeholder = findViewById(R.id.detailThumbnailPlaceholder);
        
        if (youtubePlayerView != null && videoUrl != null && !videoUrl.isEmpty()) {
            getLifecycle().addObserver(youtubePlayerView);
            
            final String finalVideoId = extractVideoId(videoUrl);
            
            // Load Placeholder Thumbnail
            if (placeholder != null && !finalVideoId.isEmpty()) {
                String thumbnailUrl = "https://img.youtube.com/vi/" + finalVideoId + "/maxresdefault.jpg";
                Glide.with(this)
                    .load(thumbnailUrl)
                    .error("https://img.youtube.com/vi/" + finalVideoId + "/hqdefault.jpg") // Fallback if maxres not available
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(placeholder);
            }

            youtubePlayerView.addYouTubePlayerListener(new AbstractYouTubePlayerListener() {
                @Override
                public void onReady(@NonNull YouTubePlayer youtubePlayer) {
                    youtubePlayer.cueVideo(finalVideoId, 0);
                    // Hide placeholder when ready
                    if (placeholder != null) {
                        placeholder.animate().alpha(0f).setDuration(500).withEndAction(() -> 
                            placeholder.setVisibility(android.view.View.GONE)).start();
                    }
                }
            });
        }

        String stepsJson = getIntent().getStringExtra("GUIDE_STEPS");
        android.widget.TextView stepsTv = findViewById(R.id.guideSteps);
        
        if (stepsJson != null && stepsTv != null) {
            try {
                org.json.JSONArray stepsArray = new org.json.JSONArray(stepsJson);
                StringBuilder formattedSteps = new StringBuilder();
                
                for (int i = 0; i < stepsArray.length(); i++) {
                    org.json.JSONObject step = stepsArray.getJSONObject(i);
                    int order = step.optInt("order", i + 1);
                    String stepDesc = step.optString("description", "");
                    
                    formattedSteps.append(order).append(". ").append(stepDesc);
                    if (i < stepsArray.length() - 1) {
                        formattedSteps.append("\n\n");
                    }
                }
                
                if (formattedSteps.length() > 0) {
                    stepsTv.setText(formattedSteps.toString());
                } else {
                     stepsTv.setText("No specific steps available. Please watch the video.");
                }

            } catch (org.json.JSONException e) {
                // Fallback if not JSON or empty
                stepsTv.setText(stepsJson.isEmpty() || "[]".equals(stepsJson) ? "No steps available." : stepsJson);
            }
        }

        findViewById(R.id.call911Detail).setOnClickListener(v -> {
            showHotlinesBottomSheet();
        });
    }

    private void showHotlinesBottomSheet() {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = 
            new com.google.android.material.bottomsheet.BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
            
        android.view.View bottomSheetView = getLayoutInflater().inflate(R.layout.layout_emergency_hotlines_bottom_sheet, null);
        android.widget.LinearLayout container = bottomSheetView.findViewById(R.id.hotlinesContainer);
        
        HotlineHelper.populateCategorized(this, container, number -> {
            bottomSheetDialog.dismiss();
            currentNumberToCall = number;
            makeCall(number);
        });
        
        bottomSheetDialog.setContentView(bottomSheetView);
        
        // Remove white background default from bottom sheet container
        android.view.View bottomSheetViewParent = (android.view.View) bottomSheetView.getParent();
        if (bottomSheetViewParent != null) {
            bottomSheetViewParent.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }
        
        bottomSheetDialog.show();
    }

    private String currentNumberToCall = null;

    private void makeCall(String number) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.CALL_PHONE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CALL_PHONE}, 101);
        } else {
            performCall(number);
        }
    }

    private void performCall(String number) {
        if (number == null || number.isEmpty()) return;
        android.content.Intent callIntent = new android.content.Intent(android.content.Intent.ACTION_CALL);
        callIntent.setData(android.net.Uri.parse("tel:" + number));
        try {
            startActivity(callIntent);
        } catch (SecurityException e) {
            Toast.makeText(this, "Permission to place call denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                performCall(currentNumberToCall);
            } else {
                Toast.makeText(this, "Permission to place call denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String extractVideoId(String url) {
        if (url == null || url.isEmpty()) return "";
        if (url.contains("v=")) {
            // e.g. https://www.youtube.com/watch?v=VIDEO_ID
            String[] parts = url.split("v=");
            if (parts.length > 1) {
                String sub = parts[1];
                int ampersandIndex = sub.indexOf("&");
                return ampersandIndex != -1 ? sub.substring(0, ampersandIndex) : sub;
            }
        } else if (url.contains("youtu.be/")) {
            // e.g. https://youtu.be/VIDEO_ID
            String[] parts = url.split("youtu.be/");
            if (parts.length > 1) {
                String sub = parts[1];
                int questionIndex = sub.indexOf("?");
                return questionIndex != -1 ? sub.substring(0, questionIndex) : sub;
            }
        }
        return "";
    }
}
