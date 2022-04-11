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

package com.thalesgroup.tshpaysample.sdk.enrollment;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;

import androidx.annotation.NonNull;

import com.gemalto.mfs.mwsdk.mobilegateway.MGCardEnrollmentService;
import com.gemalto.mfs.mwsdk.mobilegateway.MobileGatewayError;
import com.gemalto.mfs.mwsdk.mobilegateway.MobileGatewayManager;
import com.gemalto.mfs.mwsdk.mobilegateway.enrollment.IDVMethodSelector;
import com.gemalto.mfs.mwsdk.mobilegateway.enrollment.InputMethod;
import com.gemalto.mfs.mwsdk.mobilegateway.enrollment.IssuerData;
import com.gemalto.mfs.mwsdk.mobilegateway.enrollment.PendingCardActivation;
import com.gemalto.mfs.mwsdk.mobilegateway.enrollment.PendingCardActivationState;
import com.gemalto.mfs.mwsdk.mobilegateway.enrollment.TermsAndConditionSession;
import com.gemalto.mfs.mwsdk.mobilegateway.enrollment.TermsAndConditions;
import com.gemalto.mfs.mwsdk.mobilegateway.listener.CardEligibilityListener;
import com.gemalto.mfs.mwsdk.mobilegateway.listener.MGDigitizationListener;
import com.gemalto.mfs.mwsdk.mobilegateway.utils.MGCardInfoEncryptor;
import com.gemalto.mfs.mwsdk.provisioning.ProvisioningServiceManager;
import com.gemalto.mfs.mwsdk.provisioning.listener.EnrollingServiceListener;
import com.gemalto.mfs.mwsdk.provisioning.model.EnrollmentStatus;
import com.gemalto.mfs.mwsdk.provisioning.model.ProvisioningServiceError;
import com.gemalto.mfs.mwsdk.provisioning.sdkconfig.EnrollingBusinessService;
import com.gemalto.mfs.mwsdk.provisioning.sdkconfig.ProvisioningBusinessService;
import com.gemalto.mfs.mwsdk.utils.chcodeverifier.CHCodeVerifier;
import com.gemalto.mfs.mwsdk.utils.chcodeverifier.SecureCodeInputer;
import com.thalesgroup.tshpaysample.R;
import com.thalesgroup.tshpaysample.sdk.SdkHelper;
import com.thalesgroup.tshpaysample.sdk.init.TshInitBase;
import com.thalesgroup.tshpaysample.utlis.AppLoggerHelper;

import java.util.Arrays;

public class TshEnrollment implements CardEligibilityListener, MGDigitizationListener, EnrollingServiceListener {

    //region Defines

    private static final String TAG = TshEnrollment.class.getName();

    private Context mContext;

    private byte[] mActivationCode;
    private TermsAndConditions mTermsAndConditions;
    private TshEnrollmentDelegate mDelegate;

    private TshEnrollmentState mEnrollmentState = TshEnrollmentState.INACTIVE;
    private String mEnrollmentError = null;


    //endregion

    //region Public API

    public void init(@NonNull final Context context) {
        mContext = context;
    }

    public void enrollCard(@NonNull final String cardPan,
                           @NonNull final String cardExp,
                           @NonNull final String cardCvv,
                           @NonNull final TshEnrollmentDelegate dataProvider) {
        // TODO: Check state and not allow next enrollment before finish.

        // Reset previous state.
        cleanUp();

        mDelegate = dataProvider;

        // Check card eligibility requires WSE to be finished.
        updateState(TshEnrollmentState.WSE_CHECK_START);
        SdkHelper.getInstance().getInit().performWseIfNeeded(new TshInitBase.InitSdkCallback() {
            @Override
            public void onSuccess() {
                updateState(TshEnrollmentState.ELIGIBILITY_CHECK_START);

                final byte[] cardEncryptedData = getCardEncryptedData(cardPan, cardExp, cardCvv);
                final MGCardEnrollmentService enrollmentService = MobileGatewayManager.INSTANCE.getCardEnrollmentService();
                enrollmentService.checkCardEligibility(cardEncryptedData, InputMethod.MANUAL, "en", TshEnrollment.this, getDeviceSerial());
            }

            @Override
            public void onError(final String error) {
                updateState(TshEnrollmentState.WSE_CHECK_ERROR, error);
            }
        });
    }

