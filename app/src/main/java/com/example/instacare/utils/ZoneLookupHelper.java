package com.example.instacare.utils;

import com.example.instacare.data.local.BarangayZone;
import java.util.List;

public class ZoneLookupHelper {

    public static String findZone(double lat, double lon, List<BarangayZone> zones) {
        if (zones == null || zones.isEmpty()) return "Unknown Zone";

        String nearestZone = null;
        double nearestDist = Double.MAX_VALUE;

        for (BarangayZone zone : zones) {
            String coords = zone.boundaryCoords;
            if (coords == null || coords.equals("coords")) continue;

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

                    double centerLat = (minLat + maxLat) / 2.0;
                    double centerLon = (minLon + maxLon) / 2.0;
                    double dist = haversine(lat, lon, centerLat, centerLon);
                    if (dist < nearestDist) {
                        nearestDist = dist;
                        nearestZone = zone.name;
                    }
                }
            } catch (Exception ignored) {}
        }

        if (nearestZone != null) return nearestZone;

        int idx = Math.abs((int)(lat * 10000 + lon * 10000)) % zones.size();
        return zones.get(idx).name;
    }

    private static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
