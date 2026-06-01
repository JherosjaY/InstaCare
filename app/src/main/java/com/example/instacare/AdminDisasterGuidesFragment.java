package com.example.instacare;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.instacare.data.local.AppDatabase;
import com.example.instacare.data.local.DisasterGuide;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AdminDisasterGuidesFragment extends Fragment {

    private RecyclerView rvGuides;
    private AdminDisasterGuidesAdapter adapter;
    private AppDatabase db;
    private View emptyState;
    private List<DisasterGuide> currentList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_disaster_guides, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        db = AppDatabase.getDatabase(requireContext());
        rvGuides = view.findViewById(R.id.rvDisastersAdmin);
        emptyState = view.findViewById(R.id.emptyStateGuides);

        adapter = new AdminDisasterGuidesAdapter(currentList, new AdminDisasterGuidesAdapter.Listener() {
            @Override
            public void onEdit(DisasterGuide guide) {
                showAddEditDialog(guide);
            }
            @Override
            public void onDelete(DisasterGuide guide) {
                confirmDelete(guide, null);
            }
        });

        rvGuides.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvGuides.setAdapter(adapter);

        view.findViewById(R.id.fabAddGuide).setOnClickListener(v -> showAddEditDialog(null));

        View btnMenu = view.findViewById(R.id.btnMenu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                if (getActivity() instanceof AdminDashboardActivity) {
                    ((AdminDashboardActivity) getActivity()).openDrawer();
                }
            });
        }

        loadGuides();
    }

    private void loadGuides() {
        new Thread(() -> {
            List<DisasterGuide> guides = db.disasterGuideDao().getAllDisasterGuidesDirect();
            final List<DisasterGuide> result = guides != null ? guides : new ArrayList<>();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    currentList = result;
                    adapter.updateList(currentList);
                    if (result.isEmpty()) {
                        rvGuides.setVisibility(View.GONE);
                        emptyState.setVisibility(View.VISIBLE);
                    } else {
                        rvGuides.setVisibility(View.VISIBLE);
                        emptyState.setVisibility(View.GONE);
                    }
                });
            }
        }).start();
    }

    private void showAddEditDialog(@Nullable DisasterGuide existing) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_guide, null);

        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        dialogTitle.setText(existing == null ? "Add Disaster Guide" : "Edit Disaster Guide");

        com.google.android.material.textfield.TextInputEditText etTitle = dialogView.findViewById(R.id.etGuideTitle);
        com.google.android.material.textfield.TextInputEditText etCategory = dialogView.findViewById(R.id.etGuideCategory);
        com.google.android.material.textfield.TextInputEditText etDescription = dialogView.findViewById(R.id.etGuideDescription);

        // Map Category to DisasterType
        etCategory.setHint("Disaster Type (e.g. Flood)");

        if (existing != null) {
            etTitle.setText(existing.title);
            etCategory.setText(existing.disasterType);
            etDescription.setText(existing.description);
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

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> {
            if (existing != null) {
               confirmDelete(existing, dialog);
            } else {
               dialog.dismiss();
            }
        });

        // if edit mode, change cancel button to delete
        if(existing != null) {
            // we will use standard cancel or change text
            android.widget.Button btnCancel = dialogView.findViewById(R.id.btnCancel);
            btnCancel.setText("Delete");
            btnCancel.setTextColor(android.graphics.Color.RED);
        }

        dialogView.findViewById(R.id.btnSave).setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String cat = etCategory.getText().toString().trim();
            String desc = etDescription.getText().toString().trim();
            if (title.isEmpty()) { Toast.makeText(requireContext(), "Title required", Toast.LENGTH_SHORT).show(); return; }

            final DisasterGuide guide;
            if (existing != null) {
                guide = existing;
                guide.title = title;
                guide.disasterType = cat.isEmpty() ? "General" : cat;
                guide.description = desc;
            } else {
                guide = new DisasterGuide(UUID.randomUUID().toString(), title, cat.isEmpty() ? "General" : cat,
                        desc, "Medium", "5 mins", 0, null, "[]", false);
            }
            new Thread(() -> {
                db.disasterGuideDao().insert(guide);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Guide saved", Toast.LENGTH_SHORT).show();
                        loadGuides();
                    });
                }
            }).start();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void confirmDelete(DisasterGuide guide, AlertDialog parentDialog) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Guide")
                .setMessage("Remove \"" + guide.title + "\"?")
                .setPositiveButton("Delete", (d, w) -> new Thread(() -> {
                    db.disasterGuideDao().deleteById(guide.id);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            Toast.makeText(requireContext(), "Guide deleted", Toast.LENGTH_SHORT).show();
                            loadGuides();
                        });
                    }
                }).start())
                .setNegativeButton("Cancel", null)
                .show();
        parentDialog.dismiss();
    }
}
