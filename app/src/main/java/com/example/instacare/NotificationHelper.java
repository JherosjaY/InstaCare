package com.example.instacare;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

public class NotificationHelper {

    private static final String CHANNEL_ID = "instacare_broadcasts";
    private static final String CHANNEL_NAME = "Admin Broadcasts";
    private static final String CHANNEL_DESC = "Notifications sent by the InstaCare Administrator";

    public static final String TIP_CHANNEL_ID = "instacare_tips";
    private static final String TIP_CHANNEL_NAME = "Cara Tips";
    private static final String TIP_CHANNEL_DESC = "Helpful tips from Cara AI";
    private static final int TIP_NOTIFICATION_ID = 9999;

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);
            channel.enableVibration(true);

            NotificationChannel tipChannel = new NotificationChannel(
                    TIP_CHANNEL_ID,
                    TIP_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
            );
            tipChannel.setDescription(TIP_CHANNEL_DESC);
            tipChannel.setShowBadge(false);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
                manager.createNotificationChannel(tipChannel);
            }
        }
    }

    public static void sendLocalNotification(Context context, String title, String message) {
        Intent intent = new Intent(context, UserDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bell)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    public static void sendTipNotification(Context context, String tipText) {
        Intent intent = new Intent(context, UserDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, TIP_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bell)
                .setContentTitle("Tip from Cara")
                .setContentText(tipText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(tipText))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(TIP_NOTIFICATION_ID, builder.build());
        }
    }
}
