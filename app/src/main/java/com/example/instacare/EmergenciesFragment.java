package com.example.instacare;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.instacare.data.local.AppDatabase;
import com.example.instacare.data.local.EmergencyAlert;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class EmergenciesFragment extends Fragment {

    private RecyclerView recyclerView;
    private LinearLayout emptyState;
    private EmergencyAlertAdapter adapter;
    private AppDatabase db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_emergencies, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        db = AppDatabase.getDatabase(requireContext());
        recyclerView = view.findViewById(R.id.emergenciesRecyclerView);
        emptyState = view.findViewById(R.id.emptyState);
        
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new EmergencyAlertAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        loadEmergencies();
    }

    private void loadEmergencies() {
        SessionManager sessionManager = SessionManager.getInstance(requireContext());
        String selectedCommunity = sessionManager.getString("SELECTED_COMMUNITY", null);
        boolean isBarangay = sessionManager.getBoolean("IS_BARANGAY", false);
        boolean isAdmin = sessionManager.getBoolean("IS_ADMIN", false);

        Executors.newSingleThreadExecutor().execute(() -> {
            List<EmergencyAlert> emergencies = new ArrayList<>();
            
            // STRICT ISOLATION POLICY: 
            // 1. Barangay role is locked to THEIR zone ONLY.
            // 2. Admin role sees EVERYTHING (General view).
            
            if (isBarangay && selectedCommunity != null) {
                // Focus strictly on local zone
                emergencies = db.emergencyAlertDao().getAlertsByBarangay(selectedCommunity);
            } else if (isAdmin) {
                // Admin gets the bird's-eye view
                emergencies = db.emergencyAlertDao().getAllAlerts();
            } else if (isBarangay) {
                // Barangay but community missing? Strict empty result (Security safety)
                emergencies = new ArrayList<>();
            } else {
                // Default fallback (e.g. User checking their own status if needed)
                emergencies = db.emergencyAlertDao().getAllAlerts();
            }

            final List<EmergencyAlert> results = emergencies;
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (results.isEmpty()) {
                        emptyState.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                        // Update text for Barangay context
                        if (isBarangay) {
                            String displayBrgy = selectedCommunity != null ? selectedCommunity.replace(" Zone", "") : "your area";
                            ((android.widget.TextView)emptyState.findViewById(R.id.tvEmptyTitle)).setText("No active emergencies in " + displayBrgy);
                        }
                    } else {
                        emptyState.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                        adapter.updateData(results);
                    }
                });
            }
        });
    }
}
