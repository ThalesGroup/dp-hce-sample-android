/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.sdk.init;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gemalto.mfs.mwsdk.mobilegateway.MGSDKConfigurationState;
import com.gemalto.mfs.mwsdk.mobilegateway.MobileGatewayManager;
import com.gemalto.mfs.mwsdk.mobilegateway.exception.MGConfigurationException;
import com.gemalto.mfs.mwsdk.payment.cdcvm.DeviceCVMPreEntryReceiver;
import com.gemalto.mfs.mwsdk.payment.engine.ContactlessPaymentServiceListener;
import com.gemalto.mfs.mwsdk.payment.experience.PaymentExperience;
import com.gemalto.mfs.mwsdk.payment.experience.PaymentExperienceSettings;
import com.gemalto.mfs.mwsdk.payment.sdkconfig.SDKDataController;
import com.gemalto.mfs.mwsdk.payment.sdkconfig.SDKInitializer;
import com.gemalto.mfs.mwsdk.provisioning.ProvisioningServiceManager;
import com.gemalto.mfs.mwsdk.provisioning.listener.WalletSecureEnrollmentListener;
import com.gemalto.mfs.mwsdk.provisioning.model.EnrollmentStatus;
import com.gemalto.mfs.mwsdk.provisioning.model.WalletSecureEnrollmentError;
import com.gemalto.mfs.mwsdk.provisioning.model.WalletSecureEnrollmentState;
import com.gemalto.mfs.mwsdk.provisioning.sdkconfig.EnrollingBusinessService;
import com.gemalto.mfs.mwsdk.provisioning.sdkconfig.WalletSecureEnrollmentBusinessService;
import com.gemalto.mfs.mwsdk.sdkconfig.SDKControllerListener;
import com.gemalto.mfs.mwsdk.sdkconfig.SDKError;
import com.gemalto.mfs.mwsdk.sdkconfig.SDKInitializeErrorCode;
import com.gemalto.mfs.mwsdk.sdkconfig.SDKSetupProgressState;
import com.thalesgroup.tshpaysample.BuildConfig;
import com.thalesgroup.tshpaysample.sdk.helpers.InternalNotificationsUtils;
import com.thalesgroup.tshpaysample.utlis.AppLoggerHelper;

public class TshInit {

    //region Defines

    private static final String TAG = TshInit.class.getSimpleName();

    protected static final int INIT_DELAY_MS = 700;
    protected static final int INIT_RETRY_DELAY_MS = 2500;
    protected static final int INIT_ATTEMPTS = 3;

    protected Context mContext;
    protected int mInitAttemptCount = 0;
    protected TshInitState mInitState = TshInitState.INACTIVE;
    protected String mInitError = null;
    protected DeviceCVMPreEntryReceiver mPreEntryReceiver;

    public interface InitSdkCallback {
        void onSuccess();

        void onError(final String error);
    }

    private static final String PAYMENT_EXPERIENCE_ONE_TAP = "ONE_TAP";
    private static final String PAYMENT_EXPERIENCE_TWO_TAP = "TWO_TAP";

    //endregion

    //region Public API

    public MGSDKConfigurationState getMgSdkState() {
        return MobileGatewayManager.INSTANCE.getConfigurationState();
    }

    public void init(@NonNull final Context context){
        init(context, false);
    }

