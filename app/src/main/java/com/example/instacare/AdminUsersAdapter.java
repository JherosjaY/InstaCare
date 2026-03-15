package com.example.instacare;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.data.local.User;
import com.google.android.material.button.MaterialButton;
import java.util.List;

public class AdminUsersAdapter extends RecyclerView.Adapter<AdminUsersAdapter.ViewHolder> {

    public interface Listener {
        void onSuspend(User user);
        void onDelete(User user);
        void onUserClick(User user);
    }

    private List<User> users;
    private final Listener listener;

    public AdminUsersAdapter(List<User> users, Listener listener) {
        this.users = users;
        this.listener = listener;
    }

    public void updateList(List<User> newList) {
        this.users = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_user, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        User u = users.get(position);
        String initial = (u.username != null && !u.username.isEmpty())
                ? String.valueOf(u.username.charAt(0)).toUpperCase() : "?";
        h.tvInitial.setText(initial);
        h.tvName.setText(u.username != null ? u.username : "Unknown");
        h.tvEmail.setText(u.email != null ? u.email : "");
        
        h.containerMain.setOnClickListener(v -> listener.onUserClick(u));

        android.content.Context ctx = h.itemView.getContext();
        int colorRed = androidx.core.content.ContextCompat.getColor(ctx, R.color.emergency_red);
        int colorGreen = androidx.core.content.ContextCompat.getColor(ctx, R.color.success_green);
        int colorOrange = androidx.core.content.ContextCompat.getColor(ctx, R.color.warning_orange);

        if (u.isSuspended) {
            h.tvStatus.setText("Suspended");
            h.tvStatus.setTextColor(colorRed);
            h.tvStatus.setBackgroundTintList(null);
            h.btnSuspend.setText("Unsuspend");
            h.btnSuspend.setStrokeColor(android.content.res.ColorStateList.valueOf(colorGreen));
            h.btnSuspend.setTextColor(colorGreen);
        } else if (!u.isVerified) {
            h.tvStatus.setText("Unverified");
            h.tvStatus.setTextColor(colorOrange);
            h.btnSuspend.setText("Suspend");
            h.btnSuspend.setStrokeColor(android.content.res.ColorStateList.valueOf(colorOrange));
            h.btnSuspend.setTextColor(colorOrange);
        } else {
            h.tvStatus.setText("Active ✓");
            h.tvStatus.setTextColor(colorGreen);
            h.btnSuspend.setText("Suspend");
            h.btnSuspend.setStrokeColor(android.content.res.ColorStateList.valueOf(colorOrange));
            h.btnSuspend.setTextColor(colorOrange);
        }

        h.btnSuspend.setOnClickListener(v -> listener.onSuspend(u));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(u));
    }

    @Override
    public int getItemCount() { return users.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvInitial, tvName, tvEmail, tvStatus;
        MaterialButton btnSuspend, btnDelete;
        View containerMain;

        ViewHolder(@NonNull View v) {
            super(v);
            tvInitial = v.findViewById(R.id.tvUserInitial);
            tvName = v.findViewById(R.id.tvUserName);
            tvEmail = v.findViewById(R.id.tvUserEmail);
            tvStatus = v.findViewById(R.id.tvUserStatus);
            btnSuspend = v.findViewById(R.id.btnSuspendUser);
            btnDelete = v.findViewById(R.id.btnDeleteUser);
            containerMain = v.findViewById(R.id.containerMain);
        }
    }
}
