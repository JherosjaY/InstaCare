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

    public interface OnChatClickListener {
        void onChatClick(Endorsement endorsement);
    }

    private List<Endorsement> items;
    private int currentUserId;
    private String currentUserRole;
    private OnItemClickListener listener;
    private OnChatClickListener chatListener;

    public EndorsementAdapter(List<Endorsement> items, int currentUserId, String currentUserRole, OnItemClickListener listener, OnChatClickListener chatListener) {
        this.items = items;
        this.currentUserId = currentUserId;
        this.currentUserRole = currentUserRole;
        this.listener = listener;
        this.chatListener = chatListener;
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

        // Unread Badge Logic
        com.example.instacare.data.local.AppDatabase db = com.example.instacare.data.local.AppDatabase.getDatabase(holder.itemView.getContext());
        new Thread(() -> {
            int unreadCount;
            if (currentUserRole.equals("ADMIN") || currentUserRole.equals("BARANGAY")) {
                unreadCount = db.chatMessageDao().getUnreadCountForEndorsement(e.id, currentUserId, currentUserRole);
            } else {
                // For regular users, sum unread from both roles
                unreadCount = db.chatMessageDao().getUnreadCountForEndorsement(e.id, currentUserId, "ADMIN") +
                             db.chatMessageDao().getUnreadCountForEndorsement(e.id, currentUserId, "BARANGAY");
            }
            
            holder.itemView.post(() -> {
                if (unreadCount > 0) {
                    holder.tvUnreadBadge.setVisibility(View.VISIBLE);
                    holder.tvUnreadBadge.setText(String.valueOf(unreadCount));
                } else {
                    holder.tvUnreadBadge.setVisibility(View.GONE);
                }
            });
        }).start();

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
            case "flood":
                iconResId = R.drawable.ic_storm;
                break;
            case "earthquake":
                iconResId = R.drawable.ic_alert_triangle;
                break;
            case "fire":
                iconResId = R.drawable.ic_fire;
                break;
            case "typhoon":
                iconResId = R.drawable.ic_storm;
                break;
            default:
                iconResId = R.drawable.ic_endorsement;
                break;
        }
        // Dynamic Theme for Icon
        boolean isBrgy = "BARANGAY".equals(currentUserRole);
        int themeColor = isBrgy ? 
            holder.itemView.getContext().getResources().getColor(R.color.barangay_primary) : 
            holder.itemView.getContext().getResources().getColor(R.color.primary);
        
        holder.statusIconFrame.setBackgroundResource(isBrgy ? R.drawable.bg_rounded_soft : R.drawable.bg_icon_red);
        if (isBrgy) {
            holder.statusIconFrame.setBackgroundTintList(android.content.res.ColorStateList.valueOf(themeColor));
        }
        
        holder.statusIcon.setImageResource(iconResId);
        holder.statusIcon.setColorFilter(themeColor, android.graphics.PorterDuff.Mode.SRC_IN);
        if (isBrgy) holder.statusIcon.setColorFilter(Color.WHITE, android.graphics.PorterDuff.Mode.SRC_IN);

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

        // Dynamic Ripple Color
        if (holder.itemView instanceof com.google.android.material.card.MaterialCardView) {
            com.google.android.material.card.MaterialCardView card = (com.google.android.material.card.MaterialCardView) holder.itemView;
            int rippleColor = isBrgy ? 
                holder.itemView.getContext().getResources().getColor(R.color.blue_bg) : 
                holder.itemView.getContext().getResources().getColor(R.color.red_bg);
            card.setRippleColor(android.content.res.ColorStateList.valueOf(rippleColor));
        }

        holder.btnChat.setImageResource(R.drawable.ic_chat_bubble);
        if (isBrgy) {
            holder.btnChat.setBackgroundResource(R.drawable.bg_icon_blue);
            holder.btnChat.setImageTintList(android.content.res.ColorStateList.valueOf(holder.itemView.getContext().getResources().getColor(R.color.barangay_primary)));
        } else {
            holder.btnChat.setBackgroundResource(R.drawable.bg_icon_red);
            holder.btnChat.setImageTintList(android.content.res.ColorStateList.valueOf(holder.itemView.getContext().getResources().getColor(R.color.primary)));
        }

        holder.btnChat.setOnClickListener(v -> {
            if (currentUserRole.equals("BARANGAY")) {
                // Staff opens their own thread
                if (chatListener != null) chatListener.onChatClick(e);
            } else if (currentUserRole.equals("USER")) {
                // User opens direct chat with Barangay
                openChatActivity(holder.itemView.getContext(), e, "BARANGAY");
            }
        });

        // Hide chat for ADMIN
        if (currentUserRole.equals("ADMIN")) {
            holder.btnChat.setVisibility(View.GONE);
        } else {
            holder.btnChat.setVisibility(View.VISIBLE);
        }
    }


    private void openChatActivity(android.content.Context context, Endorsement e, String role) {
        android.content.Intent intent = new android.content.Intent(context, ChatActivity.class);
        intent.putExtra("ENDORSEMENT_ID", e.id);
        intent.putExtra("OTHER_USER_ID", 1000); // Staff placeholder ID
        intent.putExtra("CASE_REF", e.caseRef);
        intent.putExtra("CONVERSATION_ROLE", role);
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvPurpose, tvHospital, tvDate, tvStatus, tvUnreadBadge;
        FrameLayout statusIconFrame;
        android.widget.ImageView statusIcon;
        android.widget.ImageButton btnChat;

        ViewHolder(@NonNull View v) {
            super(v);
            tvPurpose = v.findViewById(R.id.tvPurpose);
            tvHospital = v.findViewById(R.id.tvHospital);
            tvDate = v.findViewById(R.id.tvDate);
            tvStatus = v.findViewById(R.id.tvStatus);
            statusIconFrame = v.findViewById(R.id.statusIconFrame);
            statusIcon = v.findViewById(R.id.statusIcon);
            btnChat = v.findViewById(R.id.btnChat);
            tvUnreadBadge = v.findViewById(R.id.tvUnreadBadge);
        }
    }
}
