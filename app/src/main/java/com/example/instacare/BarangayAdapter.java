package com.example.instacare;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.data.local.BarangayZone;
import java.util.List;

public class BarangayAdapter extends RecyclerView.Adapter<BarangayAdapter.ViewHolder> {

    private final List<BarangayZone> barangays;
    private final OnBarangayClickListener listener;

    public interface OnBarangayClickListener {
        void onBarangayClick(BarangayZone barangay);
    }

    public BarangayAdapter(List<BarangayZone> barangays, OnBarangayClickListener listener) {
        this.barangays = barangays;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_community_selection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BarangayZone barangay = barangays.get(position);
        holder.tvName.setText(barangay.name);
        holder.itemView.setOnClickListener(v -> listener.onBarangayClick(barangay));
    }

    @Override
    public int getItemCount() {
        return barangays.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;

        ViewHolder(View view) {
            super(view);
            tvName = view.findViewById(R.id.tvBarangayName);
        }
    }
}