    public void acceptTermsAndConditions(final boolean accept) {
        // Check whether we are in correct state to prevent any crash situation.
        if (mEnrollmentState != TshEnrollmentState.ELIGIBILITY_TERMS_AND_CONDITIONS) {
            AppLoggerHelper.debug(TAG, "Unexpected state for calling this accept terms and conditions method.");
            return;
        }

        if (accept) {
            // Send notification to UI and wait for response.
            updateState(TshEnrollmentState.DIGITIZATION_START);

            final TermsAndConditionSession session = mTermsAndConditions.accept();
            mTermsAndConditions = null;

            // Get value from t&c and remove reference to it before calling next step -> digitalize.
            final MGCardEnrollmentService enrollmentService = MobileGatewayManager.INSTANCE.getCardEnrollmentService();
            enrollmentService.digitizeCard(session, null, TshEnrollment.this);
        } else {
            cleanUp();
        }
    }

    public void invokePendingActivation(@NonNull final PendingCardActivation pendingCardActivation,
                                        @NonNull final TshEnrollmentDelegate delegate) {
        mDelegate = delegate;

        final PendingCardActivationState state = pendingCardActivation.getState();
        switch (state) {
            case IDV_METHOD_NOT_SELECTED:
                pendingCardActivation.invokeIdvSelection(SdkHelper.getInstance().getTshEnrollment());
                break;
            case OTP_NEEDED:
                mDelegate.onActivationRequired(pendingCardActivation);
                break;
            case WEB_3DS_NEEDED:
            case APP2APP_NEEDED:
                // Not in the scope of sample app.
                break;
            default:
                AppLoggerHelper.error(TAG, "Activation state not handled: " + state);
                break;
        }
    }

    //endregion

    //region CardEligibilityListener

    @Override
    public void onSuccess(final TermsAndConditions termsAndConditions, final IssuerData issuerData) {
        // Ask delegate to accept T&C.
        // We would not be able to pass data object to final fragment correctly because it's
        // not serializable, however this class is designed to have only one enrollment
        // at time so we can store it here.
        mTermsAndConditions = termsAndConditions;
        updateState(TshEnrollmentState.ELIGIBILITY_TERMS_AND_CONDITIONS);
    }

    @Override
    public void onError(final MobileGatewayError mobileGatewayError) {
        // Notify logic layer and update any possible UI.
        updateState(TshEnrollmentState.ELIGIBILITY_CHECK_ERROR, mobileGatewayError.getMessage());
    }

    //endregion

    //region MGDigitizationListener

    @Override
    public void onCPSActivationCodeAcquired(final String identifier, final byte[] code) {
        updateState(TshEnrollmentState.DIGITIZATION_ACTIVATION_CODE_AQUIRED);

        //Pass correct sender_Id to get correct token.
        final String firebaseToken = SdkHelper.getInstance().getPush().getPushTokenLocal(mContext);
        if (firebaseToken == null) {
            throw new IllegalStateException(mContext.getString(R.string.push_token_missing));
        }

        final EnrollingBusinessService enrollingService = ProvisioningServiceManager.getEnrollingBusinessService();
        final ProvisioningBusinessService provisioningBusinessService = ProvisioningServiceManager.getProvisioningBusinessService();

        mActivationCode = new byte[code.length];
        System.arraycopy(code, 0, mActivationCode, 0, code.length);

        //WalletID of MG SDK is userID of CPS SDK Enrollment process
        final String userId = MobileGatewayManager.INSTANCE.getCardEnrollmentService().getWalletId();

        final EnrollmentStatus status = enrollingService.isEnrolled();
        switch (status) {
            case ENROLLMENT_NEEDED:
                updateState(TshEnrollmentState.DIGITIZATION_ACTIVATION_CODE_AQUIRED_ENROLLMENT_NEEDED);

                //First card, first try
                enrollingService.enroll(userId, firebaseToken, "en", this);
                break;
            case ENROLLMENT_IN_PROGRESS:
                //First card, second try
                enrollingService.continueEnrollment("en", this);
                break;
            case ENROLLMENT_COMPLETE:
                //Second card
                provisioningBusinessService.sendActivationCode(this);
                break;
            default:
                AppLoggerHelper.error(TAG, "Unhandled status: " + status);
                break;
        }
    }

    @Override
    public void onSelectIDVMethod(final IDVMethodSelector idvMethodSelector) {
        mDelegate.onSelectIDVMethod(idvMethodSelector);
    }

