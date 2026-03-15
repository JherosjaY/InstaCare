package com.example.instacare;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.data.local.AppDatabase;
import com.example.instacare.data.local.Hospital;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AdminHospitalsFragment extends Fragment {

    private RecyclerView rvHospitals;
    private AdminHospitalsAdapter adapter;
    private AppDatabase db;
    private View emptyState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_hospitals, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        db = AppDatabase.getDatabase(requireContext());
        rvHospitals = view.findViewById(R.id.rvHospitals);
        emptyState = view.findViewById(R.id.emptyStateHospitals);

        adapter = new AdminHospitalsAdapter(new ArrayList<>(), new AdminHospitalsAdapter.Listener() {
            @Override public void onEdit(Hospital h) { showAddEditDialog(h); }
            @Override public void onDelete(Hospital h) { confirmDelete(h); }
        });

        rvHospitals.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvHospitals.setAdapter(adapter);

        view.findViewById(R.id.fabAddHospital).setOnClickListener(v -> showAddEditDialog(null));

        loadHospitals();
    }

    private void loadHospitals() {
        new Thread(() -> {
            List<Hospital> hospitals = db.hospitalDao().getAllHospitalsDirect();
            final List<Hospital> result = hospitals != null ? hospitals : new ArrayList<>();
            requireActivity().runOnUiThread(() -> {
                adapter.updateList(result);
                if (result.isEmpty()) {
                    rvHospitals.setVisibility(View.GONE);
                    emptyState.setVisibility(View.VISIBLE);
                } else {
                    rvHospitals.setVisibility(View.VISIBLE);
                    emptyState.setVisibility(View.GONE);
                }
            });
        }).start();
    }

    private void showAddEditDialog(@Nullable Hospital existing) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_hospital, null);

        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        dialogTitle.setText(existing == null ? "Add Hospital" : "Edit Hospital");

        com.google.android.material.textfield.TextInputEditText etName = dialogView.findViewById(R.id.etHospitalName);
        com.google.android.material.textfield.TextInputEditText etAddress = dialogView.findViewById(R.id.etHospitalAddress);
        com.google.android.material.textfield.TextInputEditText etPhone = dialogView.findViewById(R.id.etHospitalPhone);
        com.google.android.material.textfield.TextInputEditText etType = dialogView.findViewById(R.id.etHospitalType);

        if (existing != null) {
            etName.setText(existing.name);
            etAddress.setText(existing.address);
            etPhone.setText(existing.phone);
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
            String phone = etPhone.getText().toString().trim();
            String type = etType.getText().toString().trim();
            if (name.isEmpty()) { Toast.makeText(requireContext(), "Name required", Toast.LENGTH_SHORT).show(); return; }

            final Hospital hosp;
            if (existing != null) {
                hosp = existing;
                hosp.name = name;
                hosp.address = address;
                hosp.phone = phone;
                hosp.type = type.isEmpty() ? "Hospital" : type;
            } else {
                hosp = new Hospital(UUID.randomUUID().toString(), name, "--",
                        address, phone, true, type.isEmpty() ? "Hospital" : type, "[]", 0.0, 0.0, null, "Available");
            }
            new Thread(() -> {
                db.hospitalDao().insert(hosp);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Hospital saved", Toast.LENGTH_SHORT).show();
                    loadHospitals();
                });
            }).start();
            dialog.dismiss();
        });

        dialog.show();
    }



    private void confirmDelete(Hospital hosp) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Hospital")
                .setMessage("Remove \"" + hosp.name + "\"?")
                .setPositiveButton("Delete", (d, w) -> new Thread(() -> {
                    db.hospitalDao().delete(hosp);
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Hospital deleted", Toast.LENGTH_SHORT).show();
                        loadHospitals();
                    });
                }).start())
                .setNegativeButton("Cancel", null)
                .show();
    }
}
