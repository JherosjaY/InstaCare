package com.example.instacare.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;
import com.example.instacare.data.local.ActivityLog;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportExporter {

    public static void exportActivityLogs(Context context, List<ActivityLog> logs) {
        StringBuilder csv = new StringBuilder();
        csv.append("Timestamp,Type,Category,User,Description\n");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        for (ActivityLog log : logs) {
            csv.append(sdf.format(new Date(log.timestamp))).append(",");
            csv.append(escapeCsv(log.type)).append(",");
            csv.append(escapeCsv(log.category)).append(",");
            csv.append(escapeCsv(String.valueOf(log.userId))).append(",");
            csv.append(escapeCsv(log.description)).append("\n");
        }

        shareFile(context, "InstaCare_ActivityLogs.csv", csv.toString());
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static void shareFile(Context context, String fileName, String content) {
        try {
            File file = new File(context.getCacheDir(), fileName);
            FileOutputStream out = new FileOutputStream(file);
            out.write(content.getBytes());
            out.close();

            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", file);
            
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_SUBJECT, "InstaCare Audit Report - " + new Date().toString());
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            context.startActivity(Intent.createChooser(intent, "Export Report"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
