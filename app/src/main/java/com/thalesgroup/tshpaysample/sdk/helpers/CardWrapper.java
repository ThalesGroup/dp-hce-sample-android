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

import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gemalto.mfs.mwsdk.dcm.DigitalizedCard;
import com.gemalto.mfs.mwsdk.dcm.DigitalizedCardManager;
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
import com.gemalto.mfs.mwsdk.payment.PaymentServiceErrorCode;
import com.gemalto.mfs.mwsdk.payment.engine.TransactionContext;
import com.gemalto.mfs.mwsdk.provisioning.ProvisioningServiceManager;
import com.gemalto.mfs.mwsdk.provisioning.listener.PushServiceListener;
import com.gemalto.mfs.mwsdk.provisioning.model.ProvisioningServiceError;
import com.gemalto.mfs.mwsdk.provisioning.model.ProvisioningServiceErrorCodes;
import com.gemalto.mfs.mwsdk.provisioning.sdkconfig.ProvisioningBusinessService;
import com.gemalto.mfs.mwsdk.sdkconfig.AndroidContextResolver;
import com.gemalto.mfs.mwsdk.sdkconfig.SDKError;
import com.thalesgroup.tshpaysample.sdk.SdkHelper;
import com.thalesgroup.tshpaysample.sdk.payment.TshPaymentListener;
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

    //endregion

    //region Life Cycle

    public CardWrapper(final String cardId) {
        mCardId = cardId;
        mDigitalizedCard = DigitalizedCardManager.getDigitalizedCard(cardId);
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

    public void isDefault(@NonNull final CardActionDelegate delegate) {
        mDigitalizedCard.isDefault(PaymentType.CONTACTLESS, new AsyncHandlerBool(new AsyncHandlerBool.Delegate() {
            @Override
            public void onSuccess(final Boolean value) {
                delegate.onFinished(value, null);
            }

            @Override
            public void onError(final String error) {
                delegate.onFinished(false, error);
                AppLoggerHelper.error(TAG, error);
            }
        }));
    }

    public void setDefault(@NonNull final CardActionDelegate delegate) {
        mDigitalizedCard.setDefault(PaymentType.CONTACTLESS, new AsyncHandlerVoid(new AsyncHandlerVoid.Delegate() {
            @Override
            public void onSuccess() {
                delegate.onFinished(true, null);
            }

            @Override
            public void onError(final String error) {
                delegate.onFinished(false, error);
                AppLoggerHelper.error(TAG, error);
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

        isDefault((isDefault, message) -> {

            // If selected card is not the default card, take note of the original default card,
            // then set the selected card as the default, before proceeding with payment.
            if (!isDefault) {

                final String defaultCardTokenID = DigitalizedCardManager
                        .getDefault(PaymentType.CONTACTLESS, null).waitToComplete().getResult();

                final CardWrapper originalDefault = new CardWrapper(defaultCardTokenID);

                // Set the selected card as the new default temporarily.
                setDefault((value, setSelectedDefaultMessage) -> {
                    final TshPaymentListener paymentServiceListener = new TshPaymentListener() {

                        @Override
                        public void onTransactionCompleted(final TransactionContext ctx) {
                            super.onTransactionCompleted(ctx);

                            // After successful transaction, revert to the original default card, if necessary.
                            originalDefault.setDefault((setOriginalDefaultValue, setOriginalDefaultMessage) -> {

                            });
                        }

                        @Override
                        public void onError(final SDKError<PaymentServiceErrorCode> sdkError) {
                            super.onError(sdkError);

                            // After failed transaction, revert to the original default card, if necessary.
                            originalDefault.setDefault((setOriginalDefaultValue, setOriginalDefaultMessage) -> {

                            });
                        }
                    };
                    new Handler(Looper.getMainLooper()).post(() -> {
                        paymentServiceListener.init(context);
                        paymentBS.startAuthentication(paymentServiceListener, PaymentType.CONTACTLESS);
                    });
                });
            } else {
                paymentBS.startAuthentication(SdkHelper.getInstance().getTshPaymentListener(), PaymentType.CONTACTLESS);
            }
        });
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
        getDigitalizedCardState(new AsyncHelperCardState.Delegate() {
            @Override
            public void onSuccess(final DigitalizedCardStatus value) {
                if (value.needsReplenishment()) {
                    final ProvisioningBusinessService businessService = ProvisioningServiceManager.getProvisioningBusinessService();
                    businessService.sendRequestForReplenishment(mDigitalizedCard.getTokenizedCardID(),
                            SdkHelper.getInstance().getPush(), forcedReplenishment);
                }
            }

            @Override
            public void onError(final String error) {
                AppLoggerHelper.error(TAG, error);
            }
        });
    }

    //endregion

    //region Private Helpers

    private static void writeToFile(final Context context,
                                    final String fileName,
                                    final byte[] data) {
        try {
            final FileOutputStream outputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            outputStream.write(data);
            outputStream.close();
        } catch (final IOException exception) {
            // It's not important in same app. In worst case it will download card art again.
            AppLoggerHelper.error(TAG, exception.getMessage());
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
            // It's not important in same app. In worst case it will download card art again.
            AppLoggerHelper.error(TAG, exception.getMessage());
        }

        return new byte[0];
    }

    //endregion
}
