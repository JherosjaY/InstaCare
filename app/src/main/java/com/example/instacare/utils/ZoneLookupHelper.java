package com.example.instacare.utils;

import com.example.instacare.data.local.BarangayZone;
import java.util.List;

public class ZoneLookupHelper {
    /**
     * Finds the matching barangay zone for a given set of coordinates.
     * For now, this is a placeholder that returns a zone name based on range.
     * In a real app, this would use Poly-in-Point logic.
     */
    public static String findZone(double lat, double lon, List<BarangayZone> zones) {
        if (zones == null || zones.isEmpty()) return "Unknown Zone";
        
        for (BarangayZone zone : zones) {
            String coords = zone.boundaryCoords;
            if (coords != null && !coords.equals("coords")) {
                try {
                    String[] parts = coords.split(",");
                    if (parts.length == 4) {
                        double minLat = Double.parseDouble(parts[0]);
                        double maxLat = Double.parseDouble(parts[1]);
                        double minLon = Double.parseDouble(parts[2]);
                        double maxLon = Double.parseDouble(parts[3]);
                        
                        if (lat >= minLat && lat <= maxLat && lon >= minLon && lon <= maxLon) {
                            return zone.name;
                        }
                    }
                } catch (Exception e) {
                    // Fallback to name search if parsing fails
                }
            }
        }
        
        // Fallback for demo: if no coordinate match, find a zone with "Zone" in name
        for (BarangayZone zone : zones) {
            if (zone.name.toLowerCase().contains("zone")) {
                return zone.name;
            }
        }
        return zones.get(0).name;
    }
}
