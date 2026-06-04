package com.example.instacare;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.data.local.Hospital;
import java.util.List;

public class HospitalAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<Hospital> hospitals;
    private OnHospitalClickListener listener;
    private boolean isLoading = false;

    private static final int VIEW_TYPE_SHIMMER = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    public interface OnHospitalClickListener {
        void onHospitalClick(Hospital hospital);
    }

    public HospitalAdapter(List<Hospital> hospitals, OnHospitalClickListener listener) {
        this.hospitals = hospitals;
        this.listener = listener;
    }

    public void setHospitals(List<Hospital> hospitals) {
        this.hospitals = hospitals;
        isLoading = false;
        notifyDataSetChanged();
    }

    public void setLoading(boolean loading) {
        isLoading = loading;
        if (loading) hospitals = null;
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
        return new HospitalViewHolder(
            LayoutInflater.from(parent.getContext()).inflate(
                R.layout.item_hospital, parent, false
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
            ((HospitalViewHolder) holder).bind(hospitals.get(position));
        }
    }

    @Override
    public int getItemCount() {
        if (isLoading) return 5;
        return hospitals != null ? hospitals.size() : 0;
    }

    static class ShimmerViewHolder extends RecyclerView.ViewHolder {
        ShimmerViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    static class HospitalViewHolder extends RecyclerView.ViewHolder {
        private TextView name, distance, address;
        private android.widget.ImageView image;
        private OnHospitalClickListener listener;

        HospitalViewHolder(@NonNull View itemView, OnHospitalClickListener listener) {
            super(itemView);
            this.listener = listener;
            name = itemView.findViewById(R.id.hospitalName);
            distance = itemView.findViewById(R.id.hospitalDistance);
            address = itemView.findViewById(R.id.hospitalAddress);
            image = itemView.findViewById(R.id.hospitalImage);
        }

        void bind(Hospital item) {
            name.setText(item.name);
            distance.setText(item.distance);
            address.setText(item.address);

            if (item.imageUrl != null && !item.imageUrl.isEmpty()) {
                int resourceId = itemView.getContext().getResources().getIdentifier(item.imageUrl, "drawable", itemView.getContext().getPackageName());
                if (resourceId != 0) {
                    com.bumptech.glide.Glide.with(itemView.getContext())
                        .load(resourceId)
                        .placeholder(R.drawable.ic_hospital_modern)
                        .error(R.drawable.ic_hospital_modern)
                        .into(image);
                } else {
                    com.bumptech.glide.Glide.with(itemView.getContext())
                        .load(item.imageUrl)
                        .placeholder(R.drawable.ic_hospital_modern)
                        .error(R.drawable.ic_hospital_modern)
                        .into(image);
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
                    if (listener != null) listener.onHospitalClick(item);
                }, 160);
            });

            try {
                if (item.distance != null && item.distance.contains(" km")) {
                    String val = item.distance.split(" ")[0];
                    double dist = Double.parseDouble(val);

                    int color;
                    if (dist < 5.0) {
                        color = androidx.core.content.ContextCompat.getColor(itemView.getContext(), R.color.success_green);
                    } else if (dist <= 15.0) {
                        color = androidx.core.content.ContextCompat.getColor(itemView.getContext(), R.color.warning_orange);
                    } else {
                        color = androidx.core.content.ContextCompat.getColor(itemView.getContext(), R.color.emergency_red);
                    }
                    distance.setTextColor(color);
                } else {
                    distance.setTextColor(androidx.core.content.ContextCompat.getColor(itemView.getContext(), R.color.text_secondary));
                }
            } catch (Exception e) {
                distance.setTextColor(androidx.core.content.ContextCompat.getColor(itemView.getContext(), R.color.text_secondary));
            }
        }
    }
}
