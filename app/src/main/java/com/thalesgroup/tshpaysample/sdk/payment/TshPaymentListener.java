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

package com.thalesgroup.tshpaysample.sdk.payment;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.gemalto.mfs.mwsdk.dcm.DigitalizedCard;
import com.gemalto.mfs.mwsdk.dcm.DigitalizedCardManager;
import com.gemalto.mfs.mwsdk.dcm.DigitalizedCardStatus;
import com.gemalto.mfs.mwsdk.dcm.PaymentType;
import com.gemalto.mfs.mwsdk.payment.CHVerificationMethod;
import com.gemalto.mfs.mwsdk.payment.CVMResetTimeoutListener;
import com.gemalto.mfs.mwsdk.payment.PaymentServiceErrorCode;
import com.gemalto.mfs.mwsdk.payment.engine.ContactlessPaymentServiceListener;
import com.gemalto.mfs.mwsdk.payment.engine.DeactivationStatus;
import com.gemalto.mfs.mwsdk.payment.engine.PaymentService;
import com.gemalto.mfs.mwsdk.payment.engine.TransactionContext;
import com.gemalto.mfs.mwsdk.sdkconfig.SDKError;
import com.thalesgroup.tshpaysample.sdk.SdkHelper;
import com.thalesgroup.tshpaysample.sdk.helpers.CardWrapper;
import com.thalesgroup.tshpaysample.sdk.helpers.InternalNotificationsUtils;
import com.thalesgroup.tshpaysample.ui.PaymentActivity;
import com.thalesgroup.tshpaysample.utlis.AppLoggerHelper;
import com.thalesgroup.tshpaysample.utlis.UtilsCurrenciesConstants;

public class TshPaymentListener implements ContactlessPaymentServiceListener {

    //region Defines

    private static final String TAG = TshPaymentListener.class.getSimpleName();
    protected static final int ERROR_THRESHOLD = 3;
    protected static final int ERROR_DELAY = 2000;

    private double mAmount;
    private String mCurrency;
    private String mCurrentCardId;

    private TshPaymentState mPaymentState;

    protected Context mContext;
    protected int mPosCommDisconnectedErrCount;
    protected Handler mDelayedError;


    //endregion

    //region Public API

    /**
     * Main entry point which need to be called as soon as the app will run.
     */
    public void init(@NonNull final Context context) {
        mContext = context;
        mDelayedError = new Handler(Looper.getMainLooper());

        // Prepare default values.
        resetState(true);
    }

    public TshPaymentState getPaymentState() {
        return mPaymentState;
    }

    //endregion

    //region Protected Helpers

    protected void resetState(final boolean includingErrCount) {
        if (includingErrCount) {
            mPosCommDisconnectedErrCount = 0;
        }

        mAmount = 0.0;
        mCurrency = null;
        mPaymentState = TshPaymentState.STATE_NONE;
    }


    protected void updateState(final TshPaymentState state,
                               final TshPaymentData data) {
        // Store last state so it can be read onResume when app was not in foreground.
        mPaymentState = state;
        Log.d(TAG, "New payment state: " + state.toString());

        // Notify rest of the application in UI thread.
        new Handler(Looper.getMainLooper()).post(() -> {
            final Intent intent = new Intent(mContext, PaymentActivity.class);
            intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra(PaymentActivity.STATE_EXTRA_KEY, state);
            intent.putExtra(PaymentActivity.PAYMENT_DATA_EXTRA_KEY, data);
            mContext.startActivity(intent);
        });
    }

    //endregion

    //region ContactlessPaymentServiceListener

    /**
     * Callback indicating first tap transaction is completed.
     */
    @Override
    public void onFirstTapCompleted() {
        SdkHelper.getInstance().getInit().init(mContext);
    }

    /**
     * First callback indicating payment transaction is started.
     * This is called when the first APDU is received via NFC link
     */
    @Override
    public void onTransactionStarted() {
        // All current state values are no longer relevant.
        resetState(true);

        loadCurrentCardData();

        // Update state and notify everyone.
        updateState(TshPaymentState.STATE_ON_TRANSACTION_STARTED, null);

    }

    /**
     * Callback to indicate that payment requires CVM method.
     * Use chVerificationMethod to determine what is the cvm type and display appropriate UI.
     */
    @Override
    public void onAuthenticationRequired(final PaymentService paymentService,
                                         final CHVerificationMethod chVerificationMethod,
                                         final long cvmResetTimeout) {
        // All current state values are no longer relevant.
        resetState(true);

        updateAmountAndCurrency(paymentService);

        loadCurrentCardData();

        // Update state and notify everyone.
        updateState(TshPaymentState.STATE_ON_AUTHENTICATION_REQUIRED, new TshPaymentAuthenticationRequestData(chVerificationMethod, mAmount, mCurrency, mCurrentCardId));

    }

