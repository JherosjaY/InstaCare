package com.example.instacare;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.data.local.AppDatabase;
import com.example.instacare.data.local.EvacuationCenter;
import com.example.instacare.data.local.FavoriteCenter;
import com.google.android.material.button.MaterialButton;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public class EvacuationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<EvacuationCenter> centers;
    private OnEvacClickListener listener;
    private OnViewDetailsClickListener detailsListener;
    private OnFavoriteToggleListener favoriteListener;
    private AppDatabase db;
    private ExecutorService executor;
    private Set<String> favoriteNames;
    private boolean isLoading = false;

    private static final int VIEW_TYPE_SHIMMER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    public interface OnEvacClickListener {
        void onEvacClick(EvacuationCenter center);
    }

    public interface OnViewDetailsClickListener {
        void onViewDetails(EvacuationCenter center);
    }

    public interface OnFavoriteToggleListener {
        void onFavoriteToggled(EvacuationCenter center, boolean isFavorite);
    }

    public EvacuationAdapter(List<EvacuationCenter> centers, OnEvacClickListener listener) {
        this.centers = centers;
        this.listener = listener;
    }

    public void setFavoriteDb(AppDatabase db, ExecutorService executor) {
        this.db = db;
        this.executor = executor;
    }

    public void setFavoriteNames(Set<String> names) {
        this.favoriteNames = names;
    }

    public void setOnFavoriteToggleListener(OnFavoriteToggleListener listener) {
        this.favoriteListener = listener;
    }

    public void setOnViewDetailsClickListener(OnViewDetailsClickListener listener) {
        this.detailsListener = listener;
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
        EvacViewHolder evh = new EvacViewHolder(
            LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_evacuation, parent, false
            ),
            listener
        );
        evh.detailsListener = detailsListener;
        evh.favoriteListener = favoriteListener;
        return evh;
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
            ((EvacViewHolder) holder).bind(centers.get(position), position, favoriteNames, db, executor);
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
        private TextView name, distance, address, status, capacityText, nearestBadge;
        private android.widget.ProgressBar capacityBar;
        private MaterialButton btnDirections, btnViewDetails;
        private android.widget.ImageButton btnFavorite;
        private OnEvacClickListener listener;
        private OnViewDetailsClickListener detailsListener;
        private OnFavoriteToggleListener favoriteListener;

        EvacViewHolder(@NonNull View itemView, OnEvacClickListener listener) {
            super(itemView);
            this.listener = listener;
            name     = itemView.findViewById(R.id.evacName);
            distance = itemView.findViewById(R.id.evacDistance);
            address  = itemView.findViewById(R.id.evacAddress);
            status   = itemView.findViewById(R.id.evacStatus);
            capacityBar = itemView.findViewById(R.id.evacCapacityBar);
            capacityText = itemView.findViewById(R.id.evacCapacityText);
            btnDirections = itemView.findViewById(R.id.btnDirections);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
            nearestBadge = itemView.findViewById(R.id.nearestBadge);
            btnFavorite = itemView.findViewById(R.id.btnFavorite);
        }

        void bind(EvacuationCenter item, int position, Set<String> favoriteNames, AppDatabase db, ExecutorService executor) {
            name.setText(item.name);

            String addressText = item.address != null && !item.address.isEmpty() ? item.address : "";
            if (item.type != null && !item.type.isEmpty()) {
                addressText = addressText.isEmpty() ? item.type : addressText + " · " + item.type;
            }
            address.setText(addressText);

            distance.setText(item.distance != null && !item.distance.isEmpty() ? item.distance : "--");
            applyDistanceColor(item.distance);

            if (nearestBadge != null) {
                nearestBadge.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
            }

            if (btnFavorite != null) {
                boolean isFav = favoriteNames != null && item.name != null && favoriteNames.contains(item.name);
                btnFavorite.setImageResource(isFav ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
                btnFavorite.setTag(isFav);
                if (db != null && executor != null) {
                    final String centerName = item.name;
                    btnFavorite.setOnClickListener(v -> {
                        boolean currentlyFav = btnFavorite.getTag() != null && (boolean) btnFavorite.getTag();
                        executor.execute(() -> {
                            if (currentlyFav) {
                                db.favoriteCenterDao().deleteByName(centerName);
                            } else {
                                db.favoriteCenterDao().insert(new FavoriteCenter(centerName, item.latitude, item.longitude));
                            }
                        });
                        btnFavorite.setImageResource(!currentlyFav ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
                        btnFavorite.setTag(!currentlyFav);
                        if (favoriteListener != null) favoriteListener.onFavoriteToggled(item, !currentlyFav);
                    });
                }
            }

            bindStatusBadge(item.status);

            if (capacityBar != null && capacityText != null) {
                if (item.capacity > 0) {
                    int pct = Math.min((item.occupied * 100) / item.capacity, 100);
                    capacityBar.setProgress(pct);
                    capacityText.setText(item.occupied + "/" + item.capacity);
                    int color;
                    if (pct >= 85) color = 0xFFE53935;
                    else if (pct >= 60) color = 0xFFFB8C00;
                    else color = 0xFF43A047;
                    capacityBar.setProgressTintList(android.content.res.ColorStateList.valueOf(color));
                    capacityBar.setVisibility(View.VISIBLE);
                    capacityText.setVisibility(View.VISIBLE);
                } else {
                    capacityBar.setVisibility(View.GONE);
                    capacityText.setVisibility(View.GONE);
                }
            }

            if (btnViewDetails != null) {
                btnViewDetails.setOnClickListener(v -> {
                    if (detailsListener != null) detailsListener.onViewDetails(item);
                });
            }

            if (btnDirections != null) {
                if (item.latitude != 0 || item.longitude != 0) {
                    btnDirections.setVisibility(View.VISIBLE);
                    btnDirections.setOnClickListener(v -> {
                        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + item.latitude + "," + item.longitude);
                        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                        mapIntent.setPackage("com.google.android.apps.maps");
                        if (mapIntent.resolveActivity(itemView.getContext().getPackageManager()) != null) {
                            itemView.getContext().startActivity(mapIntent);
                        } else {
                            Uri webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + item.latitude + "," + item.longitude);
                            itemView.getContext().startActivity(new Intent(Intent.ACTION_VIEW, webUri));
                        }
                    });
                } else {
                    btnDirections.setVisibility(View.GONE);
                }
            }

            itemView.setOnClickListener(v -> {
                ScaleAnimation scale = new ScaleAnimation(1f, 0.95f, 1f, 0.95f,
                        Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                scale.setDuration(80);
                scale.setRepeatMode(Animation.REVERSE);
                scale.setRepeatCount(1);
                v.startAnimation(scale);
                v.postDelayed(() -> {
                    if (listener != null) listener.onEvacClick(item);
                }, 160);
            });
        }

        private void applyDistanceColor(String distanceStr) {
            try {
                String distStr = (distanceStr != null ? distanceStr : "")
                        .replace(" km", "").trim();
                double dist = Double.parseDouble(distStr);
                int color;
                if (dist < 1.0) {
                    color = ContextCompat.getColor(itemView.getContext(), R.color.success_green);
                } else if (dist <= 3.0) {
                    color = ContextCompat.getColor(itemView.getContext(), R.color.warning_orange);
                } else {
                    color = ContextCompat.getColor(itemView.getContext(), android.R.color.holo_red_light);
                }
                distance.setTextColor(color);
            } catch (Exception e) {
                distance.setTextColor(ContextCompat.getColor(itemView.getContext(), android.R.color.holo_red_light));
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