    /**
     * Initializes the SDK
     * @param context
     * @param fromAppOnCreate    Indicates if the request is coming from App#onCreate method.
     *                           We track that because for TWO_TAP_ALWAYS we want to postpone the
     *                           init to {@link ContactlessPaymentServiceListener#onFirstTapCompleted()}
     *                           as per the developer portal doc:
     *                           https://developer.dbp.thalescloud.io/docs/tsh-hce-android/2b6d6cbf0fc6e-payment-fast-path-pfp
     */
    public void init(@NonNull final Context context, final boolean fromAppOnCreate) {
        mContext = context;

        final PaymentExperience paymentExperience = PaymentExperienceSettings.getPaymentExperience(context);

        if (PAYMENT_EXPERIENCE_ONE_TAP.equals(BuildConfig.PAYMENT_EXPERIENCE_VARIANT)) {
            if (paymentExperience == PaymentExperience.ONE_TAP_REQUIRES_SDK_INITIALIZED) {
                // Payment experience has not yet been set
                // For this build variant we will set in that case PaymentExperience.ONE_TAP_ENABLED
                PaymentExperienceSettings.setPaymentExperience(mContext, PaymentExperience.ONE_TAP_ENABLED);
            }
        } else if (PAYMENT_EXPERIENCE_TWO_TAP.equals(BuildConfig.PAYMENT_EXPERIENCE_VARIANT)) {
            if(paymentExperience == PaymentExperience.TWO_TAP_ALWAYS && fromAppOnCreate){
                // Do nothing as init is meant to be called only from
                // ContactlessPaymentServiceListener#onFirstTapCompleted() as per the developer guide:
                // https://developer.dbp.thalescloud.io/docs/tsh-hce-android/2b6d6cbf0fc6e-payment-fast-path-pfp
                AppLoggerHelper.debug(TAG, "Ignoring init request from App#onCreated");
                return;
            }

            if (paymentExperience == PaymentExperience.ONE_TAP_REQUIRES_SDK_INITIALIZED) {
                // Payment experience has not yet been set
                // For this build variant we will set in that case PaymentExperience.TWO_TAP_ALWAYS
                PaymentExperienceSettings.setPaymentExperience(mContext, PaymentExperience.TWO_TAP_ALWAYS);
            }
        } else {
            throw new RuntimeException("Payment experience not supported by the sample application.");
        }

        AppLoggerHelper.debug(TAG, "Proceeding with SDK init");

        executeInit();
    }

    public void performWseIfNeeded(@NonNull final InitSdkCallback callback) {
        // First check current status. Whether we need WSE at all.
        final WalletSecureEnrollmentBusinessService wseService = ProvisioningServiceManager.getWalletSecureEnrollmentBusinessService();
        final WalletSecureEnrollmentState state = wseService.getState();

        switch (state) {
            case WSE_COMPLETED:
            case WSE_NOT_REQUIRED:
                // WSE was already done in current or some previous instance.
                callback.onSuccess();
                break;
            case WSE_STARTED:
                // WSE was triggered during this instance. Simple wait for the first one to finish.
                return;
            case WSE_REQUIRED:
                // Trigger WS enrollment.
                wseService.startWalletSecureEnrollment(new WalletSecureEnrollmentListener() {
                    @Override
                    public void onProgressUpdate(final WalletSecureEnrollmentState wseState) {
                        if (wseState == WalletSecureEnrollmentState.WSE_COMPLETED) {
                            callback.onSuccess();
                        }
                    }

                    @Override
                    public void onError(final WalletSecureEnrollmentError wbDynamicKeyRenewalServiceError) {
                        callback.onError(wbDynamicKeyRenewalServiceError.getErrorMessage());
                    }
                });
                break;
            default:
                AppLoggerHelper.error(TAG, "Unknown WSE state: " + state.toString());
                break;
        }
    }

    //endregion


    //region Properties

    public TshInitState geInitState() {
        return mInitState;
    }

    public final String getInitError() {
        return mInitError;
    }

    //endregion

    //region Protected Helpers

    protected void updateState(@NonNull final TshInitState state) {
        // Wallet secure enrollment is expensive task and so we are calling in only when it's
        // necessary. During SDK init it's required only in case of migration when
        // we already have some tokens enrolled. Otherwise it will be done during initial
        // card enrollment.
        final EnrollingBusinessService enrollingService = ProvisioningServiceManager.getEnrollingBusinessService();
        if (state == TshInitState.INIT_SUCCESSFUL && enrollingService.isEnrolled() == EnrollmentStatus.ENROLLMENT_COMPLETE) {
            performWseIfNeeded(new InitSdkCallback() {
                @Override
                public void onSuccess() {
                    updateState(state, null);
                }

                @Override
                public void onError(final String error) {
                    updateState(TshInitState.INIT_FAILED, error);
                }
            });
        } else {
            updateState(state, null);
        }
    }

    protected void updateState(@NonNull final TshInitState state,
                               @Nullable final String error) {
        mInitState = state;
        mInitError = error;

        InternalNotificationsUtils.updateInitState(mContext, state, error);
    }

