package com.example.instacare;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.data.local.EmergencyAlert;
import java.util.List;

public class EmergencyAlertAdapter extends RecyclerView.Adapter<EmergencyAlertAdapter.ViewHolder> {

    private List<EmergencyAlert> emergencies;
    private int expandedPosition = -1;

    public EmergencyAlertAdapter(List<EmergencyAlert> emergencies) {
        this.emergencies = emergencies;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_emergency_alert, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EmergencyAlert emergency = emergencies.get(position);
        holder.tvCaseRef.setText(emergency.caseRef != null ? emergency.caseRef : "N/A");
        holder.tvPatientName.setText(emergency.patientName);
        holder.tvLocation.setText(emergency.barangayZone != null ? emergency.barangayZone : "Unknown Zone");
        
        // Role-based Styling (Red for Admin, Blue for Barangay)
        SessionManager sessionManager = SessionManager.getInstance(holder.itemView.getContext());
        boolean isBarangay = sessionManager.getBoolean("IS_BARANGAY", false);
        
        int primaryColor = android.graphics.Color.parseColor(isBarangay ? "#2563EB" : "#DC2626");
        int lightBgColor = android.graphics.Color.parseColor(isBarangay ? "#EFF6FF" : "#FEF2F2");
        int borderColor = android.graphics.Color.parseColor(isBarangay ? "#DBEAFE" : "#FEE2E2");

        holder.cardView.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(lightBgColor));
        holder.cardView.setStrokeColor(android.content.res.ColorStateList.valueOf(borderColor));
        
        holder.iconCircle.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primaryColor));
        holder.tvCaseRef.setTextColor(primaryColor);
        holder.tvStatus.setTextColor(primaryColor);
        holder.tvStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(borderColor));
        
        com.google.android.material.button.MaterialButton btn = (com.google.android.material.button.MaterialButton) holder.btnViewMap;
        
        if (isBarangay) {
            // Filled Blue Button for Barangay
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primaryColor));
            btn.setTextColor(android.graphics.Color.WHITE);
            btn.setIconTint(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
            btn.setRippleColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#40FFFFFF")));
        } else {
            // Text/Red Button for Admin
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
            btn.setTextColor(primaryColor);
            btn.setIconTint(android.content.res.ColorStateList.valueOf(primaryColor));
        }

        // Expansion Logic
        final boolean isExpanded = position == expandedPosition;
        holder.extraInfoContainer.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
        
        // Update horizontal dividers color
        holder.divider1.setBackgroundColor(borderColor);
        if (holder.divider2 != null) holder.divider2.setBackgroundColor(borderColor);

        // Populate Details
        holder.tvLatitude.setText(String.format("%.6f", emergency.latitude));
        holder.tvLongitude.setText(String.format("%.6f", emergency.longitude));
        
        // Mock Personal Info (as requested)
        holder.tvPatientInfo.setText("Age: 25 | Blood: B+");
        holder.tvConditions.setText("No known allergies");

        holder.itemView.setOnClickListener(v -> {
            int prev = expandedPosition;
            expandedPosition = isExpanded ? -1 : position;
            notifyItemChanged(prev);
            notifyItemChanged(position);
        });
        
        holder.btnViewMap.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), SOSTrackActivity.class);
            intent.putExtra("PATIENT_NAME", emergency.patientName);
            intent.putExtra("EMERGENCY_TYPE", emergency.type);
            intent.putExtra("CASE_REF", emergency.caseRef);
            intent.putExtra("LOCATION_ZONE", emergency.barangayZone);
            intent.putExtra("LATITUDE", emergency.latitude);
            intent.putExtra("LONGITUDE", emergency.longitude);
            v.getContext().startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return emergencies.size();
    }

    public void updateData(List<EmergencyAlert> newData) {
        this.emergencies = newData;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvCaseRef, tvPatientName, tvLocation, tvStatus;
        TextView tvLatitude, tvLongitude, tvPatientInfo, tvConditions;
        View btnViewMap, extraInfoContainer, iconCircle, divider1, divider2;
        com.google.android.material.card.MaterialCardView cardView;

        ViewHolder(View view) {
            super(view);
            cardView = (com.google.android.material.card.MaterialCardView) view;
            tvCaseRef = view.findViewById(R.id.tvCaseRef);
            tvPatientName = view.findViewById(R.id.tvPatientName);
            tvLocation = view.findViewById(R.id.tvLocation);
            tvStatus = view.findViewById(R.id.tvStatus);
            btnViewMap = view.findViewById(R.id.btnViewMap);
            
            iconCircle = view.findViewById(R.id.iconCircle);
            divider1 = view.findViewById(R.id.divider1);
            divider2 = view.findViewById(R.id.divider2);
            
            extraInfoContainer = view.findViewById(R.id.extraInfoContainer);
            tvLatitude = view.findViewById(R.id.tvLatitude);
            tvLongitude = view.findViewById(R.id.tvLongitude);
            tvPatientInfo = view.findViewById(R.id.tvPatientInfo);
            tvConditions = view.findViewById(R.id.tvConditions);
        }
    }
}
