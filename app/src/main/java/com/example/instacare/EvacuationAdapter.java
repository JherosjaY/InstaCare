package com.example.instacare;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.data.local.EvacuationCenter;
import java.util.List;

public class EvacuationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<EvacuationCenter> centers;
    private OnEvacClickListener listener;
    private boolean isLoading = false;

    private static final int VIEW_TYPE_SHIMMER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    public interface OnEvacClickListener {
        void onEvacClick(EvacuationCenter center);
    }

    public EvacuationAdapter(List<EvacuationCenter> centers, OnEvacClickListener listener) {
        this.centers = centers;
        this.listener = listener;
    }

    public void setCenters(List<EvacuationCenter> centers) {
        this.centers = centers;
        isLoading = false;
        notifyDataSetChanged();
    }

    public void setLoading(boolean loading) {
        isLoading = loading;
        if (loading) centers = null;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return isLoading ? VIEW_TYPE_SHIMMER : VIEW_TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SHIMMER) {
            return new ShimmerViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.item_location_skeleton, parent, false
                )
            );
        }
        return new EvacViewHolder(
            LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_evacuation, parent, false
            ),
            listener
        );
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ShimmerViewHolder) {
            if (holder.itemView.getAnimation() == null) {
                android.view.animation.Animation pulse = android.view.animation.AnimationUtils.loadAnimation(
                    holder.itemView.getContext(), R.anim.pulse_shimmer);
                holder.itemView.startAnimation(pulse);
            }
        } else {
            ((EvacViewHolder) holder).bind(centers.get(position));
        }
    }

    @Override
    public int getItemCount() {
        if (isLoading) return 5;
        return centers != null ? centers.size() : 0;
    }

    static class ShimmerViewHolder extends RecyclerView.ViewHolder {
        ShimmerViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    static class EvacViewHolder extends RecyclerView.ViewHolder {
        private TextView name, distance, address, status;
        private OnEvacClickListener listener;

        EvacViewHolder(@NonNull View itemView, OnEvacClickListener listener) {
            super(itemView);
            this.listener = listener;
            name     = itemView.findViewById(R.id.evacName);
            distance = itemView.findViewById(R.id.evacDistance);
            address  = itemView.findViewById(R.id.evacAddress);
            status   = itemView.findViewById(R.id.evacStatus);
        }

        void bind(EvacuationCenter item) {
            name.setText(item.name);

            String addressText = item.address != null && !item.address.isEmpty() ? item.address : "";
            if (item.type != null && !item.type.isEmpty()) {
                addressText = addressText.isEmpty() ? item.type : addressText + " · " + item.type;
            }
            address.setText(addressText);

            distance.setText(item.distance != null && !item.distance.isEmpty() ? item.distance : "--");
            applyDistanceColor(item.distance);

            bindStatusBadge(item.status);

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onEvacClick(item);
            });
        }

        private void applyDistanceColor(String distanceStr) {
            try {
                String distStr = (distanceStr != null ? distanceStr : "")
                        .replace(" km", "").trim();
                double dist = Double.parseDouble(distStr);
                int color;
                if (dist < 2.0) {
                    color = ContextCompat.getColor(itemView.getContext(), R.color.success_green);
                } else if (dist <= 5.0) {
                    color = ContextCompat.getColor(itemView.getContext(), R.color.warning_orange);
                } else {
                    color = Color.parseColor("#FF6D00");
                }
                distance.setTextColor(color);
            } catch (Exception e) {
                distance.setTextColor(Color.parseColor("#FF6D00"));
            }
        }

        private void bindStatusBadge(String statusStr) {
            if (status == null) return;

            if (statusStr == null || statusStr.trim().isEmpty()) {
                status.setText("Unknown");
                status.getBackground().setTint(Color.parseColor("#9E9E9E"));
                return;
            }

            switch (statusStr.trim().toLowerCase()) {
                case "open":
                    status.setText("● Open");
                    status.getBackground().setTint(Color.parseColor("#10B981"));
                    break;
                case "full":
                    status.setText("● Full");
                    status.getBackground().setTint(Color.parseColor("#EF4444"));
                    break;
                case "closed":
                    status.setText("● Closed");
                    status.getBackground().setTint(Color.parseColor("#6B7280"));
                    break;
                default:
                    status.setText(statusStr);
                    status.getBackground().setTint(Color.parseColor("#F97316"));
                    break;
            }
        }
    }
}
