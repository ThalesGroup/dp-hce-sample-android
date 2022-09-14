/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.sdk.push;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.thalesgroup.tshpaysample.sdk.SdkHelper;
import com.thalesgroup.tshpaysample.utlis.AppLoggerHelper;

public class FcmService extends FirebaseMessagingService {

    //region Defines

    private static final String TAG = FcmService.class.getSimpleName();

    //endregion

    //region FirebaseMessagingService

    @Override
    public void onNewToken(final @NonNull String token) {
        super.onNewToken(token);

        // Make sure, that token is up-to date.
        SdkHelper.getInstance().getPush().updateToken(this, token);
    }

    @Override
    public void onMessageReceived(@NonNull final RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Process incoming message in common class.
        SdkHelper.getInstance().getPush().onMessageReceived(this, remoteMessage.getData());
    }

    //endregion

    //region Public API

    public static boolean isAvailable(final Context context) {
        boolean isAvailable = false;
        if (context != null) {
            final int result = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context);
            isAvailable = com.google.android.gms.common.ConnectionResult.SUCCESS == result;
        }
        return isAvailable;
    }

    public static void init(final Context context) {
        // This approach will return FCM token each time. Even if it's not changed.
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Make sure, that token is up-to date.
                SdkHelper.getInstance().getPush().updateToken(context, task.getResult());
            } else {
                AppLoggerHelper.exception(TAG, "Fetching FCM registration token failed", task.getException());
            }
        });
    }

    //endregion

}
