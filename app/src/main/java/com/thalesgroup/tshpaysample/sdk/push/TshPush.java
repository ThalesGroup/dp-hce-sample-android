/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.sdk.push;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gemalto.mfs.mwsdk.mobilegateway.MGSDKConfigurationState;
import com.gemalto.mfs.mwsdk.provisioning.ProvisioningServiceManager;
import com.gemalto.mfs.mwsdk.provisioning.listener.PushServiceListener;
import com.gemalto.mfs.mwsdk.provisioning.model.ProvisioningServiceError;
import com.gemalto.mfs.mwsdk.provisioning.model.ProvisioningServiceMessage;
import com.gemalto.mfs.mwsdk.provisioning.sdkconfig.ProvisioningBusinessService;
import com.thalesgroup.tshpaysample.R;
import com.thalesgroup.tshpaysample.sdk.SdkHelper;
import com.thalesgroup.tshpaysample.sdk.helpers.CardWrapper;
import com.thalesgroup.tshpaysample.sdk.helpers.InternalNotificationsUtils;
import com.thalesgroup.tshpaysample.utlis.AppLoggerHelper;

import java.util.Map;
import java.util.Stack;

public final class TshPush implements PushServiceListener {

    //region Defines

    interface TshMessageDelegate {
        void onMessageProcessed(final Bundle bundle,
                                final TshPushSender sender,
                                final String action,
                                final String digitalizedCardId);
    }

    public interface PushTokenListener {
        void onComplete(final String token);
    }

    private static final String TAG = TshPush.class.getSimpleName();

    private static final String KEY_SENDER = "sender";
    private static final String KEY_ACTION = "action";
    private static final String KEY_DIGITALIZED_CARD_ID = "digitalCardID";
    private static final String KEY_REPLENISHMENT = "MG:ReplenishmentNeededNotification";

    private Context mContext;

    // Used for tracking server message codes that were processed
    private Stack<String> mPushServerMessageCodes;

    //endregion

    //region Public API

    /**
     * Main entry point which need to be called as soon as the app will run.
     */
    public void init(@NonNull final Context context) {
        mContext = context;
        mPushServerMessageCodes = new Stack<>();

        // Decide which push notification service we want to use.
        if (FcmService.isAvailable(context)) {
            // Google play store
            FcmService.init(context);
        } else if (HmsService.isAvailable(context)) {
            // Huawei app gallery.
            HmsService.init();
        } else {
            throw new IllegalStateException(context.getString(R.string.push_provider_missing));
        }
    }

    public void getPushToken(final PushTokenListener completion) {
        if (FcmService.isAvailable(mContext)) {
            FcmService.getPushToken(mContext, completion);
        } else if (HmsService.isAvailable(mContext)) {
            HmsService.getPushToken(mContext, completion);
        } else {
            throw new IllegalStateException(mContext.getString(R.string.push_provider_missing));
        }
    }

    //endregion

    //region Protected API

    void updateToken(@NonNull final Context context,
                     @Nullable final String token) {
        final ProvisioningBusinessService provisioningService = ProvisioningServiceManager.getProvisioningBusinessService();
        provisioningService.updatePushToken(token, new PushServiceListener() {
            @Override
            public void onError(final ProvisioningServiceError provisioningServiceError) {
                AppLoggerHelper.error(TAG, provisioningServiceError.getErrorMessage());
                // TODO: Catch original error and describe that it's not an issue.

            }

            @Override
            public void onUnsupportedPushContent(final Bundle bundle) {
                // This method is not relevant for push update method.
            }

            @Override
            public void onServerMessage(final String message,
                                        final ProvisioningServiceMessage provisioningServiceMessage) {
                // This method is not relevant for push update method.
            }

            @Override
            public void onComplete() {
            }
        });
    }

