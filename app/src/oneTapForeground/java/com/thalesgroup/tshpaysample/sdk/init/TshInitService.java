/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.sdk.init;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.thalesgroup.tshpaysample.R;
import com.thalesgroup.tshpaysample.sdk.SdkHelper;
import com.thalesgroup.tshpaysample.utlis.AppLoggerHelper;
import com.thalesgroup.tshpaysample.utlis.NotificationUtil;

public class TshInitService extends Service {

    //region Description

    private static final String TAG = TshInitService.class.getSimpleName();
    private static final int FOREGROUND_NOTIFICATION_ID = 7;

    //endregion

    //region Service

    @Nullable
    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(final Intent intent, final int flags, final int startId) {
        AppLoggerHelper.debug(TAG, "onStartCommand");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForeground(FOREGROUND_NOTIFICATION_ID,
                    NotificationUtil.getNotification(this,
                            getString(R.string.foreground_service_message),
                            getString(R.string.foreground_service_channel)));
        }

        // Keep all init logic in one package for better readability.
        SdkHelper.getInstance().getInit().onServiceStartCommand();

        return START_STICKY;
    }

    //endregion
}
