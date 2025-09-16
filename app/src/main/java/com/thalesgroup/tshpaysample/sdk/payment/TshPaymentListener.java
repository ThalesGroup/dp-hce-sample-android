/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.sdk.payment;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

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
import com.gemalto.mfs.mwsdk.utils.async.AsyncResult;
import com.thalesgroup.tshpaysample.sdk.SdkHelper;
import com.thalesgroup.tshpaysample.sdk.helpers.CardWrapper;
import com.thalesgroup.tshpaysample.sdk.helpers.HceHelper;
import com.thalesgroup.tshpaysample.sdk.init.TshInitStateEnum;
import com.thalesgroup.tshpaysample.ui.PaymentActivity;
import com.thalesgroup.tshpaysample.utlis.AppLoggerHelper;
import com.thalesgroup.tshpaysample.utlis.UtilsCurrenciesConstants;

public class TshPaymentListener implements ContactlessPaymentServiceListener {

    //region Defines

    private static final String TAG = TshPaymentListener.class.getSimpleName();
    protected static final int ERROR_DELAY = 300;

    private double mAmount;
    private String mCurrency;

    private TshPaymentState mPaymentState;

    protected Context mContext;
    protected Handler mDelayedError;

    private final MutableLiveData<Integer> mReadyToTapTimeRemaining = new MutableLiveData<>(45);
    private final MutableLiveData<String> mDefaultCardId = new MutableLiveData<>(null);
    private CardWrapper mPreferredCard;

    //endregion

    //region Properties
    public TshPaymentState getPaymentState() {
        return mPaymentState;
    }

    public LiveData<Integer> getReadyToTapTimeRemaining() {
        return mReadyToTapTimeRemaining;
    }

    public LiveData<String> getDefaultCardId(){
        return mDefaultCardId;
    }

    public void saveDefaultAsPreferredCard() {
        mPreferredCard = new CardWrapper(mDefaultCardId.getValue());
    }

    //endregion

    //region Public API

    /**
     * Main entry point which need to be called as soon as the app will run.
     */
    public void init(@NonNull final Context context) {
        mContext = context;
        mDelayedError = new Handler(Looper.getMainLooper());

        // Prepare default values.
        initDefaultCardId();
        resetState();
    }

    private void initDefaultCardId() {
        SdkHelper.getInstance().getInit().getSdkInitState().observeForever(state -> {
           if(state.getState() == TshInitStateEnum.INIT_SUCCESSFUL){
               final AsyncResult<String> stringAsyncResult = DigitalizedCardManager.getDefault(PaymentType.CONTACTLESS, null).waitToComplete();

               if(stringAsyncResult.isSuccessful()){
                   onDefaultCardIdChanged(stringAsyncResult.getResult());
               } else {
                   AppLoggerHelper.error(TAG, "Failed to get default card id. Error: " + stringAsyncResult.getErrorMessage());
               }
           }

        });
    }

    public void onDefaultCardIdChanged(final String newDefaultCardId){
        mDefaultCardId.postValue(newDefaultCardId);
    }

    public void restoreOriginalDefaultCard(){

        if(mPreferredCard != null && mPreferredCard.getCardId() != mDefaultCardId.getValue()){
            mPreferredCard.setDefault((result, error) -> {
                // We will do only logging here as the CardWrapper#setDefault handles the propagation of the change

                if (result) {
                    AppLoggerHelper.info(TAG, "Default card restored to " + mPreferredCard.getCardId());
                } else {
                    AppLoggerHelper.error(TAG, "Failed to restore original default card. Error: " + error);
                }
            });
        }
    }

    //endregion






    //region Protected Helpers

    protected void resetState() {
        mAmount = 0.0;
        mCurrency = null;
        mPaymentState = TshPaymentState.STATE_NONE;
    }


