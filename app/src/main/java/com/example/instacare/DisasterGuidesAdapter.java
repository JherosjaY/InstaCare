package com.example.instacare;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.instacare.data.local.DisasterGuide;
import java.util.ArrayList;
import java.util.List;

public class DisasterGuidesAdapter extends RecyclerView.Adapter<DisasterGuidesAdapter.ViewHolder> {

    private List<DisasterGuide> guides;
    private final Listener listener;

    public interface Listener {
        void onClick(DisasterGuide guide);
        default void onBookmarkClick(DisasterGuide guide) {}
    }

    public DisasterGuidesAdapter(List<DisasterGuide> guides, Listener listener) {
        this.guides = guides;
        this.listener = listener;
    }

    public void updateList(List<DisasterGuide> newList) {
        this.guides = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_disaster_guide, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DisasterGuide guide = guides.get(position);
        holder.tvTitle.setText(guide.title);
        holder.tvCategory.setText(guide.disasterType);
        holder.tvDescription.setText(guide.description);
        holder.tvDifficulty.setText(guide.difficulty);
        holder.tvDuration.setText(guide.duration);

        // Load YouTube Thumbnail
        String videoId = extractVideoId(guide.videoUrl);
        if (!videoId.isEmpty()) {
            String thumbnailUrl = "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg";
            Glide.with(holder.itemView.getContext())
                .load(thumbnailUrl)
                .transition(DrawableTransitionOptions.withCrossFade())
                .placeholder(R.drawable.motivational_be_prepared)
                .into(holder.ivThumbnail);
        } else {
            holder.ivThumbnail.setImageResource(R.drawable.motivational_be_prepared);
        }

        // Bookmark state
        if (guide.isBookmarked) {
            holder.btnBookmark.setImageResource(R.drawable.ic_bookmark_filled);
            holder.btnBookmark.setImageTintList(android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.highlight_yellow)));
        } else {
            holder.btnBookmark.setImageResource(R.drawable.ic_bookmark);
            holder.btnBookmark.setImageTintList(android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(holder.itemView.getContext(), R.color.white)));
        }

        holder.itemView.setOnClickListener(v -> listener.onClick(guide));
        holder.btnBookmark.setOnClickListener(v -> listener.onBookmarkClick(guide));
    }

    private String extractVideoId(String url) {
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

    @Override
    public int getItemCount() {
        return guides != null ? guides.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCategory, tvDescription, tvDifficulty, tvDuration;
        ImageView ivThumbnail;
        ImageButton btnBookmark;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvGuideTitle);
            tvCategory = itemView.findViewById(R.id.tvGuideCategory);
            tvDescription = itemView.findViewById(R.id.tvGuideDescription);
            tvDifficulty = itemView.findViewById(R.id.tvDifficulty);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            ivThumbnail = itemView.findViewById(R.id.guideThumbnail);
            btnBookmark = itemView.findViewById(R.id.btnBookmarkDisaster);
        }
    }
}

