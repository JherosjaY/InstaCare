package com.example.instacare;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.data.local.Hospital;
import java.util.List;

public class HospitalAdapter extends RecyclerView.Adapter<HospitalAdapter.HospitalViewHolder> {

    private List<Hospital> hospitals;
    private OnHospitalClickListener listener;

    public interface OnHospitalClickListener {
        void onHospitalClick(Hospital hospital);
    }

    public HospitalAdapter(List<Hospital> hospitals, OnHospitalClickListener listener) {
        this.hospitals = hospitals;
        this.listener = listener;
    }

    public void setHospitals(List<Hospital> hospitals) {
        this.hospitals = hospitals;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HospitalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new HospitalViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(
                        R.layout.item_hospital, parent, false
                ),
                listener
        );
    }

    @Override
    public void onBindViewHolder(@NonNull HospitalViewHolder holder, int position) {
        holder.bind(hospitals.get(position));
    }

    @Override
    public int getItemCount() {
        return hospitals != null ? hospitals.size() : 0;
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

            // Load Image with Glide
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
                if (listener != null) listener.onHospitalClick(item);
            });

            // Set color based on distance
            try {
                String distanceStr = item.distance.replace(" miles", "").replace(" km", "").trim();
                double dist = Double.parseDouble(distanceStr);
                
                int color;
                if (dist < 2.0) { // Adjusted for miles
                    color = androidx.core.content.ContextCompat.getColor(itemView.getContext(), R.color.success_green);
                } else if (dist <= 5.0) {
                    color = androidx.core.content.ContextCompat.getColor(itemView.getContext(), R.color.warning_orange);
                } else {
                    color = androidx.core.content.ContextCompat.getColor(itemView.getContext(), R.color.emergency_red);
                }
                distance.setTextColor(color);
            } catch (Exception e) {
                // Default to secondary text color if parsing fails
                distance.setTextColor(androidx.core.content.ContextCompat.getColor(itemView.getContext(), R.color.text_secondary));
            }
        }
    }
}
