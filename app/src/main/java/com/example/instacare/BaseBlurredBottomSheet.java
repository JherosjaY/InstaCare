package com.example.instacare;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.example.instacare.utils.BlurUtils;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Base class to ensure all BottomSheets exhibit the Premium blurred background.
 */
public class BaseBlurredBottomSheet extends BottomSheetDialogFragment {

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            // Apply the Glassmorphism blur effect globally
            BlurUtils.applyBlur(dialog);
            
            // Ensure transparent window background for correct corner rendering
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }
        }
    }
}
