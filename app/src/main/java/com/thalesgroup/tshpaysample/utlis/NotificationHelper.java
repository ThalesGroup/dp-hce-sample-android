package com.thalesgroup.tshpaysample.utlis;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationHelper {
    private static final String CHANNEL_ID = "com.thalesgroup.tshpaysample.notification.channel";
    private static final int NOTIFICATION_ID = 1;
    private static final String TAG = NotificationHelper.class.getSimpleName();

    public static void showNotification(final Context context,
                                        final String title,
                                        final String message) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            AppLoggerHelper.warn(TAG, "Notification permission has not been granted!");
            return;
        }

        // Create notification channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final int importance = NotificationManager.IMPORTANCE_DEFAULT;
            final NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "NFC Wallet SDK Sample Channel", importance);
            channel.setDescription("The sample app will post notifications to this channel");
            final NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        // Build the notification
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)  // Icon shown in the status bar
                .setContentTitle(title)  // Notification title
                .setContentText(message)  // Notification message
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);  // Priority for Android < 8.0

        // Show the notification
        final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}
