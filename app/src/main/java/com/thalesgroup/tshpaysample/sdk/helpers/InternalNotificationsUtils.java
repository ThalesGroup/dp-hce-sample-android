/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.sdk.helpers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.thalesgroup.tshpaysample.sdk.push.ServerMessageInfo;
import com.thalesgroup.tshpaysample.utlis.AppLoggerHelper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class InternalNotificationsUtils {

    //region Defines

    private static final String ACTION_PUSH_MSG_PROCESSED = "com.thalesgroup.tshpaysample.ACTION_PUSH_MSG_PROCESSED";
    private static final String ACTION_PUSH_MSG_ERROR = "com.thalesgroup.tshpaysample.ACTION_PUSH_MSG_ERROR";

    private static final String EXTRA_ERROR_MESSAGE = "com.thalesgroup.tshpaysample.EXTRA_ERROR_MESSAGE";
    private static final String EXTRA_SERVER_MESSAGES = "com.thalesgroup.tshpaysample.EXTRA_SERVER_MESSAGES";
    private static final String TAG = InternalNotificationsUtils.class.getSimpleName();

    //endregion


    //region Public API - Push Notifications

    public interface PushMsgResultHandler {
        void onPushProcessed(@NonNull final List<ServerMessageInfo> serverMessageInfoList,
                             @Nullable final String error);
    }

    public static BroadcastReceiver registerForPushMsgProcessingResult(@NonNull final Context context,
                                                                       @NonNull final PushMsgResultHandler handler) {
        final BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {

                if(intent == null) {
                    throw new IllegalArgumentException("Intent cannot be null");
                }

                if (intent.hasExtra(EXTRA_ERROR_MESSAGE)) {
                    final String error = intent.getStringExtra(EXTRA_ERROR_MESSAGE);
                    handler.onPushProcessed(null, error);
                } else if (intent.hasExtra(EXTRA_SERVER_MESSAGES)) {
                    final Serializable retrievedExtra = intent.getSerializableExtra(EXTRA_SERVER_MESSAGES);
                    try{
                        final ArrayList<ServerMessageInfo> serverMessageInfos = (ArrayList<ServerMessageInfo>) retrievedExtra;
                        handler.onPushProcessed(serverMessageInfos, null);

                    } catch (final Exception exception){
                        AppLoggerHelper.error(TAG, "Failed to retrieve server messages from intent: " + exception.getMessage());
                    }
                }

            }
        };

        // Handle enrollment state changes.
        final IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PUSH_MSG_PROCESSED);
        filter.addAction(ACTION_PUSH_MSG_ERROR);
        LocalBroadcastManager.getInstance(context).registerReceiver(receiver, filter);

        return receiver;
    }


    public static void onPushMsgProcessingFailed(@NonNull final Context context, @NonNull final String error) {
        final Intent intent = new Intent(ACTION_PUSH_MSG_ERROR);
        intent.putExtra(EXTRA_ERROR_MESSAGE, error);

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    public static void onPushProcessingCompleted(@NonNull final Context context, @NonNull final List<ServerMessageInfo> messages) {
        final Intent intent = new Intent(ACTION_PUSH_MSG_PROCESSED);
        intent.putExtra(EXTRA_SERVER_MESSAGES, new ArrayList<>(messages));

        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    //endregion

}
