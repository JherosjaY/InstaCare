package com.example.instacare;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.instacare.data.local.AppDatabase;
import com.example.instacare.data.local.DisasterGuide;
import com.example.instacare.data.local.Guide;
import java.util.List;

public class GenericGuidesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_FIRST_AID = 0;
    private static final int TYPE_DISASTER = 1;

    private List<Object> items;

    public void setItems(List<Object> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (items.get(position) instanceof Guide) return TYPE_FIRST_AID;
        return TYPE_DISASTER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_FIRST_AID) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_guide, parent, false);
            return new FirstAidViewHolder(v);
        } else {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_disaster_guide, parent, false);
            return new DisasterViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = items.get(position);
        if (holder instanceof FirstAidViewHolder) {
            ((FirstAidViewHolder) holder).bind((Guide) item);
        } else {
            ((DisasterViewHolder) holder).bind((DisasterGuide) item);
        }
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class FirstAidViewHolder extends RecyclerView.ViewHolder {
        TextView title, category, description;
        ImageButton btnBookmark;
        ImageView thumbnail;
        View videoContainer;

        FirstAidViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.guideTitle);
            category = itemView.findViewById(R.id.guideCategory);
            description = itemView.findViewById(R.id.guideDescription);
            btnBookmark = itemView.findViewById(R.id.btnBookmark);
            thumbnail = itemView.findViewById(R.id.guideThumbnail);
            videoContainer = itemView.findViewById(R.id.videoThumbnailContainer);
        }

        void bind(Guide guide) {
            title.setText(guide.title);
            category.setText(guide.category);
            description.setText(guide.description);
            
            updateBookmarkIcon(guide.isBookmarked);

            loadThumbnail(guide.videoUrl, thumbnail);

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), GuideDetailActivity.class);
                intent.putExtra("GUIDE_ID", guide.id);
                intent.putExtra("GUIDE_TITLE", guide.title);
                intent.putExtra("GUIDE_DESC", guide.description);
                intent.putExtra("GUIDE_STEPS", guide.stepsJson);
                intent.putExtra("GUIDE_VIDEO_URL", guide.videoUrl);
                v.getContext().startActivity(intent);
            });

            btnBookmark.setOnClickListener(v -> {
                guide.isBookmarked = !guide.isBookmarked;
                updateBookmarkIcon(guide.isBookmarked);
                new Thread(() -> AppDatabase.getDatabase(v.getContext().getApplicationContext())
                        .guideDao().updateBookmarkStatus(guide.id, guide.isBookmarked)).start();
            });
        }

        private void updateBookmarkIcon(boolean bookmarked) {
            btnBookmark.setImageResource(bookmarked ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark);
            btnBookmark.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(itemView.getContext(), 
                    bookmarked ? R.color.highlight_yellow : R.color.white)));
        }
    }

    static class DisasterViewHolder extends RecyclerView.ViewHolder {
        TextView title, category, description, difficulty, duration;
        ImageButton btnBookmark;
        ImageView thumbnail;

        DisasterViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tvGuideTitle);
            category = itemView.findViewById(R.id.tvGuideCategory);
            description = itemView.findViewById(R.id.tvGuideDescription);
            difficulty = itemView.findViewById(R.id.tvDifficulty);
            duration = itemView.findViewById(R.id.tvDuration);
            btnBookmark = itemView.findViewById(R.id.btnBookmarkDisaster);
            thumbnail = itemView.findViewById(R.id.guideThumbnail);
        }

        void bind(DisasterGuide guide) {
            title.setText(guide.title);
            category.setText(guide.disasterType);
            description.setText(guide.description);
            difficulty.setText(guide.difficulty);
            duration.setText(guide.duration);

            updateBookmarkIcon(guide.isBookmarked);

            loadThumbnail(guide.videoUrl, thumbnail);

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), GuideDetailActivity.class);
                intent.putExtra("GUIDE_ID", guide.id);
                intent.putExtra("GUIDE_TITLE", guide.title);
                intent.putExtra("GUIDE_DESC", guide.description);
                intent.putExtra("GUIDE_VIDEO_URL", guide.videoUrl);
                intent.putExtra("GUIDE_STEPS", guide.stepsJson);
                intent.putExtra("IS_DISASTER", true);
                v.getContext().startActivity(intent);
            });

            btnBookmark.setOnClickListener(v -> {
                guide.isBookmarked = !guide.isBookmarked;
                updateBookmarkIcon(guide.isBookmarked);
                new Thread(() -> AppDatabase.getDatabase(v.getContext().getApplicationContext())
                        .disasterGuideDao().updateBookmarkStatus(guide.id, guide.isBookmarked)).start();
            });
        }

        private void updateBookmarkIcon(boolean bookmarked) {
            btnBookmark.setImageResource(bookmarked ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark);
            btnBookmark.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(itemView.getContext(), 
                    bookmarked ? R.color.highlight_yellow : R.color.white)));
        }
    }

    private static void loadThumbnail(String url, ImageView iv) {
        String videoId = extractVideoId(url);
        if (!videoId.isEmpty()) {
            String thumbnailUrl = "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
            Glide.with(iv.getContext())
                .load(thumbnailUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(iv);
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
