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
import com.example.instacare.utils.BlurUtils;
import com.example.instacare.utils.CaseRefGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class EndorsementRequestSheet extends BaseBlurredBottomSheet {

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
        AutoCompleteTextView spinnerBarangay = view.findViewById(R.id.spinnerBarangay);
        AutoCompleteTextView spinnerPurpose = view.findViewById(R.id.spinnerPurpose);
        AutoCompleteTextView spinnerHospital = view.findViewById(R.id.spinnerHospital);
        TextInputEditText editReason = view.findViewById(R.id.editReason);

        SessionManager sessionManager = SessionManager.getInstance(requireContext());
        int currentUserId = sessionManager.getCurrentUserUid();
        editAddress.setText(sessionManager.getString("USER_ADDRESS", ""));

        // Spinners setup
        spinnerPurpose.setFocusable(true);
        spinnerPurpose.setFocusableInTouchMode(true);
        spinnerHospital.setFocusable(true);
        spinnerHospital.setFocusableInTouchMode(true);
        spinnerBarangay.setFocusable(true);
        spinnerBarangay.setFocusableInTouchMode(true);

        AppDatabase db = AppDatabase.getDatabase(requireContext());

        // Barangay Dropdown
        Executors.newSingleThreadExecutor().execute(() -> {
            List<com.example.instacare.data.local.BarangayZone> zones = db.barangayZoneDao().getAllZones();
            List<String> brgyNames = new ArrayList<>();
            if (zones != null) {
                for (com.example.instacare.data.local.BarangayZone zone : zones) brgyNames.add(zone.name);
                java.util.Collections.sort(brgyNames);
            }
            if (brgyNames.isEmpty()) brgyNames.add("No Barangays Configured");
            final List<String> finalNames = brgyNames;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    spinnerBarangay.setOnClickListener(v -> showBottomSheetDropdown("Select Barangay", finalNames, spinnerBarangay));
                    spinnerBarangay.setOnFocusChangeListener((v, f) -> { if(f) showBottomSheetDropdown("Select Barangay", finalNames, spinnerBarangay); });
                });
            }
        });

        // Purpose Dropdown
        String[] purposes = {"Medical Assistance", "Hospital Referral", "Lab / Check-up", "Other"};
        spinnerPurpose.setOnClickListener(v -> showBottomSheetDropdown("Select Purpose", java.util.Arrays.asList(purposes), spinnerPurpose));
        spinnerPurpose.setOnFocusChangeListener((v, f) -> { if(f) showBottomSheetDropdown("Select Purpose", java.util.Arrays.asList(purposes), spinnerPurpose); });

        // Hospital Dropdown (Dynamic)
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                // Simplified Overpass fetch
                String query = "[out:json];node[\"amenity\"=\"hospital\"](around:50000,7.9065,125.0938);out;";
                String urlString = "https://overpass-api.de/api/interpreter?data=" + java.net.URLEncoder.encode(query, "UTF-8");
                java.net.URL url = new java.net.URL(urlString);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                java.io.BufferedReader in = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) response.append(line);
                in.close();
                org.json.JSONObject jsonResponse = new org.json.JSONObject(response.toString());
                org.json.JSONArray elements = jsonResponse.getJSONArray("elements");
                List<String> names = new ArrayList<>();
                for (int i = 0; i < elements.length(); i++) {
                    org.json.JSONObject tags = elements.getJSONObject(i).optJSONObject("tags");
                    if (tags != null && tags.has("name")) names.add(tags.getString("name"));
                }
                if (names.isEmpty()) names.add("Valencia City General Hospital");
                final List<String> finalHospitals = names;
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        spinnerHospital.setOnClickListener(v -> showBottomSheetDropdown("Target Hospital", finalHospitals, spinnerHospital));
                        spinnerHospital.setOnFocusChangeListener((v, f) -> { if(f) showBottomSheetDropdown("Target Hospital", finalHospitals, spinnerHospital); });
                    });
                }
            } catch (Exception e) { e.printStackTrace(); }
        });

        // --- Unified Request Type Logic ---
        View layoutMedical = view.findViewById(R.id.layoutMedicalFields);
        View layoutEvac = view.findViewById(R.id.layoutEvacFields);
        com.google.android.material.chip.ChipGroup requestTypeGroup = view.findViewById(R.id.chipGroupRequestType);

        requestTypeGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipTypeMedical) {
                layoutMedical.setVisibility(View.VISIBLE);
                layoutEvac.setVisibility(View.GONE);
            } else {
                layoutMedical.setVisibility(View.GONE);
                layoutEvac.setVisibility(View.VISIBLE);
            }
        });

        // Evacuation specific selections
        final String[] selectedDisaster = {"flood"};
        final String[] selectedSpecial = {"none"};
        
        com.google.android.material.chip.ChipGroup cgDisaster = view.findViewById(R.id.chipGroupEvacDisaster);
        cgDisaster.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipEvacFlood) selectedDisaster[0] = "flood";
            else if (id == R.id.chipEvacEarthquake) selectedDisaster[0] = "earthquake";
            else if (id == R.id.chipEvacFire) selectedDisaster[0] = "fire";
            else if (id == R.id.chipEvacTyphoon) selectedDisaster[0] = "typhoon";
        });

        com.google.android.material.chip.ChipGroup cgSpecial = view.findViewById(R.id.chipGroupEvacSpecial);
        cgSpecial.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chipEvacElderly) selectedSpecial[0] = "elderly";
            else if (id == R.id.chipEvacPWD) selectedSpecial[0] = "PWD";
            else selectedSpecial[0] = "none";
        });

        // Submit Logic
        view.findViewById(R.id.btnSubmitEndorsement).setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String address = editAddress.getText().toString().trim();
            String brgy = spinnerBarangay.getText().toString().trim();
            String reason = editReason.getText().toString().trim();

            if (name.isEmpty() || address.isEmpty() || brgy.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill in required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            boolean isMedical = requestTypeGroup.getCheckedChipId() == R.id.chipTypeMedical;
            long now = System.currentTimeMillis();

            Executors.newSingleThreadExecutor().execute(() -> {
                if (isMedical) {
                    // Prevent duplicate pending requests for the same barangay
                    int pendingCount = db.endorsementDao().getPendingCountByUserAndBarangay(currentUserId, brgy);
                    if (pendingCount > 0) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() ->
                                Toast.makeText(requireContext(), "You already have a pending request for this barangay", Toast.LENGTH_SHORT).show()
                            );
                        }
                        return;
                    }
                    String purpose = spinnerPurpose.getText().toString().trim();
                    String hospital = spinnerHospital.getText().toString().trim();
                    String caseRef = CaseRefGenerator.generate();
                    Endorsement endor = new Endorsement(currentUserId, name, address, purpose, hospital, reason, "PENDING", caseRef, brgy, 0, now, now);
                    long endorId = db.endorsementDao().insert(endor);
                    sendAutoWelcomeMsg((int) endorId, currentUserId, caseRef, "BARANGAY");
                } else {
                    String caseRef = "EV-" + new java.text.SimpleDateFormat("yyyy").format(new java.util.Date(now)) + "-" + String.format("%04d", (int)(Math.random()*9000+1000));
                    com.example.instacare.data.local.EvacuationEndorsement evac = new com.example.instacare.data.local.EvacuationEndorsement(
                        currentUserId, name, address, selectedDisaster[0], selectedSpecial[0], reason, "PENDING", caseRef, brgy, now, now
                    );
                    db.evacuationEndorsementDao().insert(evac);
                }
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Submitted!", Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.onSubmitted();
                        dismiss();
                    });
                }
            });
        });

        view.findViewById(R.id.btnCancelEndorsement).setOnClickListener(v -> dismiss());
    }

    private void sendAutoWelcomeMsg(int endorsementId, int userId, String caseRef, String role) {
        AppDatabase db = AppDatabase.getDatabase(requireContext());
        com.example.instacare.data.local.ChatMessage autoChat = new com.example.instacare.data.local.ChatMessage(
            endorsementId, 1000, userId,
            "Good day! This is the Barangay Staff. We are reviewing your request.",
            System.currentTimeMillis(), false
        );
        autoChat.conversationRole = role;
        autoChat.senderRole = role;
        db.chatMessageDao().insert(autoChat);
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
        
        android.widget.LinearLayout rootContainer = new android.widget.LinearLayout(requireContext());
        rootContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
        rootContainer.setBackgroundResource(R.drawable.bg_bottom_sheet);

        // Drag handle
        View handle = new View(requireContext());
        android.widget.LinearLayout.LayoutParams handleParams = new android.widget.LinearLayout.LayoutParams(dpToPx(40), dpToPx(4));
        handleParams.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        handleParams.topMargin = dpToPx(16);
        handleParams.bottomMargin = dpToPx(16);
        handle.setLayoutParams(handleParams);
        handle.setBackgroundResource(R.drawable.bg_drag_handle);
        rootContainer.addView(handle);

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
        rootContainer.addView(titleView);

        // NestedScrollView for proper BottomSheet scroll cooperation
        androidx.core.widget.NestedScrollView nestedScrollView = new androidx.core.widget.NestedScrollView(requireContext());
        android.widget.LinearLayout.LayoutParams scrollParams = new android.widget.LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nestedScrollView.setLayoutParams(scrollParams);
        nestedScrollView.setNestedScrollingEnabled(true);

        android.widget.LinearLayout itemsContainer = new android.widget.LinearLayout(requireContext());
        itemsContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
        itemsContainer.setPadding(0, 0, 0, dpToPx(24));

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
                targetView.setText(item, false);
                dialog.dismiss();
            });
            itemsContainer.addView(itemView);
        }

        nestedScrollView.addView(itemsContainer);
        rootContainer.addView(nestedScrollView);

        dialog.setContentView(rootContainer);
        BlurUtils.applyBlur(dialog);
        
        // Expand fully and lock — scroll happens INSIDE the list, not on the sheet
        dialog.setOnShowListener(d -> {
            com.google.android.material.bottomsheet.BottomSheetDialog bsd = (com.google.android.material.bottomsheet.BottomSheetDialog) d;
            View sheet = bsd.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (sheet != null) {
                com.google.android.material.bottomsheet.BottomSheetBehavior<View> behavior = 
                    com.google.android.material.bottomsheet.BottomSheetBehavior.from(sheet);
                behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);
            }
        });

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
