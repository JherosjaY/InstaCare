package com.example.instacare;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    private List<NewsItem> newsList;

    public NewsAdapter(List<NewsItem> newsList) {
        this.newsList = newsList;
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NewsViewHolder(
            LayoutInflater.from(parent.getContext()).inflate(R.layout.item_news, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        NewsItem item = newsList.get(position);
        holder.tvCategory.setText(item.getCategory());
        holder.tvTitle.setText(item.getTitle());
        holder.tvSummary.setText(item.getSummary());
        holder.tvTime.setText(item.getTimeAgo());
        holder.ivIcon.setImageResource(item.getIconResId());
        holder.ivIcon.setColorFilter(holder.itemView.getContext().getResources().getColor(R.color.emergency_red, null));
        
        // Dynamic colors based on category
        if ("Alert".equals(item.getCategory()) || "National".equals(item.getCategory())) {
            holder.tvCategory.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.emergency_red, null));
        } else {
            holder.tvCategory.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.primary, null));
        }

        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(holder.itemView.getContext(), NewsDetailActivity.class);
            intent.putExtra("NEWS_TITLE", item.getTitle());
            intent.putExtra("NEWS_CATEGORY", item.getCategory());
            intent.putExtra("NEWS_SUMMARY", item.getSummary());
            intent.putExtra("NEWS_TIME", item.getTimeAgo());
            intent.putExtra("NEWS_ICON", item.getIconResId());
            holder.itemView.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return newsList.size();
    }

    static class NewsViewHolder extends RecyclerView.ViewHolder {
        TextView tvCategory, tvTitle, tvSummary, tvTime;
        ImageView ivIcon;

        NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCategory = itemView.findViewById(R.id.tvNewsCategory);
            tvTitle = itemView.findViewById(R.id.tvNewsTitle);
            tvSummary = itemView.findViewById(R.id.tvNewsSummary);
            tvTime = itemView.findViewById(R.id.tvNewsTime);
            ivIcon = itemView.findViewById(R.id.ivNewsIcon);
        }
    }
}