    protected void executeInit() {

        // Notify UI, that we started with SDK init.
        updateState(TshInitState.INIT_IN_PROGRESS);

        // CPS and MG might be initialised independently, however in order to keep
        // code simpler and not keep track for multiple states, we would do it in series.
        initCpsSdk(new InitSdkCallback() {
            @Override
            public void onSuccess() {

                // Init of MG component is delayed in case that app cold starts when POS is tapped
                // because it could slow down the payment processing otherwise
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    initMgSdk(new InitSdkCallback() {
                        @Override
                        public void onSuccess() {
                            updateState(TshInitState.INIT_SUCCESSFUL);
                        }

                        @Override
                        public void onError(final String error) {
                            updateState(TshInitState.INIT_FAILED, error);
                        }
                    });
                }, INIT_DELAY_MS);
            }

            @Override
            public void onError(final String error) {
                // Informational only. Actual error is handled by updateInitState.
                AppLoggerHelper.error(TAG, "InitSdkCallback#onError(): " + error);
            }
        });
    }

    protected void initMgSdk(@NonNull final InitSdkCallback callback) {
        final MobileGatewayManager mobileGatewayManager = MobileGatewayManager.INSTANCE;

        try {
            // Avoid multiple init.
            if (mobileGatewayManager.getConfigurationState() != MGSDKConfigurationState.NOT_CONFIGURED) {
                callback.onSuccess();
            } else {
                // Init MG and notify other layers.
                mobileGatewayManager.configure(mContext);
                callback.onSuccess();
            }
        } catch (final MGConfigurationException exception) {
            // Notify handler about the issue.
            callback.onError(exception.getLocalizedMessage());
        }
    }

    protected void initCpsSdk(@NonNull final InitSdkCallback callback) {
        mInitAttemptCount++;
        final SDKControllerListener sdkControllerListener = createSDKControllerListenerObject(callback);
        SDKInitializer.INSTANCE.initialize(mContext, sdkControllerListener);
    }

    protected SDKControllerListener createSDKControllerListenerObject(@NonNull final InitSdkCallback callback) {
        return new SDKControllerListener() {
            @Override
            public void onError(final SDKError<SDKInitializeErrorCode> initializeError) {
                // Internal callback only.
                callback.onError(initializeError.getErrorMessage());

                // Update data layer and notify UI.
                updateState(TshInitState.INIT_FAILED, initializeError.getErrorMessage());

                if (initializeError.getErrorCode() == SDKInitializeErrorCode.SDK_INITIALIZED) {
                    callback.onSuccess();
                } else if (SDKInitializeErrorCode.SDK_INITIALIZING_IN_PROGRESS == initializeError.getErrorCode()) {
                    AppLoggerHelper.info(TAG, "SDK_INITIALIZING_IN_PROGRESS");
                } else if (mInitAttemptCount < INIT_ATTEMPTS && (
                        SDKInitializeErrorCode.INTERNAL_COMPONENT_ERROR == initializeError.getErrorCode() ||
                                SDKInitializeErrorCode.SDK_INIT_FAILED == initializeError.getErrorCode() ||
                                SDKInitializeErrorCode.STORAGE_COMPONENT_ERROR == initializeError.getErrorCode() ||
                                SDKInitializeErrorCode.INVALID_PREVIOUS_VERSION == initializeError.getErrorCode() ||
                                SDKInitializeErrorCode.ASM_INIT_ERROR == initializeError.getErrorCode() ||
                                SDKInitializeErrorCode.ASM_MIGRATION_ERROR == initializeError.getErrorCode())) {

                    try {

                        SDKDataController.INSTANCE.wipeAll(mContext);
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            initCpsSdk(callback);
                        }, INIT_RETRY_DELAY_MS);
                    } catch (final Exception exception) {
                        callback.onError(initializeError.getErrorMessage() + " e::" + exception.getMessage());
                    }


                } else {
                    callback.onError(initializeError.getErrorMessage());

                    // Update data layer and notify UI.
                    updateState(TshInitState.INIT_FAILED, initializeError.getErrorMessage());
                }

            }

            @Override
            public void onSetupProgress(final SDKSetupProgressState sdkSetupProgressState,
                                        final String progressMessage) {
                AppLoggerHelper.info(TAG, sdkSetupProgressState.toString());
            }

            @Override
            public void onSetupComplete() {
                // Internal callback only. Two tap or Service which needs to continue with other stuff.
                callback.onSuccess();
            }
        };
    }

    //endregion

}