    @Override
    public void onActivationRequired(final PendingCardActivation pendingCardActivation) {
        mDelegate.onActivationRequired(pendingCardActivation);
    }

    @Override
    public void onComplete(final String message) {
        updateState(TshEnrollmentState.DIGITIZATION_FINISHED);
    }

    @Override
    public void onError(final String message,
                        final MobileGatewayError mobileGatewayError) {
        // Notify logic layer and update any possible UI.
        updateState(TshEnrollmentState.DIGITIZATION_ERROR, mobileGatewayError.getMessage());
    }

    //endregion

    //region EnrollingServiceListener

    @Override
    public void onCodeRequired(final CHCodeVerifier chCodeVerifier) {
        // Notify logic layer and update any possible UI.
        updateState(TshEnrollmentState.ENROLLING_CODE_REQUIRED);

        // Set activation code to SDK.
        final SecureCodeInputer inputer = chCodeVerifier.getSecureCodeInputer();
        for (final byte loopByte : mActivationCode) {
            inputer.input(loopByte);
        }
        inputer.finish();

        // Wipe after use
        Arrays.fill(mActivationCode, (byte) 0);
        mActivationCode = null;
    }

    @Override
    public void onStarted() {
        // Notify logic layer and update any possible UI.
        updateState(TshEnrollmentState.ENROLLING_START);
    }

    @Override
    public void onError(final ProvisioningServiceError provisioningServiceError) {
        // Notify logic layer and update any possible UI.
        updateState(TshEnrollmentState.ENROLLING_ERROR, provisioningServiceError.getErrorMessage());
    }

    @Override
    public void onComplete() {
        updateState(TshEnrollmentState.ENROLLING_FINISHED_WAITING_FOR_SERVER);
    }

    //endregion

    //region Properties

    public TshEnrollmentState getEnrollmentState() {
        return mEnrollmentState;
    }

    public String getEnrollmentError() {
        return mEnrollmentError;
    }

    public final String getTermsAndConditionsText() {
        // Check whether we are in correct state to prevent any crash situation.
        if (mEnrollmentState != TshEnrollmentState.ELIGIBILITY_TERMS_AND_CONDITIONS || mTermsAndConditions == null) {
            AppLoggerHelper.debug(TAG, "Unexpected state for calling getTermsAndConditionsText method.");
            return "";
        }

        return mTermsAndConditions.getText();
    }

    //endregion

    //region Private Helpers

    private void updateState(final TshEnrollmentState state) {
        updateState(state, null);
    }

    private void updateState(final TshEnrollmentState state, final String error) {
        // Same state as last time and it's not error. Nothing to handle.
        if (state.equals(mEnrollmentState) && !mEnrollmentState.isErrorState()) {
            return;
        }

        mEnrollmentState = state;
        if (mDelegate != null) {
            // Notify rest of the application in UI thread.
            new Handler(Looper.getMainLooper()).post(() -> mDelegate.onStateChange(state, error));
        }

        // Notify push handler as well. It's dependent on few steps.
        SdkHelper.getInstance().getPush().onEnrollmentStateChange(state);
    }

    private void cleanUp() {
        mEnrollmentError = null;
        mDelegate = null;
        mTermsAndConditions = null;
        mEnrollmentState = TshEnrollmentState.INACTIVE;
    }

    private String getDeviceSerial() {
        return Settings.Secure.getString(mContext.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    private byte[] getCardEncryptedData(@NonNull final String cardPan,
                                        @NonNull final String cardExp,
                                        @NonNull final String cardCvv) {
        // #################### FOR SAMPLE APP USE ONLY ####################
        // 1] Card data as well as public key and subject identifier are part of values.xml
        //    for testing purposes and simplification. Real application can't expose any of
        //    those values.
        // 2] Green flow must not be calculated on client side. It's only mock behaviour on
        //    sandbox or preproduction environment.
        // #################################################################
        final byte[] pubKeyBytes = MGCardInfoEncryptor.parseHex(mContext.getString(R.string.test_data_public_key));
        final byte[] subKeyBytes = MGCardInfoEncryptor.parseHex(mContext.getString(R.string.test_data_subject_identifier));
        return MGCardInfoEncryptor.encrypt(pubKeyBytes, subKeyBytes,
                cardPan.getBytes(), cardExp.getBytes(), cardCvv.getBytes());
    }

    //endregion
}