    void onMessageReceived(@NonNull final Context context,
                           @NonNull final Map<String, String> data) {
        processTshMessage(context, data, (bundle, sender, action, digitalizedCardId) -> {
            switch (sender) {
                case CPS:
                    // Main notification that needs to be handled in order to progress with
                    // enrollment, replenishment, card state changes etc...
                    final ProvisioningBusinessService provService = ProvisioningServiceManager.getProvisioningBusinessService();
                    provService.processIncomingMessage(bundle, TshPush.this);
                    break;
                case MG:
                    // We run out of keys and backend is asking wallet to force replenishment.
                    if (KEY_REPLENISHMENT.equalsIgnoreCase(action) &&
                            digitalizedCardId != null && !digitalizedCardId.isEmpty()) {
                        new CardWrapper(digitalizedCardId).replenishKeysIfNeeded(true);
                    }
                    break;
                case TNS:
                    // Transaction history notification. Not in current scope of sample application.
                    AppLoggerHelper.error(TAG, context.getString(R.string.push_received_transaction_history));
                    break;
                case UNKNOWN:
                    // Notification is not from Tsh. This is example how app can handle
                    // own push notification.
                    AppLoggerHelper.error(TAG, context.getString(R.string.push_received_unknown));
                    break;
            }
        });
    }

    //endregion

    //region Private Helpers

    private void processTshMessage(@NonNull final Context context,
                                   @NonNull final Map<String, String> data,
                                   @NonNull final TshMessageDelegate delegate) {
        String retSender = "";
        String retDigitalCardID = "";
        String retAction = "";

        final Bundle bundle = new Bundle();
        if (!data.isEmpty()) {
            for (final String loopKey : data.keySet()) {
                final String value = data.get(loopKey);
                AppLoggerHelper.debug(TAG, loopKey + " ---|--- " + value);
                if (null != data.get(loopKey)) {
                    bundle.putString(loopKey, value);
                    if (KEY_SENDER.equalsIgnoreCase(loopKey)) {
                        retSender = value;
                    } else if (KEY_ACTION.equalsIgnoreCase(loopKey)) {
                        retAction = value;
                    } else if (KEY_DIGITALIZED_CARD_ID.equalsIgnoreCase(loopKey)) {
                        retDigitalCardID = value;
                    }
                }
            }
        }

        // We will return data back only in case that SDK is fully initialized,
        // or received message is not from TSH. If the message is Tsh, but SDK is not initialized,
        // we will store data for future processing.
        final TshPushSender pushSender = TshPushSender.senderFromString(retSender);
        final boolean isTshConfigured = SdkHelper.getInstance().getInit().getMgSdkState() == MGSDKConfigurationState.CONFIGURED;
        if (isTshConfigured || pushSender == TshPushSender.UNKNOWN) {
            delegate.onMessageProcessed(bundle, pushSender, retAction, retDigitalCardID);
        } else {
            // TODO: We need to init the SDK and process the push notification directly.
            // SDK is not yet loaded. Store unprocessed notification and use it after init.
            AppLoggerHelper.error(TAG, context.getString(R.string.push_received_sdk_not_initialized));
        }
    }

    //endregion

    //region PushServiceListener

    @Override
    public void onError(final ProvisioningServiceError provisioningServiceError) {
        AppLoggerHelper.info(TAG, "onError");

        // Notify application about push processing issue.
        InternalNotificationsUtils.onPushReceived(mContext, TshPushType.UNKNOWN, provisioningServiceError.getErrorMessage());
    }

    @Override
    public void onUnsupportedPushContent(final Bundle bundle) {
        AppLoggerHelper.info(TAG, "onUnsupportedPushContent");

        // Irrelevant for sample application.
    }

    @Override
    public void onServerMessage(final String tokenizedCardId,
                                final ProvisioningServiceMessage msg) {
        AppLoggerHelper.info(TAG, "onServerMessage: " + msg.getMsgCode());

        // add them provisioning message to the list and process them only at the completion time
        mPushServerMessageCodes.push(msg.getMsgCode());
    }

    @Override
    public void onComplete() {
        AppLoggerHelper.info(TAG, "onComplete: " + String.join(",", mPushServerMessageCodes));

        // Notify application about the latest server message processed
        // This is an optimization which avoids for example issuing a replenishment request
        // just after the PROV_CARD command ("msgCode" : "CBP.info.tokenProvisioned") is processed
        TshPushType lastServerMessage = TshPushType.UNKNOWN;
        if(!mPushServerMessageCodes.empty()) {
            lastServerMessage = TshPushType.getTypeFromString(mPushServerMessageCodes.pop());
        }
        InternalNotificationsUtils.onPushReceived(mContext, lastServerMessage, null);

        mPushServerMessageCodes.clear();
        mPushServerMessageCodes = new Stack<>();
    }

    //endregion

}
