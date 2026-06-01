package com.example.instacare;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.data.local.DisasterGuide;
import java.util.List;

public class AdminDisasterGuidesAdapter extends RecyclerView.Adapter<AdminDisasterGuidesAdapter.ViewHolder> {

    private List<DisasterGuide> guides;
    private final Listener listener;

    public interface Listener {
        void onEdit(DisasterGuide guide);
        void onDelete(DisasterGuide guide);
    }

    public AdminDisasterGuidesAdapter(List<DisasterGuide> guides, Listener listener) {
        this.guides = guides;
        this.listener = listener;
    }

    public void updateList(List<DisasterGuide> newList) {
        this.guides = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_disaster_guide, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DisasterGuide guide = guides.get(position);
        holder.tvTitle.setText(guide.title);
        holder.tvCategory.setText(guide.disasterType);

        holder.btnEdit.setOnClickListener(v -> listener.onEdit(guide));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(guide));
        holder.itemView.setOnClickListener(v -> listener.onEdit(guide));
    }

    @Override
    public int getItemCount() {
        return guides != null ? guides.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCategory;
        ImageButton btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvGuideTitle);
            tvCategory = itemView.findViewById(R.id.tvGuideCategory);
            btnEdit = itemView.findViewById(R.id.btnEditGuide);
            btnDelete = itemView.findViewById(R.id.btnDeleteGuide);
        }
    }
}
