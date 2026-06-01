package com.example.instacare.utils;

import java.util.Locale;

/**
 * 📏 Centralized Utility to prevent distance discrepancies between List and Map.
 * Ensures consistent rounding and formatting for KM.
 */
public class DistanceUtils {

    /**
     * Formats distance in KM with consistent precision.
     * @param km Distance in kilometers.
     * @return Formatted string (e.g., "5.2 km", "124 km")
     */
    public static String formatDistance(double km) {
        if (km > 100) {
            return String.format(Locale.US, "%.0f km", km);
        } else {
            return String.format(Locale.US, "%.1f km", km);
        }
    }

    /**
     * Converts meters to KM and formats.
     */
    public static String formatMeters(double meters) {
        return formatDistance(meters / 1000.0);
    }
}
