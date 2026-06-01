package com.example.instacare;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class MotivationalCarouselAdapter extends RecyclerView.Adapter<MotivationalCarouselAdapter.SlideViewHolder> {

    private final int[] imageResIds;

    public MotivationalCarouselAdapter(int[] imageResIds) {
        this.imageResIds = imageResIds;
    }

    @NonNull
    @Override
    public SlideViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView imageView = new ImageView(parent.getContext());
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return new SlideViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(@NonNull SlideViewHolder holder, int position) {
        ((ImageView) holder.itemView).setImageResource(imageResIds[position]);
    }

    @Override
    public int getItemCount() {
        return imageResIds.length;
    }

    static class SlideViewHolder extends RecyclerView.ViewHolder {
        SlideViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
