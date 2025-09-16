/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.multidex.MultiDexApplication;

import com.gemalto.mfs.mwsdk.dcm.DigitalizedCard;
import com.gemalto.mfs.mwsdk.dcm.DigitalizedCardDetails;
import com.gemalto.mfs.mwsdk.dcm.DigitalizedCardManager;
import com.gemalto.mfs.mwsdk.provisioning.model.KnownMessageCode;
import com.gemalto.mfs.mwsdk.utils.async.AsyncResult;
import com.thalesgroup.tshpaysample.sdk.SdkHelper;
import com.thalesgroup.tshpaysample.sdk.helpers.CardListHelper;
import com.thalesgroup.tshpaysample.sdk.helpers.CardWrapper;
import com.thalesgroup.tshpaysample.sdk.helpers.InternalNotificationsUtils;
import com.thalesgroup.tshpaysample.sdk.init.TshInitState;
import com.thalesgroup.tshpaysample.sdk.init.TshInitStateEnum;
import com.thalesgroup.tshpaysample.sdk.push.ServerMessageInfo;
import com.thalesgroup.tshpaysample.utlis.AppLoggerHelper;
import com.thalesgroup.tshpaysample.utlis.NotificationHelper;

import java.util.List;

public class PaySampleApp extends MultiDexApplication implements InternalNotificationsUtils.PushMsgResultHandler {

    private static final String TAG = PaySampleApp.class.getSimpleName();
    private ConnectivityManager.NetworkCallback mNetworkCallback;
    private BroadcastReceiver mPushMsgResultReceiver;

    private final Observer<TshInitState> mInitObserver = state -> {
        if(TshInitStateEnum.INIT_SUCCESSFUL == state.getState() ) {
            AppLoggerHelper.info(TAG, "Init completed => registering for network observer.");
            registerNetworkConnectivityAvailableCallback();

            mPushMsgResultReceiver = InternalNotificationsUtils.registerForPushMsgProcessingResult(this, this);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        // Register for SDK init changes
        SdkHelper.getInstance().getInit().getSdkInitState().observeForever(mInitObserver);

        // Start SDK init.
        AppLoggerHelper.info(TAG, "Starting to initialize");
        SdkHelper.getInstance().init(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        // Unregister init changes.
        SdkHelper.getInstance().getInit().getSdkInitState().removeObserver(mInitObserver);

        final ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (mNetworkCallback != null) {
            connectivityManager.unregisterNetworkCallback(mNetworkCallback);
        }

        if(mPushMsgResultReceiver != null){
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mPushMsgResultReceiver);
        }
    }

    private void checkAndReplenishAllCardsIfNeeded() {
        AppLoggerHelper.debug(TAG, "First retrieve list of all cards");
        new CardListHelper(this, new CardListHelper.Delegate() {
            @Override
            public void onSuccess(final List<CardWrapper> cardWrappers) {
                AppLoggerHelper.debug(TAG, "onSuccess: got list of " + cardWrappers.size() + " cards");
                for (final CardWrapper card: cardWrappers) {
                    card.replenishKeysIfNeeded(false);
                }
            }

            @Override
            public void onError(final String error) {
                AppLoggerHelper.error(TAG, "Failed to retrieve all cards: " + error);
            }
        }).getAllCards();
    }

    private void registerNetworkConnectivityAvailableCallback() {
        final ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();

        mNetworkCallback = new ConnectivityManager.NetworkCallback() {

            /**
             * This method will be triggered when
             *  - device gets back online after being offline,
             *  - but also when device already has connectivity immediately after the callback registration
             *
             * We take advantage of this behavior to check and replenish cards:
             *   - once at application startup
             *   - every time the app goes from offline to online state
             *
             * @param network
             */
            @Override
            public void onAvailable(@NonNull final Network network) {
                AppLoggerHelper.info(TAG, "The device is/got back online, let's check and replenish cards");
                checkAndReplenishAllCardsIfNeeded();
            }

            @Override
            public void onLost(@NonNull final Network network) {
                AppLoggerHelper.warn(TAG, "The device just went offline");
            }
        };
        connectivityManager.registerNetworkCallback(networkRequest, mNetworkCallback);
    }

    @Override
    public void onPushProcessed(@Nullable final List<ServerMessageInfo> serverMessageInfoList,
                                @Nullable final String error) {

        // Here we received push message processing result and we can react to it.
        if(error != null) {
            AppLoggerHelper.error(TAG, "Push message processing failed with error: " + error);
        } else if(serverMessageInfoList != null) {

            // We log all messages first
            final StringBuilder strMessages = new StringBuilder();
            for (final ServerMessageInfo serverMessageInfo : serverMessageInfoList) {
                strMessages.append(serverMessageInfo.toString());
                strMessages.append(", ");
            }
            AppLoggerHelper.info(TAG, "Push message processing completed: " + strMessages);

            // Next we demonstrate how to hook to a specific message.
            // In this example we are interested if a default card gets deleted
            // or if any card gets replenished
            for (final ServerMessageInfo serverMessageInfo : serverMessageInfoList) {
                updateDefaultCardIfDeleted(serverMessageInfo);
                notifyCardReplenished(serverMessageInfo);
            }
        }
    }

    private void updateDefaultCardIfDeleted(final ServerMessageInfo serverMessageInfo) {
        if(serverMessageInfo.messageCode().equals(KnownMessageCode.REQUEST_DELETE_CARD)
                && serverMessageInfo.tokenizedCardId().equals(SdkHelper.getInstance().getTshPaymentListener().getDefaultCardId().getValue())) {
            AppLoggerHelper.warn(TAG, "Default card has been deleted => clear the default cardId");
            SdkHelper.getInstance().getTshPaymentListener().onDefaultCardIdChanged(null);
        }
    }

    private void notifyCardReplenished(final ServerMessageInfo serverMessageInfo) {
        if(serverMessageInfo.messageCode().equals(KnownMessageCode.REQUEST_REPLENISH_KEYS)) {
            AppLoggerHelper.debug(TAG, String.format("Card %s has been replenished, will notify user", serverMessageInfo.tokenizedCardId()));
            final DigitalizedCard digitalizedCard = DigitalizedCardManager.getDigitalizedCard(serverMessageInfo.tokenizedCardId());
            final AsyncResult<DigitalizedCardDetails> asyncResult = digitalizedCard.getCardDetails(null).waitToComplete();
            if(asyncResult.isSuccessful() && asyncResult.getResult() != null){
                final String last4 = asyncResult.getResult().getLastFourDigits();
                NotificationHelper.showNotification(getApplicationContext(), "Card replenished", String.format("The card %s has been replenished!", last4));
            } else {
                AppLoggerHelper.warn(TAG, "Failed to fetch card's last 4 digits and thus won't show the notification :(");
            }
        }
    }
}
