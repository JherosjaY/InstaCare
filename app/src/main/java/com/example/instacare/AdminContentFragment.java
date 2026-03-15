package com.example.instacare;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.data.local.AppDatabase;
import com.example.instacare.data.local.Guide;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AdminContentFragment extends Fragment {

    private RecyclerView rvGuides;
    private AdminGuidesAdapter adapter;
    private AppDatabase db;
    private View emptyState;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_admin_content, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        db = AppDatabase.getDatabase(requireContext());
        rvGuides = view.findViewById(R.id.rvGuides);
        emptyState = view.findViewById(R.id.emptyStateGuides);

        adapter = new AdminGuidesAdapter(new ArrayList<>(), new AdminGuidesAdapter.Listener() {
            @Override public void onEdit(Guide guide) { showAddEditDialog(guide); }
            @Override public void onDelete(Guide guide) { confirmDelete(guide); }
        });

        rvGuides.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvGuides.setAdapter(adapter);

        view.findViewById(R.id.fabAddGuide).setOnClickListener(v -> showAddEditDialog(null));

        loadGuides();
    }

    private void loadGuides() {
        new Thread(() -> {
            List<Guide> guides = db.guideDao().getAllGuidesDirect();
            final List<Guide> result = guides != null ? guides : new ArrayList<>();
            requireActivity().runOnUiThread(() -> {
                adapter.updateList(result);
                if (result.isEmpty()) {
                    rvGuides.setVisibility(View.GONE);
                    emptyState.setVisibility(View.VISIBLE);
                } else {
                    rvGuides.setVisibility(View.VISIBLE);
                    emptyState.setVisibility(View.GONE);
                }
            });
        }).start();
    }

    private void showAddEditDialog(@Nullable Guide existing) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_guide, null);

        TextView dialogTitle = dialogView.findViewById(R.id.dialogTitle);
        dialogTitle.setText(existing == null ? "Add Guide" : "Edit Guide");

        com.google.android.material.textfield.TextInputEditText etTitle = dialogView.findViewById(R.id.etGuideTitle);
        com.google.android.material.textfield.TextInputEditText etCategory = dialogView.findViewById(R.id.etGuideCategory);
        com.google.android.material.textfield.TextInputEditText etDescription = dialogView.findViewById(R.id.etGuideDescription);

        if (existing != null) {
            etTitle.setText(existing.title);
            etCategory.setText(existing.category);
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

        dialogView.findViewById(R.id.btnCancel).setOnClickListener(v -> dialog.dismiss());
        dialogView.findViewById(R.id.btnSave).setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String cat = etCategory.getText().toString().trim();
            String desc = etDescription.getText().toString().trim();
            if (title.isEmpty()) { Toast.makeText(requireContext(), "Title required", Toast.LENGTH_SHORT).show(); return; }

            final Guide guide;
            if (existing != null) {
                guide = existing;
                guide.title = title;
                guide.category = cat.isEmpty() ? "General" : cat;
                guide.description = desc;
            } else {
                guide = new Guide(UUID.randomUUID().toString(), title, cat.isEmpty() ? "General" : cat,
                        desc, "Beginner", "5 mins", 0, null, "[]", false);
            }
            new Thread(() -> {
                db.guideDao().insert(guide);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Guide saved", Toast.LENGTH_SHORT).show();
                    loadGuides();
                });
            }).start();
            dialog.dismiss();
        });

        dialog.show();
    }



    private void confirmDelete(Guide guide) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Guide")
                .setMessage("Remove \"" + guide.title + "\"?")
                .setPositiveButton("Delete", (d, w) -> new Thread(() -> {
                    db.guideDao().deleteById(guide.id);
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Guide deleted", Toast.LENGTH_SHORT).show();
                        loadGuides();
                    });
                }).start())
                .setNegativeButton("Cancel", null)
                .show();
    }
}
