package com.example.instacare;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.data.local.ActivityLog;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminLogsAdapter extends RecyclerView.Adapter<AdminLogsAdapter.ViewHolder> {

    public interface OnLogClickListener {
        void onLogClick(ActivityLog log);
    }

    private List<ActivityLog> logs;
    private OnLogClickListener listener;

    public AdminLogsAdapter(List<ActivityLog> logs) {
        this(logs, null);
    }

    public AdminLogsAdapter(List<ActivityLog> logs, OnLogClickListener listener) {
        this.logs = logs;
        this.listener = listener;
    }

    public void updateList(List<ActivityLog> newList) {
        this.logs = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_log, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        ActivityLog log = logs.get(position);
        h.tvType.setText(log.type != null ? log.type : "LOG");

        // Color badge by type
        int color;
        if ("SOS".equals(log.type)) color = 0xFFE11D48;
        else if ("LOGIN".equals(log.type)) color = 0xFF6366F1;
        else if ("REGISTER".equals(log.type)) color = 0xFF16A34A;
        else color = 0xFF6B7280;
        h.tvType.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));

        h.tvDescription.setText(log.description != null ? log.description : "");
        
        if (log.userEmail != null && !log.userEmail.isEmpty()) {
            h.tvUser.setVisibility(View.VISIBLE);
            h.tvUser.setText(log.userEmail);
        } else {
            h.tvUser.setVisibility(View.GONE);
        }

        String time = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                .format(new Date(log.timestamp));
        h.tvTimestamp.setText(time);

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onLogClick(log);
        });
    }

    @Override
    public int getItemCount() { return logs.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvDescription, tvTimestamp, tvUser;
        ViewHolder(@NonNull View v) {
            super(v);
            tvType = v.findViewById(R.id.tvLogType);
            tvDescription = v.findViewById(R.id.tvLogDescription);
            tvUser = v.findViewById(R.id.tvLogUser);
            tvTimestamp = v.findViewById(R.id.tvLogTimestamp);
        }
    }
}
