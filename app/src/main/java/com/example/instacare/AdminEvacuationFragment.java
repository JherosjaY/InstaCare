package com.example.instacare;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.data.local.AppDatabase;
import com.example.instacare.data.local.EvacuationCenter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AdminEvacuationFragment extends Fragment {

    private RecyclerView rvEvacuation;
    private AppDatabase db;
    private View emptyState;
    private List<EvacuationCenter> currentList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_evacuation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        db = AppDatabase.getDatabase(requireContext());
        rvEvacuation = view.findViewById(R.id.rvEvacuation);
        emptyState = view.findViewById(R.id.emptyStateEvacuation);

        rvEvacuation.setLayoutManager(new LinearLayoutManager(requireContext()));

        view.findViewById(R.id.fabAddEvacuation).setOnClickListener(v -> showAddEditDialog(null));

        // Open Drawer from internal header
        View btnMenu = view.findViewById(R.id.btnMenu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                if (getActivity() instanceof AdminDashboardActivity) {
                    ((AdminDashboardActivity) getActivity()).openDrawer();
                }
            });
        }

        loadEvacuationCenters();
    }

    private void loadEvacuationCenters() {
        new Thread(() -> {
            List<EvacuationCenter> centers = db.evacuationCenterDao().getAllCenters();
            final List<EvacuationCenter> result = centers != null ? centers : new ArrayList<>();
            currentList = result;
            requireActivity().runOnUiThread(() -> {
                updateList(result);
                if (result.isEmpty()) {
                    rvEvacuation.setVisibility(View.GONE);
                    emptyState.setVisibility(View.VISIBLE);
                } else {
                    rvEvacuation.setVisibility(View.VISIBLE);
                    emptyState.setVisibility(View.GONE);
                }
            });
        }).start();
    }

    private void updateList(List<EvacuationCenter> list) {
        // Simple adapter using item views
        rvEvacuation.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_simple, parent, false);
                return new RecyclerView.ViewHolder(v) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                EvacuationCenter center = list.get(position);
                android.widget.TextView tvTitle = holder.itemView.findViewById(R.id.tvItemTitle);
                android.widget.TextView tvSubtitle = holder.itemView.findViewById(R.id.tvItemSubtitle);
                tvTitle.setText(center.name);
                tvSubtitle.setText(center.address + " • " + center.type);

                holder.itemView.findViewById(R.id.btnEdit).setOnClickListener(v -> showAddEditDialog(center));
                holder.itemView.findViewById(R.id.btnDelete).setOnClickListener(v -> confirmDelete(center));
            }

            @Override
            public int getItemCount() { return list.size(); }
        });
    }

    private void showAddEditDialog(@Nullable EvacuationCenter existing) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_evacuation, null);

        android.widget.TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        dialogTitle.setText(existing == null ? "Add Evacuation Center" : "Edit Evacuation Center");

        com.google.android.material.textfield.TextInputEditText etName = dialogView.findViewById(R.id.etEvacName);
        com.google.android.material.textfield.TextInputEditText etAddress = dialogView.findViewById(R.id.etEvacAddress);
        com.google.android.material.textfield.TextInputEditText etLat = dialogView.findViewById(R.id.etEvacLat);
        com.google.android.material.textfield.TextInputEditText etLng = dialogView.findViewById(R.id.etEvacLng);
        com.google.android.material.textfield.TextInputEditText etType = dialogView.findViewById(R.id.etEvacType);

        if (existing != null) {
            etName.setText(existing.name);
            etAddress.setText(existing.address);
            etLat.setText(String.valueOf(existing.latitude));
            etLng.setText(String.valueOf(existing.longitude));
            etType.setText(existing.type);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.GradientDrawable() {{
                setColor(android.graphics.Color.WHITE);
                setCornerRadius(32f);
            }});
        }

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnSave).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String address = etAddress.getText().toString().trim();
            String latStr = etLat.getText().toString().trim();
            String lngStr = etLng.getText().toString().trim();
            String type = etType.getText().toString().trim();

            if (name.isEmpty()) { Toast.makeText(requireContext(), "Name required", Toast.LENGTH_SHORT).show(); return; }

            double lat = 0, lng = 0;
            try {
                if (!latStr.isEmpty()) lat = Double.parseDouble(latStr);
                if (!lngStr.isEmpty()) lng = Double.parseDouble(lngStr);
            } catch (NumberFormatException e) {
                Toast.makeText(requireContext(), "Invalid coordinates", Toast.LENGTH_SHORT).show();
                return;
            }

            final EvacuationCenter center;
            if (existing != null) {
                center = existing;
                center.name = name;
                center.address = address;
                center.latitude = lat;
                center.longitude = lng;
                center.type = type.isEmpty() ? "Evacuation Center" : type;
            } else {
                center = new EvacuationCenter(UUID.randomUUID().toString(), name, address,
                        lat, lng, type.isEmpty() ? "Evacuation Center" : type, "Open");
            }

            new Thread(() -> {
                db.evacuationCenterDao().insert(center);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Evacuation center saved", Toast.LENGTH_SHORT).show();
                    loadEvacuationCenters();
                });
            }).start();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void confirmDelete(EvacuationCenter center) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Center")
                .setMessage("Remove \"" + center.name + "\"?")
                .setPositiveButton("Delete", (d, w) -> new Thread(() -> {
                    db.evacuationCenterDao().delete(center);
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Center deleted", Toast.LENGTH_SHORT).show();
                        loadEvacuationCenters();
                    });
                }).start())
                .setNegativeButton("Cancel", null)
                .show();
    }
}
