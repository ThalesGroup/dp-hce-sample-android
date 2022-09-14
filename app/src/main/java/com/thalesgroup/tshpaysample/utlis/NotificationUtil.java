/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.utlis;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import com.thalesgroup.tshpaysample.R;

public class NotificationUtil {

    //region Public API

    public static Notification getNotification(final Context context,
                                               final String contentMessage,
                                               final String channelId) {
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = showChannel(context, channelId);
        } else {
            builder = new Notification.Builder(context);
        }
        return builder.setAutoCancel(false)
                .setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(true)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(contentMessage)
                .setPriority(Notification.PRIORITY_MAX)
                .build();
    }

    //endregion

    //region Private Helpers

    @TargetApi(26)
    private static Notification.Builder showChannel(final Context context,
                                                    final String channelId) {
        final NotificationChannel channel = new NotificationChannel(channelId,
                channelId, NotificationManager.IMPORTANCE_LOW);
        ((NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE))
                .createNotificationChannel(channel);
        return new Notification.Builder(context, channelId);
    }

    //endregion

}
