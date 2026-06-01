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

public class EvacuationAdapter extends RecyclerView.Adapter<EvacuationAdapter.EvacViewHolder> {

    private List<EvacuationCenter> centers;
    private OnEvacClickListener listener;

    public interface OnEvacClickListener {
        void onEvacClick(EvacuationCenter center);
    }

    public EvacuationAdapter(List<EvacuationCenter> centers, OnEvacClickListener listener) {
        this.centers = centers;
        this.listener = listener;
    }

    public void setCenters(List<EvacuationCenter> centers) {
        this.centers = centers;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EvacViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new EvacViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.item_evacuation, parent, false
                ),
                listener
        );
    }

    @Override
    public void onBindViewHolder(@NonNull EvacViewHolder holder, int position) {
        holder.bind(centers.get(position));
    }

    @Override
    public int getItemCount() {
        return centers != null ? centers.size() : 0;
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
            // ── Name ──
            name.setText(item.name);

            // ── Address ──
            String addressText = item.address != null && !item.address.isEmpty() ? item.address : "";
            if (item.type != null && !item.type.isEmpty()) {
                addressText = addressText.isEmpty() ? item.type : addressText + " · " + item.type;
            }
            address.setText(addressText);

            // ── Distance ──
            distance.setText(item.distance != null && !item.distance.isEmpty() ? item.distance : "--");
            applyDistanceColor(item.distance);

            // ── Status Badge (Feature 2 — Tier 1) ──
            bindStatusBadge(item.status);

            // ── Click ──
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onEvacClick(item);
            });
        }

        /** Color-codes the distance label: green = near, orange = moderate, deep-orange = far */
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
                    color = Color.parseColor("#FF6D00"); // Deep orange for far
                }
                distance.setTextColor(color);
            } catch (Exception e) {
                distance.setTextColor(Color.parseColor("#FF6D00"));
            }
        }

        /**
         * Feature 2 — Status Badge
         * Open  → green pill
         * Full  → red pill
         * Closed → dark grey pill
         * null/unknown → grey
         */
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
                    status.getBackground().setTint(Color.parseColor("#10B981")); // success green
                    break;
                case "full":
                    status.setText("● Full");
                    status.getBackground().setTint(Color.parseColor("#EF4444")); // emergency red
                    break;
                case "closed":
                    status.setText("● Closed");
                    status.getBackground().setTint(Color.parseColor("#6B7280")); // gray
                    break;
                default:
                    status.setText(statusStr);
                    status.getBackground().setTint(Color.parseColor("#F97316")); // orange
                    break;
            }
        }
    }
}
