package com.example.instacare;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.data.local.EvacuationAnnouncement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Tier 2 — Feature C: Adapter for evacuation announcements.
 */
public class EvacuationAnnouncementAdapter extends RecyclerView.Adapter<EvacuationAnnouncementAdapter.VH> {

    private List<EvacuationAnnouncement> items;

    public EvacuationAnnouncementAdapter(List<EvacuationAnnouncement> items) {
        this.items = items;
    }

    public void setItems(List<EvacuationAnnouncement> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_evacuation_announcement, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        EvacuationAnnouncement a = items.get(position);
        h.tvTitle.setText(a.title);
        h.tvMessage.setText(a.message);
        h.tvCenter.setText(a.centerName != null && !a.centerName.isEmpty()
            ? a.centerName : "All Centers");
        h.tvTime.setText(formatTime(a.timestamp));

        // Priority badge + stripe color
        String priority = a.priority != null ? a.priority : "INFO";
        h.tvPriority.setText(priority);
        int color;
        switch (priority.toUpperCase()) {
            case "URGENT":  color = Color.parseColor("#EF4444"); break;
            case "WARNING": color = Color.parseColor("#F97316"); break;
            default:        color = Color.parseColor("#3B82F6"); break;
        }
        h.tvPriority.getBackground().setTint(color);
        if (h.viewStripe != null) h.viewStripe.getBackground().setTint(color);
    }

    @Override
    public int getItemCount() { return items != null ? items.size() : 0; }

    private String formatTime(long millis) {
        long diff = System.currentTimeMillis() - millis;
        if (diff < 60_000)   return "Just now";
        if (diff < 3600_000) return (diff / 60_000) + "m ago";
        if (diff < 86400_000) return (diff / 3600_000) + "h ago";
        return new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(new Date(millis));
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvCenter, tvTime, tvPriority;
        View viewStripe;
        VH(@NonNull View v) {
            super(v);
            tvTitle    = v.findViewById(R.id.tvAnnouncementTitle);
            tvMessage  = v.findViewById(R.id.tvAnnouncementMessage);
            tvCenter   = v.findViewById(R.id.tvAnnouncementCenter);
            tvTime     = v.findViewById(R.id.tvAnnouncementTime);
            tvPriority = v.findViewById(R.id.tvAnnouncementPriority);
            viewStripe = v.findViewById(R.id.viewPriorityStripe);
        }
    }
}
