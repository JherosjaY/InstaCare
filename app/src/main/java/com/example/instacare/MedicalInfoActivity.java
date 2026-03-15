package com.example.instacare;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomsheet.BottomSheetDialog;

public class MedicalInfoActivity extends AppCompatActivity {

    private EditText editName, editHeight, editWeight, editAddress;
    private TextView textSex, textBloodTypeVal, textDob, textOrganDonor;
    private EditText editConditions, editMedications, editAllergies, editRemarks;

    // We'll use this to isolate the saved data per active user email, 
    // but applying globally for now to match the existing pattern if preferred.
    private SharedPreferences prefs;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_medical_info);

        // Adaptive status bar color to match the Medical Info header
        getWindow().setStatusBarColor(androidx.core.content.ContextCompat.getColor(this, R.color.medical_info_header));
        boolean isDark = (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        androidx.core.view.WindowInsetsControllerCompat controller =
            androidx.core.view.WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (controller != null) {
            controller.setAppearanceLightStatusBars(!isDark);
        }

        prefs = getSharedPreferences("InstaCarePrefs", Context.MODE_PRIVATE);
        userEmail = prefs.getString("USER_EMAIL", "");

        initViews();
        loadData();
        setupFocusBehavior();
        setupClickListeners();
    }

    private void initViews() {
        editName = findViewById(R.id.editName);
        editHeight = findViewById(R.id.editHeight);
        editWeight = findViewById(R.id.editWeight);
        editAddress = findViewById(R.id.editAddress);

        textSex = findViewById(R.id.textSex);
        textBloodTypeVal = findViewById(R.id.textBloodTypeVal);
        textDob = findViewById(R.id.textDob);
        textOrganDonor = findViewById(R.id.textOrganDonor);

        editConditions = findViewById(R.id.editConditions);
        editMedications = findViewById(R.id.editMedications);
        editAllergies = findViewById(R.id.editAllergies);
        editRemarks = findViewById(R.id.editRemarks);
    }
    
    private void setupFocusBehavior() {
        setupFocusLine(editName, findViewById(R.id.dividerName), findViewById(R.id.titleName));
        setupFocusLine(editHeight, findViewById(R.id.dividerHeight), findViewById(R.id.titleHeight));
        setupFocusLine(editWeight, findViewById(R.id.dividerWeight), findViewById(R.id.titleWeight));
        setupFocusLine(editAddress, findViewById(R.id.dividerAddress), findViewById(R.id.titleAddress));
        setupFocusLine(editConditions, findViewById(R.id.dividerConditions), findViewById(R.id.titleConditions));
        setupFocusLine(editMedications, findViewById(R.id.dividerMedications), findViewById(R.id.titleMedications));
        setupFocusLine(editAllergies, findViewById(R.id.dividerAllergies), findViewById(R.id.titleAllergies));
        setupFocusLine(editRemarks, findViewById(R.id.dividerRemarks), findViewById(R.id.titleRemarks));
    }

    private void setupFocusLine(EditText editText, android.view.View divider, TextView title) {
        editText.setOnFocusChangeListener((v, hasFocus) -> {
            int focusColor = androidx.core.content.ContextCompat.getColor(this, R.color.primary);
            int unfocusDividerColor = androidx.core.content.ContextCompat.getColor(this, R.color.medical_info_divider);
            int unfocusTitleColor = androidx.core.content.ContextCompat.getColor(this, R.color.medical_info_label);
            
            divider.setBackgroundColor(hasFocus ? focusColor : unfocusDividerColor);
            title.setTextColor(hasFocus ? focusColor : unfocusTitleColor);
        });
    }

    private void loadData() {
        // Basic Info
        editName.setText(prefs.getString("MED_NAME_" + userEmail, prefs.getString("USER_NAME", "")));
        editHeight.setText(prefs.getString("MED_HEIGHT_" + userEmail, ""));
        editWeight.setText(prefs.getString("MED_WEIGHT_" + userEmail, ""));
        editAddress.setText(prefs.getString("MED_ADDRESS_" + userEmail, ""));

        // Personal Details
        textSex.setText(prefs.getString("MED_SEX_" + userEmail, "Not set"));
        textBloodTypeVal.setText(prefs.getString("USER_BLOOD_TYPE_" + userEmail, prefs.getString("USER_BLOOD_TYPE", "Not set")));
        textDob.setText(prefs.getString("MED_DOB_" + userEmail, "Not set"));
        textOrganDonor.setText(prefs.getString("MED_DONOR_" + userEmail, "Not set"));

        // Health Info
        editConditions.setText(prefs.getString("MED_COND_" + userEmail, ""));
        editMedications.setText(prefs.getString("MED_MEDS_" + userEmail, ""));
        editAllergies.setText(prefs.getString("USER_ALLERGIES_" + userEmail, prefs.getString("USER_ALLERGIES", "")));
        editRemarks.setText(prefs.getString("MED_REMARKS_" + userEmail, ""));
    }

    private void setupClickListeners() {
        findViewById(R.id.btnCancel).setOnClickListener(v -> finish());

        findViewById(R.id.btnSave).setOnClickListener(v -> {
            saveData();
            Toast.makeText(this, "Medical info saved", Toast.LENGTH_SHORT).show();
            // Send broadcast or set result if we want the caller fragment to update immediately
            setResult(RESULT_OK);
            finish();
        });

        // Setup Dialogs for clickable list items
        findViewById(R.id.btnSex).setOnClickListener(v -> showSelectionDialog("Sex", new String[]{"Male", "Female", "Other", "Prefer not to say"}, textSex));
        findViewById(R.id.btnBloodType).setOnClickListener(v -> showSelectionDialog("Blood type", new String[]{"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"}, textBloodTypeVal));
        findViewById(R.id.btnDob).setOnClickListener(v -> showDatePickerDialog());
        findViewById(R.id.btnOrganDonor).setOnClickListener(v -> showSelectionDialog("Organ donor", new String[]{"Yes", "No"}, textOrganDonor));
    }

    private void showDatePickerDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.dialog_bottom_sheet_datepicker, null);
        dialog.setContentView(view);

        android.widget.DatePicker datePicker = view.findViewById(R.id.datePicker);
        view.findViewById(R.id.btnConfirmDate).setOnClickListener(btnConf -> {
            int year = datePicker.getYear();
            int month = datePicker.getMonth();
            int day = datePicker.getDayOfMonth();
            textDob.setText(year + "-" + String.format(java.util.Locale.US, "%02d", month + 1) + "-" + String.format(java.util.Locale.US, "%02d", day));
            dialog.dismiss();
        });

        // Set transparent background for bottom sheet corners
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialog.show();
    }

    private void showSelectionDialog(String title, String[] options, TextView targetView) {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.dialog_bottom_sheet_list, null);
        dialog.setContentView(view);

        TextView textTitle = view.findViewById(R.id.textListTitle);
        textTitle.setText(title);

        android.widget.LinearLayout listContainer = view.findViewById(R.id.listContainer);
        for (String option : options) {
            TextView optionView = (TextView) getLayoutInflater().inflate(R.layout.item_bottom_sheet_option, listContainer, false);
            optionView.setText(option);
            optionView.setOnClickListener(v -> {
                targetView.setText(option);
                dialog.dismiss();
            });
            listContainer.addView(optionView);
        }

        // Set transparent background for bottom sheet corners
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialog.show();
    }

    private void saveData() {
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("MED_NAME_" + userEmail, editName.getText().toString().trim());
        editor.putString("MED_HEIGHT_" + userEmail, editHeight.getText().toString().trim());
        editor.putString("MED_WEIGHT_" + userEmail, editWeight.getText().toString().trim());
        editor.putString("MED_ADDRESS_" + userEmail, editAddress.getText().toString().trim());

        editor.putString("MED_SEX_" + userEmail, textSex.getText().toString());
        
        String bloodType = textBloodTypeVal.getText().toString();
        editor.putString("USER_BLOOD_TYPE_" + userEmail, bloodType);
        editor.putString("USER_BLOOD_TYPE", bloodType); // Keep global for backward compatibility in ProfileFragment

        editor.putString("MED_DOB_" + userEmail, textDob.getText().toString());
        editor.putString("MED_DONOR_" + userEmail, textOrganDonor.getText().toString());

        editor.putString("MED_COND_" + userEmail, editConditions.getText().toString().trim());
        editor.putString("MED_MEDS_" + userEmail, editMedications.getText().toString().trim());
        
        String allergies = editAllergies.getText().toString().trim();
        editor.putString("USER_ALLERGIES_" + userEmail, allergies);
        editor.putString("USER_ALLERGIES", allergies); // Global fallback

        editor.putString("MED_REMARKS_" + userEmail, editRemarks.getText().toString().trim());

        editor.apply();
    }
}
