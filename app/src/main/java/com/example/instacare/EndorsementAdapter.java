package com.example.instacare;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.instacare.data.local.Endorsement;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EndorsementAdapter extends RecyclerView.Adapter<EndorsementAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(Endorsement endorsement);
    }

    private List<Endorsement> items;
    private OnItemClickListener listener;

    public EndorsementAdapter(List<Endorsement> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void updateData(List<Endorsement> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_endorsement, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Endorsement e = items.get(position);

        holder.tvPurpose.setText(e.purpose);
        holder.tvHospital.setText(e.hospitalName);

        // Set dynamic icon and background based on purpose
        int iconResId;
        
        String purpose = e.purpose != null ? e.purpose : "";
        switch (purpose) {
            case "Medical Assistance":
                iconResId = R.drawable.ic_heart_pulse;
                break;
            case "Hospital Referral":
                iconResId = R.drawable.ic_hospital;
                break;
            case "Lab / Check-up":
                iconResId = R.drawable.ic_ecg_pulse_line;
                break;
            default:
                iconResId = R.drawable.ic_endorsement;
                break;
        }
        holder.statusIconFrame.setBackgroundResource(R.drawable.bg_icon_red);
        holder.statusIcon.setImageResource(iconResId);
        holder.statusIcon.setColorFilter(ContextCompat.getColor(holder.itemView.getContext(), R.color.primary), android.graphics.PorterDuff.Mode.SRC_IN);

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        holder.tvDate.setText(sdf.format(new Date(e.createdAt)));

        // Status badge styling
        holder.tvStatus.setText(e.status);
        GradientDrawable badge = new GradientDrawable();
        badge.setCornerRadius(50);

        switch (e.status) {
            case "PENDING":
                badge.setColor(Color.parseColor("#FEF3C7")); // Amber-100
                holder.tvStatus.setTextColor(Color.parseColor("#92400E")); // Amber-800
                break;
            case "APPROVED":
                badge.setColor(Color.parseColor("#D1FAE5")); // Green-100
                holder.tvStatus.setTextColor(Color.parseColor("#065F46")); // Green-800
                break;
            case "DECLINED":
                badge.setColor(Color.parseColor("#FEE2E2")); // Red-100
                holder.tvStatus.setTextColor(Color.parseColor("#991B1B")); // Red-800
                break;
        }
        holder.tvStatus.setBackground(badge);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(e);
        });
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPurpose, tvHospital, tvDate, tvStatus;
        FrameLayout statusIconFrame;
        android.widget.ImageView statusIcon;

        ViewHolder(@NonNull View v) {
            super(v);
            tvPurpose = v.findViewById(R.id.tvPurpose);
            tvHospital = v.findViewById(R.id.tvHospital);
            tvDate = v.findViewById(R.id.tvDate);
            tvStatus = v.findViewById(R.id.tvStatus);
            statusIconFrame = v.findViewById(R.id.statusIconFrame);
            statusIcon = v.findViewById(R.id.statusIcon);
        }
    }
}
