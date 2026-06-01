package com.example.instacare;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.data.local.EvacuationEndorsement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EvacuationEndorsementAdapter extends RecyclerView.Adapter<EvacuationEndorsementAdapter.ViewHolder> {
    private List<EvacuationEndorsement> items;
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(EvacuationEndorsement item);
    }

    public EvacuationEndorsementAdapter(List<EvacuationEndorsement> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    public void setItems(List<EvacuationEndorsement> items) {
        this.items = items;
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
        EvacuationEndorsement e = items.get(position);
        
        holder.tvName.setText(e.patientName);
        holder.tvAddress.setText(e.address);
        
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        holder.tvDate.setText(sdf.format(new Date(e.createdAt)));

        holder.tvStatus.setText(e.status);
        
        // Premium Badge Logic
        GradientDrawable badge = new GradientDrawable();
        badge.setCornerRadius(50);

        switch (e.status) {
            case "PENDING":
                badge.setColor(Color.parseColor("#FEF3C7")); // Amber-100
                holder.tvStatus.setTextColor(Color.parseColor("#92400E")); // Amber-800
                break;
            case "ASSIGNED":
            case "ARRIVED":
            case "RESOLVED":
                badge.setColor(Color.parseColor("#D1FAE5")); // Green-100
                holder.tvStatus.setTextColor(Color.parseColor("#065F46")); // Green-800
                break;
            default:
                badge.setColor(Color.parseColor("#F3F4F6")); // Gray-100
                holder.tvStatus.setTextColor(Color.parseColor("#374151")); // Gray-800
                break;
        }
        holder.tvStatus.setBackground(badge);

        // Icon Frame Tint
        holder.statusIconFrame.setBackgroundResource(R.drawable.bg_rounded_soft);
        holder.statusIconFrame.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFF7ED")));
        holder.statusIcon.setImageResource(R.drawable.ic_storm);
        holder.statusIcon.setColorFilter(Color.parseColor("#EA580C"), android.graphics.PorterDuff.Mode.SRC_IN);

        holder.itemView.setOnClickListener(v -> listener.onItemClick(e));
        
        // Admin doesn't need chat button inside adapter for now or hides it
        if (holder.btnChat != null) holder.btnChat.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() { return items != null ? items.size() : 0; }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAddress, tvStatus, tvDate;
        View statusIconFrame;
        android.widget.ImageView statusIcon;
        View btnChat;

        ViewHolder(View v) {
            super(v);
            tvName = v.findViewById(R.id.tvPurpose); // Mapping purpose to Name for Admin list
            tvAddress = v.findViewById(R.id.tvHospital); // Mapping hospital to address for Admin list
            tvStatus = v.findViewById(R.id.tvStatus);
            tvDate = v.findViewById(R.id.tvDate);
            statusIconFrame = v.findViewById(R.id.statusIconFrame);
            statusIcon = v.findViewById(R.id.statusIcon);
            btnChat = v.findViewById(R.id.btnChat);
        }
    }
}
