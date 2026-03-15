package com.example.instacare;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.instacare.databinding.ActivitySosBinding;

public class SOSActivity extends AppCompatActivity {

    private ActivitySosBinding binding;
    private CountDownTimer countDownTimer;
    
    // Smart SOS Components
    private android.hardware.camera2.CameraManager cameraManager;
    private String cameraId;
    private boolean isFlashlightOn = false;
    private MediaPlayer sirenPlayer;
    private boolean isSirenOn = false;
    private android.os.Handler blinkHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable blinkRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // Hide System Bars for Immersive Effect
        getWindow().setStatusBarColor(androidx.core.content.ContextCompat.getColor(this, R.color.sos_background));
        getWindow().setNavigationBarColor(androidx.core.content.ContextCompat.getColor(this, R.color.sos_background));

        binding.btnImSafe.setOnClickListener(v -> finish());
        
        populateHotlines();
    }
    
    private void populateHotlines() {
        android.widget.LinearLayout container = binding.hotlinesContainer;
        if (container == null) return;

        HotlineHelper.populateCategorized(this, container, number -> makeCall(number));
    }
    
    private String currentNumberToCall = null;
    private static final int CALL_PERMISSION_REQUEST_CODE = 2;

    private void makeCall(String number) {
        currentNumberToCall = number;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, CALL_PERMISSION_REQUEST_CODE);
        } else {
            performCall(number);
        }
    }

    private void performCall(String number) {
        if (number == null || number.isEmpty()) return;
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + number));
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions, @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CALL_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            performCall(currentNumberToCall);
        } else if (requestCode == CALL_PERMISSION_REQUEST_CODE) {
            Toast.makeText(this, "Permission denied. Cannot make calls.", Toast.LENGTH_SHORT).show();
        }
    }
}
