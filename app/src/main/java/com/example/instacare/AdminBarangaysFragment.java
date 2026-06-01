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
import com.example.instacare.data.local.BarangayZone;
import java.util.ArrayList;
import java.util.List;

public class AdminBarangaysFragment extends Fragment {

    private RecyclerView rvBarangays;
    private AppDatabase db;
    private View emptyState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_barangays, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        db = AppDatabase.getDatabase(requireContext());
        rvBarangays = view.findViewById(R.id.rvBarangays);
        emptyState = view.findViewById(R.id.emptyStateBarangays);

        rvBarangays.setLayoutManager(new LinearLayoutManager(requireContext()));

        view.findViewById(R.id.fabAddBarangay).setOnClickListener(v -> showAddEditDialog(null));

        // Open Drawer from internal header
        View btnMenu = view.findViewById(R.id.btnMenu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                if (getActivity() instanceof AdminDashboardActivity) {
                    ((AdminDashboardActivity) getActivity()).openDrawer();
                }
            });
        }

        loadBarangays();
    }

    private void loadBarangays() {
        new Thread(() -> {
            List<BarangayZone> zones = db.barangayZoneDao().getAllZones();
            boolean fixedAny = false;
            if (zones != null) {
                for (BarangayZone zone : zones) {
                    if (zone.accessPin == null || zone.accessPin.length() != 4) {
                        // Generate a random 4-digit PIN (e.g., 1000-9999)
                        int randomPin = 1000 + (int)(Math.random() * 9000);
                        zone.accessPin = String.valueOf(randomPin);
                        db.barangayZoneDao().insert(zone); // Update
                        fixedAny = true;
                    }
                }
                if (fixedAny) {
                    zones = db.barangayZoneDao().getAllZones(); // Re-fetch
                }
            }

            final List<BarangayZone> result = zones != null ? zones : new ArrayList<>();
            requireActivity().runOnUiThread(() -> {
                updateList(result);
                if (result.isEmpty()) {
                    rvBarangays.setVisibility(View.GONE);
                    emptyState.setVisibility(View.VISIBLE);
                } else {
                    rvBarangays.setVisibility(View.VISIBLE);
                    emptyState.setVisibility(View.GONE);
                }
            });
        }).start();
    }

    private void updateList(List<BarangayZone> list) {
        rvBarangays.setAdapter(new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @NonNull
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_simple, parent, false);
                return new RecyclerView.ViewHolder(v) {};
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
                BarangayZone zone = list.get(position);
                android.widget.TextView tvTitle = holder.itemView.findViewById(R.id.tvItemTitle);
                android.widget.TextView tvSubtitle = holder.itemView.findViewById(R.id.tvItemSubtitle);
                tvTitle.setText(zone.name);
                String pin = (zone.accessPin != null && !zone.accessPin.isEmpty()) ? "PIN: " + zone.accessPin : "No PIN set";
                tvSubtitle.setText(pin);

                holder.itemView.findViewById(R.id.btnEdit).setOnClickListener(v -> showAddEditDialog(zone));
                holder.itemView.findViewById(R.id.btnDelete).setOnClickListener(v -> confirmDelete(zone));
            }

            @Override
            public int getItemCount() { return list.size(); }
        });
    }

    private void showAddEditDialog(@Nullable BarangayZone existing) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_barangay, null);

        android.widget.TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        dialogTitle.setText(existing == null ? "Add Barangay" : "Edit Barangay");

        com.google.android.material.textfield.TextInputEditText etName = dialogView.findViewById(R.id.etBarangayName);
        com.google.android.material.textfield.TextInputEditText etPin = dialogView.findViewById(R.id.etBarangayPin);

        if (existing != null) {
            etName.setText(existing.name);
            etPin.setText(existing.accessPin);
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
            String pin = etPin.getText().toString().trim();

            if (name.isEmpty()) { Toast.makeText(requireContext(), "Name required", Toast.LENGTH_SHORT).show(); return; }
            if (pin.length() != 4) { Toast.makeText(requireContext(), "PIN must be exactly 4 digits", Toast.LENGTH_SHORT).show(); return; }

            final BarangayZone zone;
            if (existing != null) {
                zone = existing;
                zone.name = name;
                zone.accessPin = pin;
            } else {
                zone = new BarangayZone(name, "", pin);
            }

            new Thread(() -> {
                db.barangayZoneDao().insert(zone);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Barangay saved", Toast.LENGTH_SHORT).show();
                    loadBarangays();
                });
            }).start();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void confirmDelete(BarangayZone zone) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Barangay")
                .setMessage("Remove \"" + zone.name + "\"?")
                .setPositiveButton("Delete", (d, w) -> new Thread(() -> {
                    db.barangayZoneDao().delete(zone);
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Barangay deleted", Toast.LENGTH_SHORT).show();
                        loadBarangays();
                    });
                }).start())
                .setNegativeButton("Cancel", null)
                .show();
    }
}
