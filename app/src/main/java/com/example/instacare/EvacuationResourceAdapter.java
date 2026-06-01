package com.example.instacare;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.data.local.EvacuationResource;
import java.util.List;

/**
 * Tier 2 — Feature A: Adapter for evacuation supply resources.
 */
public class EvacuationResourceAdapter extends RecyclerView.Adapter<EvacuationResourceAdapter.VH> {

    private List<EvacuationResource> items;

    public EvacuationResourceAdapter(List<EvacuationResource> items) {
        this.items = items;
    }

    public void setItems(List<EvacuationResource> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_evacuation_resource, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        EvacuationResource r = items.get(position);
        h.tvType.setText(r.resourceType);
        h.tvQty.setText(r.quantity + " " + (r.unit != null ? r.unit : ""));
        if (r.isAvailable == 1) {
            h.tvStatus.setText("Available");
            h.tvStatus.getBackground().setTint(Color.parseColor("#10B981"));
        } else {
            h.tvStatus.setText("Depleted");
            h.tvStatus.getBackground().setTint(Color.parseColor("#EF4444"));
        }
        // Pick icon based on resource type
        int iconRes = R.drawable.ic_archive;
        if (r.resourceType != null) {
            String t = r.resourceType.toLowerCase();
            if (t.contains("food"))     iconRes = R.drawable.ic_archive;
            else if (t.contains("water"))   iconRes = R.drawable.ic_cloud;
            else if (t.contains("medicine") || t.contains("kit"))
                                            iconRes = R.drawable.ic_alert_triangle;
        }
        h.ivIcon.setImageResource(iconRes);
    }

    @Override
    public int getItemCount() { return items != null ? items.size() : 0; }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvType, tvQty, tvStatus;
        ImageView ivIcon;
        VH(@NonNull View v) {
            super(v);
            tvType   = v.findViewById(R.id.tvResourceType);
            tvQty    = v.findViewById(R.id.tvResourceQuantity);
            tvStatus = v.findViewById(R.id.tvResourceStatus);
            ivIcon   = v.findViewById(R.id.ivResourceIcon);
        }
    }
}
