package com.example.instacare.utils;

import java.util.Random;
import java.util.Calendar;

public class CaseRefGenerator {
    /**
     * Generates a unique Case Reference Number in the format: IC-YYYY-XXXX
     * IC = InstaCare
     * YYYY = Current Year
     * XXXX = 4-digit random number (to be replaced by DB counter later if needed)
     */
    public static String generate() {
        int year = Calendar.getInstance().get(Calendar.YEAR);
        int randomNum = new Random().nextInt(9000) + 1000; // 1000 to 9999
        return String.format("IC-%d-%d", year, randomNum);
    }
}
