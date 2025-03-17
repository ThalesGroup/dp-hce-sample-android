/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.sdk.push;

import android.content.Context;

import androidx.annotation.NonNull;

import com.huawei.hms.api.HuaweiApiAvailability;
import com.huawei.hms.push.HmsMessageService;
import com.huawei.hms.push.RemoteMessage;
import com.thalesgroup.tshpaysample.R;
import com.thalesgroup.tshpaysample.sdk.SdkHelper;

public class HmsService extends HmsMessageService {

    //region Defines

    private static final String HMS_TOKEN_PREFIX = "HMS:";

    private static boolean sIsHuaweiMainProvider = false;

    // TODO: Is this enough or wee need to call
    //  String token = HmsInstanceId.getInstance(MainActivity.this).getToken(appId, tokenScope);
    //  It's ugly to need provide app id from json file. https://developer.huawei.com/consumer/en/doc/HMSCore-Guides/android-client-dev-0000001050042041

    private static String sToken;

    //endregion

    //region HmsMessageService

    @Override
    public void onNewToken(final @NonNull String token) {
        super.onNewToken(token);

        sToken = token;

        // Some Huawei devices might also support google services in which case they
        // they are preferred.
        if (!sIsHuaweiMainProvider) {
            return;
        }

        // Make sure, that token is up-to date.
        SdkHelper.getInstance().getPush().updateToken(this, HMS_TOKEN_PREFIX + token);
    }

    @Override
    public void onMessageReceived(@NonNull final RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Process incoming message in common class.
        SdkHelper.getInstance().getPush().onMessageReceived(this, remoteMessage.getDataOfMap());
    }

    //endregion

    //region Public API

    public static boolean isAvailable(final Context context) {
        boolean isAvailable = false;
        if (context != null) {
            final int result = HuaweiApiAvailability.getInstance().isHuaweiMobileServicesAvailable(context);
            isAvailable = com.huawei.hms.api.ConnectionResult.SUCCESS == result;
        }
        return isAvailable;
    }

    public static void init() {
        sIsHuaweiMainProvider = true;
    }

    public static void getPushToken(@NonNull final Context context,
                                    @NonNull final TshPush.PushTokenListener completion) {
        if (sToken != null) {
            completion.onComplete(sToken);
        } else {
            throw new IllegalStateException(context.getString(R.string.push_token_missing));
        }
    }

    //endregion

}
