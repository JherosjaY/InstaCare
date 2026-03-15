package com.example.instacare;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.data.local.Hospital;
import java.util.List;

public class AdminHospitalsAdapter extends RecyclerView.Adapter<AdminHospitalsAdapter.ViewHolder> {

    public interface Listener {
        void onEdit(Hospital hospital);
        void onDelete(Hospital hospital);
    }

    private List<Hospital> hospitals;
    private final Listener listener;

    public AdminHospitalsAdapter(List<Hospital> hospitals, Listener listener) {
        this.hospitals = hospitals;
        this.listener = listener;
    }

    public void updateList(List<Hospital> newList) {
        this.hospitals = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_hospital, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Hospital hosp = hospitals.get(position);
        h.tvName.setText(hosp.name != null ? hosp.name : "Unknown Hospital");
        h.tvCity.setText(hosp.address != null ? hosp.address : "Unknown Address");
        h.btnEdit.setOnClickListener(v -> listener.onEdit(hosp));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(hosp));
    }

    @Override
    public int getItemCount() { return hospitals.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCity;
        ImageButton btnEdit, btnDelete;
        ViewHolder(@NonNull View v) {
            super(v);
            tvName = v.findViewById(R.id.tvHospitalName);
            tvCity = v.findViewById(R.id.tvHospitalCity);
            btnEdit = v.findViewById(R.id.btnEditHospital);
            btnDelete = v.findViewById(R.id.btnDeleteHospital);
        }
    }
}
