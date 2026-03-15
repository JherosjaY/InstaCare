package com.example.instacare;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.data.local.AppDatabase;
import com.example.instacare.data.local.Guide;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import java.util.List;

public class GuidesAdapter extends RecyclerView.Adapter<GuidesAdapter.GuideViewHolder> {

    private List<Guide> guides;

    public GuidesAdapter(List<Guide> guides) {
        this.guides = guides;
    }

    public void setGuides(List<Guide> guides) {
        this.guides = guides;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GuideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new GuideViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.item_guide, parent, false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull GuideViewHolder holder, int position) {
        holder.bind(guides.get(position));
    }

    @Override
    public int getItemCount() {
        return guides != null ? guides.size() : 0;
    }

    static class GuideViewHolder extends RecyclerView.ViewHolder {
        private TextView title;
        private TextView category;
        private TextView description;
        private android.widget.ImageButton btnBookmark;
        private View videoThumbnailContainer;
        private android.widget.ImageView guideThumbnail;

        GuideViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.guideTitle);
            category = itemView.findViewById(R.id.guideCategory);
            description = itemView.findViewById(R.id.guideDescription);
            btnBookmark = itemView.findViewById(R.id.btnBookmark);
            videoThumbnailContainer = itemView.findViewById(R.id.videoThumbnailContainer);
            guideThumbnail = itemView.findViewById(R.id.guideThumbnail);
        }

        void bind(Guide item) {
            title.setText(item.title);
            category.setText(item.category);
            description.setText(item.description);
            
            // Set bookmark icon and tint based on state
            if (item.isBookmarked) {
                btnBookmark.setImageResource(R.drawable.ic_bookmark_filled);
                btnBookmark.setImageTintList(android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(itemView.getContext(), R.color.highlight_yellow)));
            } else {
                btnBookmark.setImageResource(R.drawable.ic_bookmark);
                btnBookmark.setImageTintList(android.content.res.ColorStateList.valueOf(
                    androidx.core.content.ContextCompat.getColor(itemView.getContext(), R.color.white)));
            }

            // Round top corners of thumbnail
            videoThumbnailContainer.setOutlineProvider(new android.view.ViewOutlineProvider() {
                @Override
                public void getOutline(android.view.View view, android.graphics.Outline outline) {
                    float radius = 12 * view.getResources().getDisplayMetrics().density;
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight() + (int)radius, radius);
                }
            });
            videoThumbnailContainer.setClipToOutline(true);

            videoThumbnailContainer.setOnClickListener(v -> {
                android.content.Context context = v.getContext();
                android.content.Intent intent = new android.content.Intent(context, VideoPlaybackActivity.class);
                intent.putExtra("VIDEO_URL", item.videoUrl);
                intent.putExtra("VIDEO_TITLE", item.title);
                context.startActivity(intent);
            });

            btnBookmark.setOnClickListener(v -> {
                boolean newStatus = !item.isBookmarked;
                item.isBookmarked = newStatus;
                
                // Update UI visually
                if (newStatus) {
                    btnBookmark.setImageResource(R.drawable.ic_bookmark_filled);
                    btnBookmark.setImageTintList(android.content.res.ColorStateList.valueOf(
                        androidx.core.content.ContextCompat.getColor(v.getContext(), R.color.highlight_yellow)));
                } else {
                    btnBookmark.setImageResource(R.drawable.ic_bookmark);
                    btnBookmark.setImageTintList(android.content.res.ColorStateList.valueOf(
                        androidx.core.content.ContextCompat.getColor(v.getContext(), R.color.white)));
                }
                
                // Update in database
                android.content.Context context = v.getContext();
                new Thread(() -> {
                    com.example.instacare.data.local.AppDatabase.getDatabase(context.getApplicationContext())
                        .guideDao().updateBookmarkStatus(item.id, newStatus);
                }).start();

                android.widget.Toast.makeText(v.getContext(), 
                    newStatus ? "Added to bookmarked list!" : "Removed from bookmarked list.", 
                    android.widget.Toast.LENGTH_SHORT).show();
            });

            // Load YouTube Thumbnail using Glide
            String videoId = extractVideoId(item.videoUrl);
            if (!videoId.isEmpty()) {
                String thumbnailUrl = "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
                Glide.with(itemView.getContext())
                    .load(thumbnailUrl)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(guideThumbnail);
            }

            itemView.setOnClickListener(v -> {
                android.content.Context context = v.getContext();
                android.content.Intent intent = new android.content.Intent(context, GuideDetailActivity.class);
                intent.putExtra("GUIDE_ID", item.id); // Pass ID for DB lookup in detail
                intent.putExtra("GUIDE_TITLE", item.title);
                intent.putExtra("GUIDE_DESC", item.description);
                intent.putExtra("GUIDE_STEPS", item.stepsJson);
                intent.putExtra("GUIDE_VIDEO_URL", item.videoUrl);
                context.startActivity(intent);
            });
        }
    }
    private static String extractVideoId(String url) {
        if (url == null || url.isEmpty()) return "";
        if (url.contains("v=")) {
            String[] parts = url.split("v=");
            if (parts.length > 1) {
                String sub = parts[1];
                int ampersandIndex = sub.indexOf("&");
                return ampersandIndex != -1 ? sub.substring(0, ampersandIndex) : sub;
            }
        } else if (url.contains("youtu.be/")) {
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