    /**
     * Last callback for successful sending of payment data to POS.
     */
    @Override
    public void onTransactionCompleted(final TransactionContext transactionContext) {
        // All current state values are no longer relevant.
        resetState(true);

        updateAmountAndCurrency(transactionContext);

        mDelayedError.removeCallbacks(null);

        // Update state and notify everyone.
        updateState(TshPaymentState.STATE_ON_TRANSACTION_COMPLETED, new TshPaymentData(mAmount, mCurrency, mCurrentCardId));
    }

    /**
     * Callback to indicate that CVM had been successfully provided by user.
     */
    @Override
    public void onReadyToTap(final PaymentService paymentService) {
        // All current state values are no longer relevant.
        resetState(true);

        updateAmountAndCurrency(paymentService);

        paymentService.setCVMResetTimeoutListener(new CVMResetTimeoutListener() {
            @Override
            public void onCredentialsTimeoutCountDown(final int seconds) {
                Log.d(TAG, "New payment state countdown: " + seconds);

                // Notify rest of the application in UI thread.
                new Handler(Looper.getMainLooper()).post(() -> {
                    InternalNotificationsUtils.updatePaymentCountdown(mContext, seconds);
                });
            }

            @Override
            public void onCredentialsTimeout(final PaymentService paymentService,
                                             final CHVerificationMethod chVerificationMethod,
                                             final long cvmResetTimeout) {
                updateAmountAndCurrency(paymentService);

                updateState(TshPaymentState.STATE_ON_ERROR, new TshPaymentErrorData("", "Timer exceeded", mAmount, mCurrency, mCurrentCardId));
            }
        });

        // Update state and notify everyone.
        updateState(TshPaymentState.STATE_ON_READY_TO_TAP, new TshPaymentData(mAmount, mCurrency, mCurrentCardId));
    }

    @Override
    public void onNextTransactionReady(final DeactivationStatus deactivationStatus,
                                       final DigitalizedCardStatus digitalizedCardStatus,
                                       final DigitalizedCard digitalizedCard) {
        if (digitalizedCard != null && digitalizedCard.getTokenizedCardID() != null) {
            final CardWrapper cardWrapper = new CardWrapper(digitalizedCard.getTokenizedCardID());
            cardWrapper.replenishKeysIfNeeded(false);
        }
    }

    @Override
    public void onError(final SDKError<PaymentServiceErrorCode> sdkError) {
        AppLoggerHelper.info(TAG, "onError");

        // All current state values are no longer relevant.
        resetState(false);

        // Handle POS disconnection since we want some threshold for better user experience.
        // In this case we will wait {ERROR_DELAY}ms, {ERROR_THRESHOLD} times before propagating the
        // error to the rest of the application.
        boolean postDelay = false;
        mPosCommDisconnectedErrCount++;
        if (sdkError != null && sdkError.getErrorCode() == PaymentServiceErrorCode.POS_COMM_DISCONNECTED) {
            if (mPosCommDisconnectedErrCount < ERROR_THRESHOLD) {
                postDelay = true;
            }
        } else {
            mPosCommDisconnectedErrCount = 0;
        }

        // It's possible that we get multiple error messages. Cancel previously scheduled update.
        mDelayedError.removeCallbacks(null);

        // Only POS_COMM_DISCONNECTED withing the threshold is delayed since it can be recovered,
        // by user action. Everything else is propagated directly.
        if (postDelay) {
            mDelayedError.postDelayed(() -> {
                if (mPaymentState == TshPaymentState.STATE_ON_ERROR) {
                    // check if state remains error after delay
                    updateState(TshPaymentState.STATE_ON_ERROR, new TshPaymentErrorData(sdkError.getErrorCode().name(), sdkError.getErrorMessage(), mAmount, mCurrency, mCurrentCardId));
                }
            }, ERROR_DELAY);
        } else {
            updateState(TshPaymentState.STATE_ON_ERROR, new TshPaymentErrorData(sdkError.getErrorCode().name(), sdkError.getErrorMessage(), mAmount, mCurrency, mCurrentCardId));
        }
    }

    //endregion

    //region Private Helpers

    private void loadCurrentCardData() {
        mCurrentCardId = DigitalizedCardManager.getDefault(PaymentType.CONTACTLESS, null).waitToComplete().getResult();
    }

    private TransactionContext retrieveTransactionContext(final PaymentService paymentService) {

        if (paymentService == null) {
            return null;
        } else {
            return paymentService.getTransactionContext();
        }
    }

    private void updateAmountAndCurrency(final PaymentService paymentService) {
        updateAmountAndCurrency(retrieveTransactionContext(paymentService));
    }

    private void updateAmountAndCurrency(final TransactionContext transactionContext) {
        if (transactionContext == null) {
            mAmount = -1.0;
            mCurrency = null;
        } else {
            mAmount = transactionContext.getAmount();
            mCurrency = UtilsCurrenciesConstants.getCurrency(transactionContext.getCurrencyCode()).getCurrencyCode();
        }
    }

    //endregion

}