    protected void updateState(final TshPaymentState state,
                               final TshPaymentData data) {
        // Store last state so it can be read onResume when app was not in foreground.
        mPaymentState = state;
        AppLoggerHelper.debug(TAG, "New payment state: " + state.toString());

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
     * Note: This callback gets triggered only for PaymentExperience#TWO_TAP_ALWAYS
     */
    @Override
    public void onFirstTapCompleted() {
        // Call to SDK init is no longer needed here as we initialize from the App#onCreate already
        // SdkHelper.getInstance().getInit().init(mContext);
    }

    /**
     * First callback indicating payment transaction is started.
     * This is called when the first APDU is received via NFC link
     */
    @Override
    public void onTransactionStarted() {
        // All current state values are no longer relevant.
        resetState();

        // Emit the transaction started event only if the app's service is set as default
        // The trouble is that in case of app handling the payment only while being foreground,
        // the screen transition in between app's activities will break up the NFC transaction.
        if(HceHelper.isHceServiceSetAsDefault(mContext)) {
            updateState(TshPaymentState.STATE_ON_TRANSACTION_STARTED, null);
        }
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
        resetState();

        updateAmountAndCurrency(paymentService);

        // Update state and notify everyone.
        updateState(TshPaymentState.STATE_ON_AUTHENTICATION_REQUIRED, new TshPaymentAuthenticationRequestData(chVerificationMethod, mAmount, mCurrency, mDefaultCardId.getValue()));

    }

    /**
     * Last callback for successful sending of payment data to POS.
     */
    @Override
    public void onTransactionCompleted(final TransactionContext transactionContext) {
        // All current state values are no longer relevant.
        resetState();

        updateAmountAndCurrency(transactionContext);

        mDelayedError.removeCallbacks(null);

        // Update state and notify everyone.
        updateState(TshPaymentState.STATE_ON_TRANSACTION_COMPLETED, new TshPaymentData(mAmount, mCurrency, mDefaultCardId.getValue()));
    }

    /**
     * Callback to indicate that CVM had been successfully provided by user.
     */
    @Override
    public void onReadyToTap(final PaymentService paymentService) {
        // All current state values are no longer relevant.
        resetState();

        updateAmountAndCurrency(paymentService);

        paymentService.setCVMResetTimeoutListener(new CVMResetTimeoutListener() {
            @Override
            public void onCredentialsTimeoutCountDown(final int seconds) {
                AppLoggerHelper.debug(TAG, "New payment state countdown: " + seconds);

                mReadyToTapTimeRemaining.postValue(seconds);
            }

            @Override
            public void onCredentialsTimeout(final PaymentService paymentService,
                                             final CHVerificationMethod chVerificationMethod,
                                             final long cvmResetTimeout) {
                updateAmountAndCurrency(paymentService);

                updateState(TshPaymentState.STATE_ON_ERROR, new TshPaymentErrorData("", "Timer exceeded", mAmount, mCurrency, mDefaultCardId.getValue()));
            }
        });

        // Update state and notify everyone.
        updateState(TshPaymentState.STATE_ON_READY_TO_TAP, new TshPaymentData(mAmount, mCurrency, mDefaultCardId.getValue()));
    }

    @Override
    public void onNextTransactionReady(final DeactivationStatus deactivationStatus,
                                       final DigitalizedCardStatus digitalizedCardStatus,
                                       final DigitalizedCard digitalizedCard) {

        AppLoggerHelper.info(TAG, String.format("onNextTransactionReady deactivationStatus: %s", deactivationStatus != null ? deactivationStatus.getSdkStatusCode() : "unknown"));

        if (digitalizedCard != null && digitalizedCardStatus != null) {
            final CardWrapper cardWrapper = new CardWrapper(digitalizedCard, digitalizedCardStatus);
            cardWrapper.replenishKeysIfNeeded(false);
        }

    }

    @Override
    public void onError(final SDKError<PaymentServiceErrorCode> sdkError) {

        // we would like to extend the error message in certain cases
        final String errorMessage[] = new String[1];

        // The callback is designed to be called always with an error, but just to be extra sure we will check it
        if(sdkError != null){
            AppLoggerHelper.error(TAG, String.format("onError: %s:%s", sdkError.getErrorCode().name(), sdkError.getErrorMessage()));

            // showcase specific handling of a particular error code
            if(sdkError.getErrorCode() == PaymentServiceErrorCode.CARD_OUT_OF_PAYMENT_KEYS){
                errorMessage[0] = "You ran out of payment keys.\nWe are trying to fetch new ones.\nMake sure that the device is online.";

                final CardWrapper cardWrapper = new CardWrapper(mDefaultCardId.getValue());
                cardWrapper.replenishKeysIfNeeded(true);
            }
            // the rest of errors will be just sent to the UI
            else {
                errorMessage[0] = sdkError.getErrorMessage();
            }
        }
        // All current state values are no longer relevant.
        resetState();

        // POS disconnection handling has been integrated in the SDK and is controlled via PaymentSettings API
        // See https://developer.dbp.thalescloud.io/docs/tsh-hce-android/2waosjpqmsz03-payment-setting-api
        // So when we get here it means that we either run of retries or we got timeout


        // It's possible that we get multiple error messages. Cancel previously scheduled update.
        mDelayedError.removeCallbacks(null);

        // Postpone the screen transition a little bit to avoid screen flickering,
        // because there are edge cases in which onError might be received before onTransactionCompleted
        mDelayedError.postDelayed(() -> updateState(TshPaymentState.STATE_ON_ERROR,
                new TshPaymentErrorData(sdkError.getErrorCode().name(),
                        errorMessage[0],
                        mAmount,
                        mCurrency,
                        mDefaultCardId.getValue())
        ), ERROR_DELAY);

    }

    @Override
    public void onTransactionInterrupted(final int code, final String message, final int retriesLeft) {
        // How many times this could occur depends on the SDK configuration provided by the app via
        // PaymentSettings.setTransactionRetryLimit() & PaymentSettings.setTransactionRetryTimeout() APIs

        AppLoggerHelper.info(TAG, String.format("onTransactionInterrupted: %d : %s; retriesLeft: %d", code, message, retriesLeft));
        // TODO: Show a hint for the user saying for example "Keep the phone still and close to the terminal"
    }

    //endregion

    //region Private Helpers

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
