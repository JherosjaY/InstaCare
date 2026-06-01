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
import com.example.instacare.utils.BlurUtils;

public class MedicalInfoActivity extends AppCompatActivity {

    private EditText editName, editHeight, editWeight;
    private TextView textSex, textBloodTypeVal, textDob, textOrganDonor;
    private EditText editConditions, editMedications, editAllergies, editRemarks;

    private SessionManager sessionManager;

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

        sessionManager = SessionManager.getInstance(this);

        initViews();
        loadData();
        setupFocusBehavior();
        setupClickListeners();
    }

    private void initViews() {
        editName = findViewById(R.id.editName);
        editHeight = findViewById(R.id.editHeight);
        editWeight = findViewById(R.id.editWeight);

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
        // Basic Info - AUTO-FILL: Default to USER_NAME if MED_NAME is empty
        String savedMedName = sessionManager.getString("MED_NAME", "");
        if (savedMedName.isEmpty()) {
            editName.setText(sessionManager.getString("USER_NAME", ""));
        } else {
            editName.setText(savedMedName);
        }

        editHeight.setText(sessionManager.getString("MED_HEIGHT", ""));
        editWeight.setText(sessionManager.getString("MED_WEIGHT", ""));

        // Personal Details
        textSex.setText(sessionManager.getString("MED_SEX", "Not set"));
        textBloodTypeVal.setText(sessionManager.getString("USER_BLOOD_TYPE", "Not set"));
        textDob.setText(sessionManager.getString("MED_DOB", "Not set"));
        textOrganDonor.setText(sessionManager.getString("MED_DONOR", "Not set"));

        // Health Info
        editConditions.setText(sessionManager.getString("MED_COND", ""));
        editMedications.setText(sessionManager.getString("MED_MEDS", ""));
        editAllergies.setText(sessionManager.getString("USER_ALLERGIES", ""));
        editRemarks.setText(sessionManager.getString("MED_REMARKS", ""));
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
        BlurUtils.applyBlur(dialog);

        com.shawnlin.numberpicker.NumberPicker pickerMonth = view.findViewById(R.id.pickerMonth);
        com.shawnlin.numberpicker.NumberPicker pickerDay = view.findViewById(R.id.pickerDay);
        com.shawnlin.numberpicker.NumberPicker pickerYear = view.findViewById(R.id.pickerYear);

        View lineMonthTop = view.findViewById(R.id.lineMonthTop);
        View lineMonthBottom = view.findViewById(R.id.lineMonthBottom);
        View lineDayTop = view.findViewById(R.id.lineDayTop);
        View lineDayBottom = view.findViewById(R.id.lineDayBottom);
        View lineYearTop = view.findViewById(R.id.lineYearTop);
        View lineYearBottom = view.findViewById(R.id.lineYearBottom);

        // --- PREPARE DATA: Alternating Directions with Precision Neighbors ---
        
        // MONTH: MM at End, Jan is the neighbor (Index 11)
        // Order: Dec, Nov, Oct, Sep, Aug, Jul, Jun, May, Apr, Mar, Feb, Jan, MM
        String[] months = {"Dec", "Nov", "Oct", "Sep", "Aug", "Jul", "Jun", "May", "Apr", "Mar", "Feb", "Jan", "MM"};
        pickerMonth.setMinValue(0);
        pickerMonth.setMaxValue(months.length - 1);
        pickerMonth.setDisplayedValues(months);
        pickerMonth.setValue(12); // Start at "MM"
        pickerMonth.setWrapSelectorWheel(false);

        // DAY: DD at Start, 1 is the neighbor (Index 1)
        String[] days = new String[32];
        days[0] = "DD";
        for (int i = 1; i <= 31; i++) days[i] = String.valueOf(i);
        pickerDay.setMinValue(0);
        pickerDay.setMaxValue(31);
        pickerDay.setDisplayedValues(days);
        pickerDay.setValue(0); // Start at "DD"
        pickerDay.setWrapSelectorWheel(false);

        // YEAR: YEAR at End, Current Year is the neighbor (Index 101-1 = 100)
        int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        String[] years = new String[102];
        // We want years to go chronologically so scrolling down from "YEAR" reveals the latest first
        // i.e. 1926, 1927, ..., 2025, 2026, YEAR
        for (int i = 0; i <= 100; i++) {
            years[i] = String.valueOf(currentYear - (100 - i)); 
            // This puts (currentYear-100) at 0, and (currentYear) at 100
        }
        years[101] = "YEAR";
        pickerYear.setMinValue(0);
        pickerYear.setMaxValue(101);
        pickerYear.setDisplayedValues(years);
        pickerYear.setValue(101); // Start at "YEAR"
        pickerYear.setWrapSelectorWheel(false);
        
        // --- AUTO-FILL EXISTING DATE ---
        String currentDob = textDob.getText().toString();
        if (!currentDob.equals("Not set") && currentDob.contains("/")) {
            try {
                String[] parts = currentDob.split("/");
                int m = Integer.parseInt(parts[0]);
                int d = Integer.parseInt(parts[1]);
                String y = parts[2];

                // Month index: (12 - m) for reverse array {"Dec", ..., "Jan", "MM"}
                pickerMonth.setValue(12 - m);
                // Day index: d for array {"DD", "1", ..., "31"}
                pickerDay.setValue(d);
                // Year index: search in Chronological array
                for (int i = 0; i < years.length; i++) {
                    if (years[i].equals(y)) {
                        pickerYear.setValue(i);
                        break;
                    }
                }

                // Apply Red highlights immediately if date exists
                int red = androidx.core.content.ContextCompat.getColor(this, R.color.primary);
                
                pickerMonth.setSelectedTextColor(red);
                lineMonthTop.setBackgroundColor(red);
                lineMonthBottom.setBackgroundColor(red);
                
                pickerDay.setSelectedTextColor(red);
                lineDayTop.setBackgroundColor(red);
                lineDayBottom.setBackgroundColor(red);
                
                pickerYear.setSelectedTextColor(red);
                lineYearTop.setBackgroundColor(red);
                lineYearBottom.setBackgroundColor(red);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // --- INTERACTION LOGIC: Incremental Red Highlight (Index-Sensitive) ---
        int redColor = androidx.core.content.ContextCompat.getColor(this, R.color.primary);

        pickerMonth.setOnValueChangedListener((picker, oldVal, newVal) -> {
            if (newVal != 12) { // Not on MM
                pickerMonth.setSelectedTextColor(redColor);
                lineMonthTop.setBackgroundColor(redColor);
                lineMonthBottom.setBackgroundColor(redColor);
            }
        });

        pickerDay.setOnValueChangedListener((picker, oldVal, newVal) -> {
            if (newVal != 0) { // Not on DD
                pickerDay.setSelectedTextColor(redColor);
                lineDayTop.setBackgroundColor(redColor);
                lineDayBottom.setBackgroundColor(redColor);
            }
        });

        pickerYear.setOnValueChangedListener((picker, oldVal, newVal) -> {
            if (newVal != 101) { // Not on YEAR
                pickerYear.setSelectedTextColor(redColor);
                lineYearTop.setBackgroundColor(redColor);
                lineYearBottom.setBackgroundColor(redColor);
            }
        });

        view.findViewById(R.id.btnConfirmDate).setOnClickListener(btnConf -> {
            // Updated Validation for indices
            if (pickerMonth.getValue() == 12 || pickerDay.getValue() == 0 || pickerYear.getValue() == 101) {
                Toast.makeText(this, "Please select a valid Month, Day, and Year!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Month calculation based on reverse array: {"Dec", ..., "Jan", "MM"}
            // Index 0 is Dec (12), Index 11 is Jan (1)
            int val = pickerMonth.getValue();
            int monthNum = 12 - val;

            String mStr = String.format(java.util.Locale.US, "%02d", monthNum);
            String dStr = String.format(java.util.Locale.US, "%02d", pickerDay.getValue());
            String yStr = years[pickerYear.getValue()];
            
            textDob.setText(mStr + "/" + dStr + "/" + yStr);
            dialog.dismiss();
        });

        // Set transparent background for bottom sheet corners
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialog.show();
    }

    private void colorizeDatePicker(android.view.View view) {
        // Redundant helper removed in favor of direct picker targeting for MM/DD/YEAR logic
    }

    private void showSelectionDialog(String title, String[] options, TextView targetView) {
        BottomSheetDialog dialog = new BottomSheetDialog(this, R.style.BottomSheetDialogTheme);
        View view = getLayoutInflater().inflate(R.layout.dialog_bottom_sheet_list, null);
        dialog.setContentView(view);
        BlurUtils.applyBlur(dialog);

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
        sessionManager.putString("MED_NAME", editName.getText().toString().trim());
        sessionManager.putString("MED_HEIGHT", editHeight.getText().toString().trim());
        sessionManager.putString("MED_WEIGHT", editWeight.getText().toString().trim());

        sessionManager.putString("MED_SEX", textSex.getText().toString());
        sessionManager.putString("USER_BLOOD_TYPE", textBloodTypeVal.getText().toString());
        sessionManager.putString("MED_DOB", textDob.getText().toString());
        sessionManager.putString("MED_DONOR", textOrganDonor.getText().toString());

        sessionManager.putString("MED_COND", editConditions.getText().toString().trim());
        sessionManager.putString("MED_MEDS", editMedications.getText().toString().trim());
        sessionManager.putString("USER_ALLERGIES", editAllergies.getText().toString().trim());
        sessionManager.putString("MED_REMARKS", editRemarks.getText().toString().trim());
    }
}
