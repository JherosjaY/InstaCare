package com.example.instacare;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.button.MaterialButton;

public class EvacuationDetailsBottomSheet extends BaseBlurredBottomSheet {

    private static final String ARG_NAME = "arg_name";
    private static final String ARG_ADDRESS = "arg_address";
    private static final String ARG_TYPE = "arg_type";
    private static final String ARG_STATUS = "arg_status";
    private static final String ARG_CAPACITY = "arg_capacity";
    private static final String ARG_OCCUPIED = "arg_occupied";
    private static final String ARG_CONTACT = "arg_contact";
    private static final String ARG_AMENITIES = "arg_amenities";
    private static final String ARG_DISTANCE = "arg_distance";
    private static final String ARG_LAT = "arg_lat";
    private static final String ARG_LNG = "arg_lng";
    private static final String ARG_PHOTO_PATH = "arg_photo_path";

    public static EvacuationDetailsBottomSheet newInstance(String name, String address, String type,
                                                            String status, int capacity, int occupied,
                                                            String contact, String amenities,
                                                            String distance, double lat, double lng) {
        return newInstance(name, address, type, status, capacity, occupied, contact, amenities, distance, lat, lng, "");
    }

    public static EvacuationDetailsBottomSheet newInstance(String name, String address, String type,
                                                            String status, int capacity, int occupied,
                                                            String contact, String amenities,
                                                            String distance, double lat, double lng,
                                                            String photoPath) {
        EvacuationDetailsBottomSheet fragment = new EvacuationDetailsBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_NAME, name);
        args.putString(ARG_ADDRESS, address);
        args.putString(ARG_TYPE, type);
        args.putString(ARG_STATUS, status);
        args.putInt(ARG_CAPACITY, capacity);
        args.putInt(ARG_OCCUPIED, occupied);
        args.putString(ARG_CONTACT, contact);
        args.putString(ARG_AMENITIES, amenities);
        args.putString(ARG_DISTANCE, distance);
        args.putDouble(ARG_LAT, lat);
        args.putDouble(ARG_LNG, lng);
        args.putString(ARG_PHOTO_PATH, photoPath);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_evacuation_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView detailName = view.findViewById(R.id.evacDetailName);
        TextView detailStatus = view.findViewById(R.id.evacDetailStatus);
        TextView detailType = view.findViewById(R.id.evacDetailType);
        TextView detailAddress = view.findViewById(R.id.evacDetailAddress);
        TextView detailDistance = view.findViewById(R.id.evacDetailDistance);
        TextView detailContact = view.findViewById(R.id.evacDetailContact);
        TextView detailAmenities = view.findViewById(R.id.evacDetailAmenities);
        TextView detailCapacityText = view.findViewById(R.id.evacDetailCapacityText);
        ProgressBar detailCapacityBar = view.findViewById(R.id.evacDetailCapacityBar);
        MaterialButton btnDirections = view.findViewById(R.id.btnDirections);
        com.google.android.material.card.MaterialCardView cardDetailPhoto = view.findViewById(R.id.cardDetailPhoto);
        android.widget.ImageView ivDetailPhoto = view.findViewById(R.id.ivDetailPhoto);

        if (getArguments() != null) {
            String name = getArguments().getString(ARG_NAME);
            String address = getArguments().getString(ARG_ADDRESS);
            String type = getArguments().getString(ARG_TYPE);
            String status = getArguments().getString(ARG_STATUS);
            int capacity = getArguments().getInt(ARG_CAPACITY);
            int occupied = getArguments().getInt(ARG_OCCUPIED);
            String contact = getArguments().getString(ARG_CONTACT);
            String amenities = getArguments().getString(ARG_AMENITIES);
            String distance = getArguments().getString(ARG_DISTANCE);
            double lat = getArguments().getDouble(ARG_LAT);
            double lng = getArguments().getDouble(ARG_LNG);
            String photoPath = getArguments().getString(ARG_PHOTO_PATH, "");

            // Photo
            if (photoPath != null && !photoPath.isEmpty() && ivDetailPhoto != null) {
                java.io.File photoFile = new java.io.File(photoPath);
                if (photoFile.exists()) {
                    android.net.Uri photoUri = android.net.Uri.fromFile(photoFile);
                    com.bumptech.glide.Glide.with(requireContext())
                        .load(photoUri)
                        .centerCrop()
                        .into(ivDetailPhoto);
                    if (cardDetailPhoto != null) cardDetailPhoto.setVisibility(View.VISIBLE);
                } else {
                    if (cardDetailPhoto != null) cardDetailPhoto.setVisibility(View.GONE);
                }
            } else {
                if (cardDetailPhoto != null) cardDetailPhoto.setVisibility(View.GONE);
            }

            detailName.setText(name != null ? name : "Evacuation Center");

            if (type != null && !type.isEmpty()) {
                detailType.setText(type);
                detailType.setVisibility(View.VISIBLE);
            } else {
                detailType.setVisibility(View.GONE);
            }

            detailAddress.setText(address != null && !address.trim().isEmpty() ? address : "Address not available");
            detailDistance.setText(distance != null ? distance + " away" : "");

            // Status badge
            String st = status != null ? status : "Open";
            detailStatus.setText(st);
            int statusColor;
            switch (st) {
                case "Full": statusColor = 0xFFF59E0B; break;
                case "Closed": statusColor = 0xFFEF4444; break;
                default: statusColor = 0xFF22C55E; break;
            }
            detailStatus.setBackgroundTintList(android.content.res.ColorStateList.valueOf(statusColor));

            // Capacity
            if (capacity > 0) {
                detailCapacityText.setVisibility(View.VISIBLE);
                detailCapacityBar.setVisibility(View.VISIBLE);
                int progress = (int) ((float) occupied / capacity * 100);
                detailCapacityBar.setProgress(Math.min(progress, 100));
                detailCapacityText.setText(occupied + "/" + capacity);
                int occColor;
                float pct = (float) occupied / capacity;
                if (pct >= 0.85f) occColor = 0xFFE53935;
                else if (pct >= 0.60f) occColor = 0xFFFB8C00;
                else occColor = 0xFF43A047;
                detailCapacityBar.setProgressTintList(android.content.res.ColorStateList.valueOf(occColor));
            } else {
                detailCapacityText.setVisibility(View.GONE);
                detailCapacityBar.setVisibility(View.GONE);
            }

            // Contact
            if (contact != null && !contact.trim().isEmpty()) {
                detailContact.setText("Contact: " + contact);
                detailContact.setVisibility(View.VISIBLE);
            } else {
                detailContact.setVisibility(View.GONE);
            }

            // Amenities
            if (amenities != null && !amenities.trim().isEmpty()) {
                detailAmenities.setText("Amenities: " + amenities);
                detailAmenities.setVisibility(View.VISIBLE);
            } else {
                detailAmenities.setVisibility(View.GONE);
            }

            // Directions button
            btnDirections.setOnClickListener(v -> {
                Uri gmmIntentUri = Uri.parse("google.navigation:q=" + lat + "," + lng);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                if (mapIntent.resolveActivity(requireContext().getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    Uri webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + lat + "," + lng);
                    startActivity(new Intent(Intent.ACTION_VIEW, webUri));
                }
            });
        }
    }
}
