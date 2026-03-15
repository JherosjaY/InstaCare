package com.example.instacare;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.data.local.Guide;
import java.util.List;

public class AdminGuidesAdapter extends RecyclerView.Adapter<AdminGuidesAdapter.ViewHolder> {

    public interface Listener {
        void onEdit(Guide guide);
        void onDelete(Guide guide);
    }

    private List<Guide> guides;
    private final Listener listener;

    public AdminGuidesAdapter(List<Guide> guides, Listener listener) {
        this.guides = guides;
        this.listener = listener;
    }

    public void updateList(List<Guide> newList) {
        this.guides = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_guide, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Guide g = guides.get(position);
        h.tvTitle.setText(g.title != null ? g.title : "Untitled");
        h.tvCategory.setText(g.category != null ? g.category : "Uncategorized");
        h.btnEdit.setOnClickListener(v -> listener.onEdit(g));
        h.btnDelete.setOnClickListener(v -> listener.onDelete(g));
    }

    @Override
    public int getItemCount() { return guides.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvCategory;
        ImageButton btnEdit, btnDelete;
        ViewHolder(@NonNull View v) {
            super(v);
            tvTitle = v.findViewById(R.id.tvGuideTitle);
            tvCategory = v.findViewById(R.id.tvGuideCategory);
            btnEdit = v.findViewById(R.id.btnEditGuide);
            btnDelete = v.findViewById(R.id.btnDeleteGuide);
        }
    }
}
