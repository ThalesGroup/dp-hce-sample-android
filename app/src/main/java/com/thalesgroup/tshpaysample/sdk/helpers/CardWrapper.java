/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.sdk.helpers;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gemalto.mfs.mwsdk.dcm.DigitalizedCard;
import com.gemalto.mfs.mwsdk.dcm.DigitalizedCardManager;
import com.gemalto.mfs.mwsdk.dcm.DigitalizedCardState;
import com.gemalto.mfs.mwsdk.dcm.DigitalizedCardStatus;
import com.gemalto.mfs.mwsdk.dcm.PaymentType;
import com.gemalto.mfs.mwsdk.mobilegateway.MGCardEnrollmentService;
import com.gemalto.mfs.mwsdk.mobilegateway.MGCardLifeCycleManager;
import com.gemalto.mfs.mwsdk.mobilegateway.MobileGatewayError;
import com.gemalto.mfs.mwsdk.mobilegateway.MobileGatewayManager;
import com.gemalto.mfs.mwsdk.mobilegateway.enrollment.CardArt;
import com.gemalto.mfs.mwsdk.mobilegateway.enrollment.CardArtType;
import com.gemalto.mfs.mwsdk.mobilegateway.enrollment.CardBitmap;
import com.gemalto.mfs.mwsdk.mobilegateway.enrollment.PendingCardActivation;
import com.gemalto.mfs.mwsdk.mobilegateway.exception.NoSuchCardException;
import com.gemalto.mfs.mwsdk.mobilegateway.listener.MGCardLifecycleEventListener;
import com.gemalto.mfs.mwsdk.payment.PaymentBusinessManager;
import com.gemalto.mfs.mwsdk.payment.PaymentBusinessService;
import com.gemalto.mfs.mwsdk.provisioning.ProvisioningServiceManager;
import com.gemalto.mfs.mwsdk.provisioning.listener.PushServiceListener;
import com.gemalto.mfs.mwsdk.provisioning.model.ProvisioningServiceError;
import com.gemalto.mfs.mwsdk.provisioning.model.ProvisioningServiceMessage;
import com.gemalto.mfs.mwsdk.provisioning.sdkconfig.ProvisioningBusinessService;
import com.thalesgroup.tshpaysample.sdk.SdkHelper;
import com.thalesgroup.tshpaysample.sdk.push.TshPush;
import com.thalesgroup.tshpaysample.utlis.AppLoggerHelper;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class CardWrapper {

    //region Defines

    private static final String TAG = CardWrapper.class.getSimpleName();

    public interface CardArtDelegate {
        void onCardArtFinished(@Nullable final Drawable drawable, final boolean loading);
    }

    public interface CardActionDelegate {
        void onFinished(final boolean value, final String message);
    }

    private final String mCardId;
    private final DigitalizedCard mDigitalizedCard;

    private DigitalizedCardStatus mDigitalizedCardStatus;

    //endregion

    //region Life Cycle

    public CardWrapper(final String cardId) {
        mCardId = cardId;
        mDigitalizedCard = DigitalizedCardManager.getDigitalizedCard(cardId);
        mDigitalizedCardStatus = null;
    }

    public CardWrapper(final DigitalizedCard card, final DigitalizedCardStatus cardStatus) {
        mCardId = card.getTokenizedCardID();
        mDigitalizedCard = card;
        mDigitalizedCardStatus = cardStatus;
    }

    //endregion

    //region Public API

    public String getCardId() {
        return mCardId;
    }

    public String getDigitalCardId() {
        return DigitalizedCardManager.getDigitalCardId(mCardId);
    }

    public void getDigitalizedCardDetails(final AsyncHelperCardDetails.Delegate delegate) {
        mDigitalizedCard.getCardDetails(new AsyncHelperCardDetails(delegate));
    }

    public void getDigitalizedCardState(final AsyncHelperCardState.Delegate delegate) {
        mDigitalizedCard.getCardState(new AsyncHelperCardState(delegate));
    }

    public boolean isActive(){
        return mDigitalizedCardStatus != null && mDigitalizedCardStatus.getState() == DigitalizedCardState.ACTIVE;
    }


    // Avoids SDK async operation and checks directly the default cardId being held by TshPaymentListener
    public boolean isDefault() {
        return mCardId.equals(SdkHelper.getInstance().getTshPaymentListener().getDefaultCardId().getValue());
    }

    public void setDefault(@Nullable final CardActionDelegate delegate) {
        mDigitalizedCard.setDefault(PaymentType.CONTACTLESS, new AsyncHandlerVoid(new AsyncHandlerVoid.Delegate() {
            @Override
            public void onSuccess() {
                SdkHelper.getInstance().getTshPaymentListener().onDefaultCardIdChanged(mDigitalizedCard.getTokenizedCardID());
                if (delegate != null) {
                    delegate.onFinished(true, null);
                }
            }

            @Override
            public void onError(final String error) {
                if (delegate != null) {
                    delegate.onFinished(false, error);
                }
                AppLoggerHelper.error(TAG, "setDefault() failed with error: " + error);
            }
        }));
    }

    public PendingCardActivation getPendingActivation() {
        final MGCardEnrollmentService enrollmentService = MobileGatewayManager.INSTANCE.getCardEnrollmentService();
        return enrollmentService.getPendingCardActivation(getDigitalCardId());
    }

    public void invokeManualPayment(final Context context) {

        final PaymentBusinessService paymentBS = PaymentBusinessManager.getPaymentBusinessService();

        // deactivate previous
        paymentBS.deactivate();

            // If selected card is not the default card, take note of the original default card,
            // then set the selected card as the default before proceeding with payment.
            if (!isDefault()) {

                SdkHelper.getInstance().getTshPaymentListener().saveDefaultAsPreferredCard();

                // Set the selected card as the new default temporarily.
                setDefault((result, error) -> {
                    // Do nothing in case of error. Error itself is already logged in the setDefault method
                    if(result) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            paymentBS.startAuthentication(SdkHelper.getInstance().getTshPaymentListener(), PaymentType.CONTACTLESS);
                        });
                    }
                });
            } else {
                paymentBS.startAuthentication(SdkHelper.getInstance().getTshPaymentListener(), PaymentType.CONTACTLESS);
            }

    }

    public void getCardArt(@NonNull final Context context,
                           @NonNull final CardArtDelegate delegate) {
        // Prevent crash in edge cases.
        final String digitalCardId = getDigitalCardId();
        if (digitalCardId == null) {
            delegate.onCardArtFinished(null, false);
            return;
        }

        // First check if we already have some image locally.
        final byte[] imageBytes = readFromFile(context, digitalCardId);
        if (imageBytes.length > 0) {
            // Some image found. Let's try to create drawable out of it.
            final Drawable image = new BitmapDrawable(context.getResources(), BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length));
            delegate.onCardArtFinished(image, false);
        } else {
            // Next request is downloading data from server which might take some time.
            // Give UI some default card meanwhile.
            delegate.onCardArtFinished(null, true);

            // Download actual card art data from backend.
            final MobileGatewayManager gatewayManager = MobileGatewayManager.INSTANCE;
            try {
                final CardArt cardArt = gatewayManager.getCardArt(digitalCardId);
                cardArt.getBitmap(CardArtType.CARD_BACKGROUND_COMBINED, new AsyncHandlerCardBitmap(new AsyncHandlerCardBitmap.Delegate() {
                    @Override
                    public void onSuccess(final CardBitmap value) {
                        // Store current data for future use.
                        writeToFile(context, digitalCardId, value.getResource());

                        // Return image in first thread.
                        final Drawable image = new BitmapDrawable(context.getResources(), BitmapFactory.decodeByteArray(value.getResource(), 0, value.getResource().length));
                        new Handler(Looper.getMainLooper()).post(() -> delegate.onCardArtFinished(image, false));
                    }

                    @Override
                    public void onError(final String error) {
                        AppLoggerHelper.error(TAG, error);
                        new Handler(Looper.getMainLooper()).post(() -> delegate.onCardArtFinished(null, false));
                    }
                }));
            } catch (final NoSuchCardException exception) {
                AppLoggerHelper.error(TAG, exception.getMessage());
                delegate.onCardArtFinished(null, false);
            }
        }
    }

    public void deleteCard(@NonNull final CardActionDelegate delegate) {
        final MGCardLifeCycleManager cardLifeCycleManager = MobileGatewayManager.INSTANCE.getCardLifeCycleManager();
        cardLifeCycleManager.deleteCard(getDigitalCardId(), new MGCardLifecycleEventListener() {
            @Override
            public void onSuccess(final String digitalCardId) {
                delegate.onFinished(true, null);
            }

            @Override
            public void onError(final String digitalCardId,
                                final MobileGatewayError mobileGatewayError) {
                AppLoggerHelper.error(TAG, mobileGatewayError.getMessage());
                delegate.onFinished(false, mobileGatewayError.getMessage());
            }
        });
    }

    public void suspendCard(@NonNull final CardActionDelegate delegate) {
        final MGCardLifeCycleManager cardLifeCycleManager = MobileGatewayManager.INSTANCE.getCardLifeCycleManager();
        cardLifeCycleManager.suspendCard(getDigitalCardId(), new MGCardLifecycleEventListener() {
            @Override
            public void onSuccess(final String digitalCardId) {
                delegate.onFinished(true, null);
            }

            @Override
            public void onError(final String digitalCardId,
                                final MobileGatewayError mobileGatewayError) {
                AppLoggerHelper.error(TAG, mobileGatewayError.getMessage());
                delegate.onFinished(false, mobileGatewayError.getMessage());
            }
        });
    }

    public void resumeCard(@NonNull final CardActionDelegate delegate) {
        final MGCardLifeCycleManager cardLifeCycleManager = MobileGatewayManager.INSTANCE.getCardLifeCycleManager();
        cardLifeCycleManager.resumeCard(getDigitalCardId(), new MGCardLifecycleEventListener() {
            @Override
            public void onSuccess(final String digitalCardId) {
                delegate.onFinished(true, null);
            }

            @Override
            public void onError(final String digitalCardId,
                                final MobileGatewayError mobileGatewayError) {
                AppLoggerHelper.error(TAG, mobileGatewayError.getMessage());
                delegate.onFinished(false, mobileGatewayError.getMessage());
            }
        });
    }

    public void replenishKeysIfNeeded(final boolean forcedReplenishment) {

        // Slight optimization: if we already hold DigitalizedCardStatus
        // we don't have to fetch it and go straight to the replenishment
        if(mDigitalizedCardStatus!=null){
            replenishIfNeeded(forcedReplenishment);
        } else {

            getDigitalizedCardState(new AsyncHelperCardState.Delegate() {
                @Override
                public void onSuccess(final DigitalizedCardStatus value) {
                    mDigitalizedCardStatus = value;
                    replenishIfNeeded(forcedReplenishment);
                }

                @Override
                public void onError(final String error) {
                    AppLoggerHelper.error(TAG, error);
                }
            });

        }

    }

    private void replenishIfNeeded(final boolean forcedReplenishment) {
        if (mDigitalizedCardStatus != null && mDigitalizedCardStatus.needsReplenishment()) {
            final ProvisioningBusinessService businessService = ProvisioningServiceManager.getProvisioningBusinessService();

            businessService.sendRequestForReplenishment(mDigitalizedCard.getTokenizedCardID(), new ReplenishmentListener(mDigitalizedCard, forcedReplenishment), forcedReplenishment);

        }
    }

    //endregion

    //region Private Helpers

    /**
     * By using this listener instead of the generic one in TshPush we will observe only the result
     * of calling the ProvisioningBusinessService#sendRequestForReplenishment() API which uses
     * same PushServiceListener API, but there is no push message processing involved.
     */

    private static class ReplenishmentListener implements PushServiceListener {

        private static final String TAG = ReplenishmentListener.class.getSimpleName();
        private final DigitalizedCard mDigitalizedCard;
        private final boolean mWasForced;

        public ReplenishmentListener(final DigitalizedCard digitalizedCard,
                                     final boolean wasForced){
            mDigitalizedCard = digitalizedCard;
            mWasForced = wasForced;
        }


        @Override
        public void onError(final ProvisioningServiceError provisioningServiceError) {
            AppLoggerHelper.error(TAG, String.format("Failed to send replenishment request for card %s, wasForced=%b, ProvisioningServiceError: %s:%s",
                    mDigitalizedCard.getTokenizedCardID(), mWasForced,
                    provisioningServiceError.getSdkErrorCode(), provisioningServiceError.getErrorMessage())
            );
        }

        @Override
        public void onUnsupportedPushContent(final Bundle bundle) {

            // This should never ever happen in the replenishment use case as we are not passing
            // a push message, but just in case we log it
            AppLoggerHelper.warn(TAG,  "Hit onUnsupportedPushContent() when attempting to replenish card " + mDigitalizedCard.getTokenizedCardID());
        }

        @Override
        public void onServerMessage(final String tokenizedCardId,
                                    final ProvisioningServiceMessage provisioningServiceMessage) {
            // Should not go through here either, but let's log it in case we will
            AppLoggerHelper.debug(TAG, String.format("onServerMessage(%s, %s) when replenishing card %s", tokenizedCardId, provisioningServiceMessage.getMsgCode(), mDigitalizedCard.getTokenizedCardID()));
        }

        @Override
        public void onComplete() {

            // For MC card it only means that we sent out the replenishment request and we need to wait
            // a push message to come once the SUKs are prepared to be fetched from the backend.
            AppLoggerHelper.info(TAG, String.format("Replenishment request for card %s (wasForced=%b) was COMPLETED", mDigitalizedCard.getTokenizedCardID(), mWasForced));

            // For Visa card this means we are done and the card is ready with new LUK
            // Thus we'll check if it is a Visa card and if so we'll reuse the push message handling code to notify the user
            final String digitalCardId = DigitalizedCardManager.getDigitalCardId(mDigitalizedCard.getTokenizedCardID());

            if(digitalCardId != null && digitalCardId.startsWith("HCESDKVTS")){
                AppLoggerHelper.debug(TAG, "Emitting replenishment message for card: " + digitalCardId);
                final TshPush tshPush = SdkHelper.getInstance().getPush();
                tshPush.onVisaCardReplenished(mDigitalizedCard.getTokenizedCardID());
                tshPush.onComplete();
            }
        }
    }

    private static void writeToFile(final Context context,
                                    final String fileName,
                                    final byte[] data) {
        try {
            final FileOutputStream outputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            outputStream.write(data);
            outputStream.close();
        } catch (final IOException exception) {
            // It's not important in same app. In worst case it will download card art again.
            AppLoggerHelper.error(TAG, "writeTofile(): " + exception.getMessage());
        }

    }

    private static byte[] readFromFile(final Context context,
                                       final String fileName) {
        final byte[] buffer = new byte[1024];

        try {
            final FileInputStream inputStream = context.openFileInput(fileName);
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            int bytesRead = inputStream.read(buffer);
            while (bytesRead != -1) {
                outputStream.write(buffer, 0, bytesRead);
                bytesRead = inputStream.read(buffer);
            }
            inputStream.close();

            return outputStream.toByteArray();
        } catch (final IOException exception) {
            // It's not important in sample app. In worst case it will download card art again.
            AppLoggerHelper.warn(TAG, "readFromFile(): " + exception.getMessage());
        }

        return new byte[0];
    }

    //endregion
}
