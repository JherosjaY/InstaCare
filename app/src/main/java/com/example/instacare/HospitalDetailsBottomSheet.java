package com.example.instacare;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;

public class HospitalDetailsBottomSheet extends BottomSheetDialogFragment {

    private static final String ARG_NAME = "arg_name";
    private static final String ARG_IMAGE_URL = "arg_image_url";
    private static final String ARG_ADDRESS = "arg_address";
    private static final String ARG_DISTANCE = "arg_distance";
    private static final String ARG_PHONE = "arg_phone";
    private static final String ARG_TYPE = "arg_type";
    private static final String ARG_CAPACITY = "arg_capacity";
    private static final String ARG_SERVICES = "arg_services";

    public static HospitalDetailsBottomSheet newInstance(String name, String imageUrl, String address, 
                                                         String distance, String phone, String type, 
                                                         String capacity, String services) {
        HospitalDetailsBottomSheet fragment = new HospitalDetailsBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_NAME, name);
        args.putString(ARG_IMAGE_URL, imageUrl);
        args.putString(ARG_ADDRESS, address);
        args.putString(ARG_DISTANCE, distance);
        args.putString(ARG_PHONE, phone);
        args.putString(ARG_TYPE, type);
        args.putString(ARG_CAPACITY, capacity);
        args.putString(ARG_SERVICES, services);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_hospital_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView detailImage = view.findViewById(R.id.detailImage);
        TextView detailName = view.findViewById(R.id.detailName);
        TextView detailCapacity = view.findViewById(R.id.detailCapacity);
        TextView detailType = view.findViewById(R.id.detailType);
        TextView detailAddress = view.findViewById(R.id.detailAddress);
        TextView detailDistance = view.findViewById(R.id.detailDistance);
        TextView detailPhone = view.findViewById(R.id.detailPhone);
        TextView detailServices = view.findViewById(R.id.detailServices);
        MaterialButton btnCallHotline = view.findViewById(R.id.btnCallHotline);

        if (getArguments() != null) {
            String name = getArguments().getString(ARG_NAME);
            String imageUrl = getArguments().getString(ARG_IMAGE_URL);
            String address = getArguments().getString(ARG_ADDRESS);
            String distance = getArguments().getString(ARG_DISTANCE);
            String phone = getArguments().getString(ARG_PHONE);
            String type = getArguments().getString(ARG_TYPE);
            String capacity = getArguments().getString(ARG_CAPACITY);
            String services = getArguments().getString(ARG_SERVICES);

            detailName.setText(name != null ? name : "Unknown Hospital");
            detailType.setText(type != null ? type : "Hospital");
            detailAddress.setText(address != null && !address.trim().isEmpty() ? address : "Address not available");
            detailDistance.setText(distance != null ? distance : "");
            
            if (phone != null && !phone.trim().isEmpty()) {
                detailPhone.setText(phone);
                detailPhone.setVisibility(View.VISIBLE);
                btnCallHotline.setEnabled(true);
            } else {
                detailPhone.setVisibility(View.GONE);
                btnCallHotline.setEnabled(false);
                btnCallHotline.setText("No Hotline Available");
            }

            if (services != null && !services.trim().isEmpty()) {
                detailServices.setText("Services: " + services);
                detailServices.setVisibility(View.VISIBLE);
            } else {
                detailServices.setVisibility(View.GONE);
            }
            
            if (capacity != null) {
                detailCapacity.setText(capacity);
                if (capacity.equalsIgnoreCase("Full")) {
                    detailCapacity.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFE53935)); // Red
                } else if (capacity.equalsIgnoreCase("Moderate")) {
                    detailCapacity.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFB8C00)); // Orange
                } else {
                    detailCapacity.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF43A047)); // Green
                }
            } else {
                detailCapacity.setVisibility(View.GONE);
            }

            if (imageUrl != null && !imageUrl.isEmpty()) {
                int resId = getResources().getIdentifier(imageUrl, "drawable", requireContext().getPackageName());
                Object loadTarget = resId != 0 ? resId : imageUrl;
                Glide.with(this)
                    .load(loadTarget)
                    .placeholder(R.drawable.bg_header_rounded)
                    .error(R.drawable.bg_header_rounded)
                    .into(detailImage);
            }

            btnCallHotline.setOnClickListener(v -> {
                if (phone != null && !phone.trim().isEmpty()) {
                    if (androidx.core.content.ContextCompat.checkSelfPermission(requireContext(), android.Manifest.permission.CALL_PHONE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        requestPermissions(new String[]{android.Manifest.permission.CALL_PHONE}, 1);
                    } else {
                        Intent intent = new Intent(Intent.ACTION_CALL);
                        intent.setData(Uri.parse("tel:" + phone));
                        startActivity(intent);
                    }
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            String phone = getArguments() != null ? getArguments().getString(ARG_PHONE) : null;
            if (phone != null && !phone.trim().isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_CALL);
                intent.setData(Uri.parse("tel:" + phone));
                startActivity(intent);
            }
        } else if (requestCode == 1) {
            android.widget.Toast.makeText(requireContext(), "Permission denied. Cannot make calls.", android.widget.Toast.LENGTH_SHORT).show();
        }
    }
}
