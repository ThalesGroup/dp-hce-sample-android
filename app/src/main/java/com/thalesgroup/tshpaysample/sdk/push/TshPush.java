/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.sdk.push;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gemalto.mfs.mwsdk.provisioning.ProvisioningServiceManager;
import com.gemalto.mfs.mwsdk.provisioning.listener.PushServiceListener;
import com.gemalto.mfs.mwsdk.provisioning.model.KnownMessageCode;
import com.gemalto.mfs.mwsdk.provisioning.model.ProvisioningServiceError;
import com.gemalto.mfs.mwsdk.provisioning.model.ProvisioningServiceMessage;
import com.gemalto.mfs.mwsdk.provisioning.sdkconfig.ProvisioningBusinessService;
import com.thalesgroup.tshpaysample.R;
import com.thalesgroup.tshpaysample.sdk.SdkHelper;
import com.thalesgroup.tshpaysample.sdk.helpers.CardWrapper;
import com.thalesgroup.tshpaysample.sdk.helpers.InternalNotificationsUtils;
import com.thalesgroup.tshpaysample.sdk.init.TshInitStateEnum;
import com.thalesgroup.tshpaysample.utlis.AppLoggerHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private List<ServerMessageInfo> mServerMessages;

    //endregion

    //region Public API

    /**
     * Main entry point which need to be called as soon as the app will run.
     */
    public void init(@NonNull final Context context) {
        mContext = context;
        mServerMessages = new ArrayList<>();

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
                // Push token was successfully updated.
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
                default:
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

        // We want to process the message even if the app is not running for LCM, replenishment etc...
        // Find the sender of the message. If it's not Thales we do not need to check SDK state.
        // Delegate will not process such messages.
        final TshPushSender pushSender = TshPushSender.senderFromString(retSender);
        if (SdkHelper.getInstance().getInit().getSdkInitStateValue().getState() == TshInitStateEnum.INIT_SUCCESSFUL) {
            delegate.onMessageProcessed(bundle, pushSender, retAction, retDigitalCardID);
        } else {
            final String finalRetAction = retAction;
            final String finalRetDigitalCardID = retDigitalCardID;
            SdkHelper.getInstance().getInit().getSdkInitState().observeForever(state -> {
                if (state.getState() == TshInitStateEnum.INIT_SUCCESSFUL) {
                    delegate.onMessageProcessed(bundle, pushSender, finalRetAction, finalRetDigitalCardID);
                }
            });
        }
    }

    //endregion

    //region PushServiceListener

    @Override
    public void onError(final ProvisioningServiceError provisioningServiceError) {
        AppLoggerHelper.info(TAG, "onError(): " + provisioningServiceError.getSdkErrorCode() + ":" + provisioningServiceError.getErrorMessage());

        InternalNotificationsUtils.onPushMsgProcessingFailed(mContext, provisioningServiceError.getErrorMessage());
    }

    @Override
    public void onUnsupportedPushContent(final Bundle bundle) {
        AppLoggerHelper.warn(TAG, "onUnsupportedPushContent(): " + bundle);

        // Irrelevant for sample application.
    }

    @Override
    public void onServerMessage(final String tokenizedCardId,
                                final ProvisioningServiceMessage msg) {
        final ServerMessageInfo serverMessageInfo = new ServerMessageInfo(tokenizedCardId, msg.getMsgCode());

        AppLoggerHelper.info(TAG, "onServerMessage(): " + serverMessageInfo);

        mServerMessages.add(serverMessageInfo);
    }

    public void onVisaCardReplenished(final String tokenizedCardId){
        final ServerMessageInfo serverMessageInfo = new ServerMessageInfo(tokenizedCardId, KnownMessageCode.REQUEST_REPLENISH_KEYS);
        mServerMessages.add(serverMessageInfo);
    }

    @Override
    public void onComplete() {
        AppLoggerHelper.info(TAG, "onComplete(): processed " + mServerMessages.size() + " messages");

        InternalNotificationsUtils.onPushProcessingCompleted(mContext, new ArrayList<>(mServerMessages));

        mServerMessages.clear();
        mServerMessages = new ArrayList<>();
    }

    //endregion

}
