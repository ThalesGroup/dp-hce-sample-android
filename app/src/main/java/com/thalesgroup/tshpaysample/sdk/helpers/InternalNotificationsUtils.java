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

package com.thalesgroup.tshpaysample.sdk.helpers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.thalesgroup.tshpaysample.sdk.init.TshInitState;
import com.thalesgroup.tshpaysample.sdk.payment.TshPaymentData;
import com.thalesgroup.tshpaysample.sdk.payment.TshPaymentState;
import com.thalesgroup.tshpaysample.sdk.push.TshPushType;

public final class InternalNotificationsUtils {

    //region Defines

    private static final String ACTION_PAYMENT_COUNTDOWN = "com.thalesgroup.tshpaysample.paymentcountdown";
    private static final String ACTION_INIT_STATE_UPDATE = "com.thalesgroup.tshpaysample.initstateupdate";
    private static final String ACTION_PUSH_NOTIFICATION = "com.thalesgroup.tshpaysample.pushnotification";
    private static final String ACTION_VALUE_REMAINING = "ValueRemaining";
    private static final String ACTION_VALUE_STATE = "ValueState";
    private static final String ACTION_VALUE_ERROR = "ValueError";

    //endregion

    //region Public API - Init

    public interface InitChangeHandler {
        void onStateChanged(@NonNull final TshInitState state,
                            @Nullable final String error);
    }

    public static BroadcastReceiver registerForInitChanges(@NonNull final Context context,
                                                           @NonNull final InitChangeHandler handler) {
        final BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                final TshInitState state = (TshInitState) intent.getSerializableExtra(ACTION_VALUE_STATE);
                final String error = intent.getStringExtra(ACTION_VALUE_ERROR);

                handler.onStateChanged(state, error);
            }
        };

        // Handle enrollment state changes.
        final IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_INIT_STATE_UPDATE);
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter);

        return receiver;
    }

    public static void updateInitState(@NonNull final Context context,
                                       @NonNull final TshInitState state,
                                       @Nullable final String error) {
        final Intent sdkState = new Intent(ACTION_INIT_STATE_UPDATE);
        sdkState.putExtra(ACTION_VALUE_STATE, state);
        if (error != null) {
            sdkState.putExtra(ACTION_VALUE_ERROR, error);
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(sdkState);
    }

    //endregion

    //region Public API - Payment

    public interface PaymentStateChangeHandler {
        void onStateChanged(@NonNull final TshPaymentState state,
                            @Nullable final TshPaymentData data);

    }

    public interface PaymentCountdownChangeHandler {
        void onCountdownChanged(final int remainingSec);
    }



    public static BroadcastReceiver registerForPaymentCountdown(@NonNull final Context context,
                                                                @NonNull final PaymentCountdownChangeHandler handler) {
        final BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                final int remainingSec = intent.getIntExtra(ACTION_VALUE_REMAINING, 0);

                handler.onCountdownChanged(remainingSec);
            }
        };

        // Handle enrollment state changes.
        final IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PAYMENT_COUNTDOWN);
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter);

        return receiver;
    }



    public static void updatePaymentCountdown(@NonNull final Context context,
                                              final int remainingSec) {
        final Intent paymentState = new Intent(ACTION_PAYMENT_COUNTDOWN);
        paymentState.putExtra(ACTION_VALUE_REMAINING, remainingSec);

        LocalBroadcastManager.getInstance(context).sendBroadcast(paymentState);
    }
    //endregion

    //region Public API - Push Notifications

    public interface PushNotificationHandler {
        void onPushReceived(@NonNull final TshPushType type,
                            @Nullable final String error);
    }

    public static BroadcastReceiver registerForPushNotifications(@NonNull final Context context,
                                                                 @NonNull final PushNotificationHandler handler) {
        final BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                final TshPushType type = (TshPushType) intent.getSerializableExtra(ACTION_VALUE_STATE);
                final String error = intent.getStringExtra(ACTION_VALUE_ERROR);

                handler.onPushReceived(type, error);
            }
        };

        // Handle enrollment state changes.
        final IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PUSH_NOTIFICATION);
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter);

        return receiver;
    }

    public static void onPushReceived(@NonNull final Context context,
                                      @NonNull final TshPushType type,
                                      @Nullable final String error) {
        final Intent enrollmentState = new Intent(ACTION_PUSH_NOTIFICATION);
        enrollmentState.putExtra(ACTION_VALUE_STATE, type);
        if (error != null) {
            enrollmentState.putExtra(ACTION_VALUE_ERROR, error);
        }
        LocalBroadcastManager.getInstance(context).sendBroadcast(enrollmentState);
    }

    //endregion

}
