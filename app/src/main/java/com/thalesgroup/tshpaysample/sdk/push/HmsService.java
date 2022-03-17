/*
 * MIT License
 *
 * Copyright (c) 2021 Thales DIS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.thalesgroup.tshpaysample.sdk.push;

import android.content.Context;

import androidx.annotation.NonNull;

import com.huawei.hms.api.HuaweiApiAvailability;
import com.huawei.hms.push.HmsMessageService;
import com.huawei.hms.push.RemoteMessage;
import com.thalesgroup.tshpaysample.sdk.SdkHelper;

public class HmsService extends HmsMessageService {

    //region Defines

    private static final String HMS_TOKEN_PREFIX = "HMS:";

    private static boolean sIsHuaweiMainProvider = false;

    //endregion

    //region HmsMessageService

    @Override
    public void onNewToken(final @NonNull String token) {
        super.onNewToken(token);

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

    //endregion

}
