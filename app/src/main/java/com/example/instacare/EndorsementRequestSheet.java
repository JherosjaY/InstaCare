package com.example.instacare;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;

import com.example.instacare.data.local.AppDatabase;
import com.example.instacare.data.local.Endorsement;
import com.example.instacare.data.local.Hospital;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class EndorsementRequestSheet extends BottomSheetDialogFragment {

    public interface OnEndorsementSubmitted {
        void onSubmitted();
    }

    private OnEndorsementSubmitted listener;

    public void setOnEndorsementSubmittedListener(OnEndorsementSubmitted listener) {
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_endorsement_request, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null) {
            View bottomSheet = getDialog().findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet).setState(
                    com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
                );
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextInputEditText editName = view.findViewById(R.id.editPatientName);
        TextInputEditText editAddress = view.findViewById(R.id.editAddress);
        AutoCompleteTextView spinnerPurpose = view.findViewById(R.id.spinnerPurpose);
        AutoCompleteTextView spinnerHospital = view.findViewById(R.id.spinnerHospital);
        TextInputEditText editReason = view.findViewById(R.id.editReason);

        SharedPreferences prefs = requireContext().getSharedPreferences("InstaCarePrefs", Context.MODE_PRIVATE);
        String userEmail = prefs.getString("USER_EMAIL", ""); // Keep for address fetching
        String username = prefs.getString("USER_USERNAME", ""); // Use for endorsement ownership

        // Pre-fill from saved user data
        editName.setText(prefs.getString("USER_NAME", ""));
        editAddress.setText(prefs.getString("MED_ADDRESS_" + userEmail, ""));

        // Make spinners focusable to show the highlight, and respond to clicks/focus
        spinnerPurpose.setFocusable(true);
        spinnerPurpose.setFocusableInTouchMode(true);
        spinnerPurpose.setCursorVisible(false);
        spinnerPurpose.setLongClickable(false);

        spinnerHospital.setFocusable(true);
        spinnerHospital.setFocusableInTouchMode(true);
        spinnerHospital.setCursorVisible(false);
        spinnerHospital.setLongClickable(false);

        // Purpose dropdown
        String[] purposes = {"Medical Assistance", "Hospital Referral", "Lab / Check-up", "Other"};
        spinnerPurpose.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) showBottomSheetDropdown("Select Purpose", java.util.Arrays.asList(purposes), spinnerPurpose);
        });
        spinnerPurpose.setOnClickListener(v -> {
            showBottomSheetDropdown("Select Purpose", java.util.Arrays.asList(purposes), spinnerPurpose);
        });

        // Hospital dropdown — load from Room DB
        AppDatabase db = AppDatabase.getDatabase(requireContext());
        Executors.newSingleThreadExecutor().execute(() -> {
            List<Hospital> hospitals = db.hospitalDao().getAllHospitalsDirect();
            List<String> names = new ArrayList<>();
            for (Hospital h : hospitals) {
                names.add(h.name);
            }
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    spinnerHospital.setOnFocusChangeListener((v, hasFocus) -> {
                        if (hasFocus) showBottomSheetDropdown("Target Hospital", names, spinnerHospital);
                    });
                    spinnerHospital.setOnClickListener(v -> {
                        showBottomSheetDropdown("Target Hospital", names, spinnerHospital);
                    });
                });
            }
        });

        // Submit
        view.findViewById(R.id.btnSubmitEndorsement).setOnClickListener(v -> {
            String name = editName.getText() != null ? editName.getText().toString().trim() : "";
            String address = editAddress.getText() != null ? editAddress.getText().toString().trim() : "";
            String purpose = spinnerPurpose.getText().toString().trim();
            String hospital = spinnerHospital.getText().toString().trim();
            String reason = editReason.getText() != null ? editReason.getText().toString().trim() : "";

            if (name.isEmpty() || purpose.isEmpty() || hospital.isEmpty() || reason.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            long now = System.currentTimeMillis();
            Endorsement endorsement = new Endorsement(username, name, address,
                    purpose, hospital, reason, "PENDING", now, now);

            Executors.newSingleThreadExecutor().execute(() -> {
                db.endorsementDao().insert(endorsement);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Endorsement request submitted!", Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.onSubmitted();
                        dismiss();
                    });
                }
            });
        });

        // Cancel
        view.findViewById(R.id.btnCancelEndorsement).setOnClickListener(v -> dismiss());
    }

    @Override
    public int getTheme() {
        return R.style.BottomSheetDialogTheme;
    }

    private com.google.android.material.bottomsheet.BottomSheetDialog currentDropdownDialog = null;

    private void showBottomSheetDropdown(String title, java.util.List<String> items, AutoCompleteTextView targetView) {
        if (currentDropdownDialog != null && currentDropdownDialog.isShowing()) {
            return;
        }

        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme);
        
        android.widget.LinearLayout container = new android.widget.LinearLayout(requireContext());
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setBackgroundResource(R.drawable.bg_bottom_sheet);
        container.setPadding(0, 0, 0, dpToPx(24));

        // Drag handle
        View handle = new View(requireContext());
        android.widget.LinearLayout.LayoutParams handleParams = new android.widget.LinearLayout.LayoutParams(dpToPx(40), dpToPx(4));
        handleParams.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        handleParams.topMargin = dpToPx(16);
        handleParams.bottomMargin = dpToPx(16);
        handle.setLayoutParams(handleParams);
        handle.setBackgroundResource(R.drawable.bg_drag_handle);
        container.addView(handle);

        // Title
        android.widget.TextView titleView = new android.widget.TextView(requireContext());
        titleView.setText(title);
        titleView.setTextSize(18);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setGravity(android.view.Gravity.CENTER);
        android.widget.LinearLayout.LayoutParams titleParams = new android.widget.LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.bottomMargin = dpToPx(16);
        titleView.setLayoutParams(titleParams);
        titleView.setTextColor(getResources().getColor(R.color.text_primary, null));
        container.addView(titleView);

        // Items
        for (String item : items) {
            android.widget.TextView itemView = new android.widget.TextView(requireContext());
            itemView.setText(item);
            itemView.setTextSize(16);
            itemView.setTypeface(null, android.graphics.Typeface.BOLD);
            itemView.setPadding(dpToPx(24), dpToPx(16), dpToPx(24), dpToPx(16));
            itemView.setTextColor(getResources().getColor(R.color.text_primary, null));
            
            // Ripple effect
            android.util.TypedValue outValue = new android.util.TypedValue();
            requireContext().getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            itemView.setBackgroundResource(outValue.resourceId);

            itemView.setOnClickListener(v -> {
                targetView.setText(item, false); // false to not trigger filter
                dialog.dismiss();
            });
            container.addView(itemView);
        }

        dialog.setContentView(container);
        
        // When dialog is fully hidden, clear focus so clicking again triggers onFocusChange
        dialog.setOnDismissListener(d -> {
            currentDropdownDialog = null;
            targetView.clearFocus();
        });

        currentDropdownDialog = dialog;
        dialog.show();
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
