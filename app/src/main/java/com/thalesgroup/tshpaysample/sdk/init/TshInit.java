/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.sdk.init;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.gemalto.mfs.mwsdk.mobilegateway.MGSDKConfigurationState;
import com.gemalto.mfs.mwsdk.mobilegateway.MobileGatewayManager;
import com.gemalto.mfs.mwsdk.mobilegateway.exception.MGConfigurationException;
import com.gemalto.mfs.mwsdk.payment.CustomConfiguration;
import com.gemalto.mfs.mwsdk.payment.cdcvm.DeviceCVMPreEntryReceiver;
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
import com.thalesgroup.tshpaysample.R;
import com.thalesgroup.tshpaysample.utlis.AppLoggerHelper;

public class TshInit {

    //region Defines

    private static final String TAG = TshInit.class.getSimpleName();

    protected static final int INIT_DELAY_MS = 700;
    protected static final int INIT_RETRY_DELAY_MS = 2500;
    protected static final int INIT_ATTEMPTS = 3;

    protected Context mContext;
    protected int mInitAttemptCount = 0;
    protected MutableLiveData<TshInitState> mInitState = new MutableLiveData<>(new TshInitState(TshInitStateEnum.INACTIVE));

    public interface InitSdkCallback {
        void onSuccess();

        void onError(final String error);
    }

    //endregion

    //region Public API

    public void init(@NonNull final Context context) {
        AppLoggerHelper.debug(TAG, "Proceeding with SDK init");

        // Store current context.
        mContext = context;

        // Select desired payment experience before SDK init.
        PaymentExperienceSettings.setPaymentExperience(mContext, PaymentExperience.ONE_TAP_ENABLED);

        // Notify UI, that we started with SDK init.
        updateState(TshInitStateEnum.INIT_IN_PROGRESS);

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
                            updateState(TshInitStateEnum.INIT_SUCCESSFUL);
                        }

                        @Override
                        public void onError(final String error) {
                            updateState(TshInitStateEnum.INIT_FAILED, error);
                        }
                    });
                }, INIT_DELAY_MS);

                registerDeviceCVMPreEntryReceiver();
            }

            @Override
            public void onError(final String error) {
                // Informational only. Actual error is handled by updateInitState.
                AppLoggerHelper.error(TAG, "InitSdkCallback#onError(): " + error);
            }
        });
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

    public LiveData<TshInitState> getSdkInitState() {
        return mInitState;
    }

    public TshInitState getSdkInitStateValue() {
        if (mInitState.getValue() != null) {
            return mInitState.getValue();
        } else {
            throw new IllegalStateException(mContext.getString(R.string.sdk_incorrect_state));
        }
    }

    //endregion

    //region Protected Helpers

    protected void updateState(@NonNull final TshInitStateEnum state) {
        // Wallet secure enrollment is expensive task and so we are calling in only when it's
        // necessary. During SDK init it's required only in case of migration when
        // we already have some tokens enrolled. Otherwise it will be done during initial
        // card enrollment.
        final EnrollingBusinessService enrollingService = ProvisioningServiceManager.getEnrollingBusinessService();
        if (state == TshInitStateEnum.INIT_SUCCESSFUL && enrollingService.isEnrolled() == EnrollmentStatus.ENROLLMENT_COMPLETE) {
            performWseIfNeeded(new InitSdkCallback() {
                @Override
                public void onSuccess() {
                    updateState(state, null);
                }

                @Override
                public void onError(final String error) {
                    updateState(TshInitStateEnum.INIT_FAILED, error);
                }
            });
        } else {
            updateState(state, null);
        }
    }

    protected void updateState(@NonNull final TshInitStateEnum state,
                               @Nullable final String error) {
        mInitState.postValue(new TshInitState(state, error));
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

        // Configure Flexible CDCVM risk management parameters
        // Ref: https://developer.dbp.thalescloud.io/docs/tsh-hce-android/fe50c969aaba4-fcdcvm-risk-management
        // Note: the following settings (except keyValidityPeriod) are only applicable fo payment experience
        // set to ONE_TAP_ENABLED
        final CustomConfiguration customConfiguration = new CustomConfiguration.Builder()
                .domesticCurrencyCode(978) // EUR
                .singleTransactionAmountLimitForLVT(2000) // 20 EUR
                .maxCumulativeAmountForLVT(10000) // 100 EUR
                .maxConsecutivePaymentsForLVT(5)
                .keyValidityPeriod(45)
                .supportTransitWithoutCDCVM(true)
                .build();

        SDKInitializer.INSTANCE.initialize(mContext, customConfiguration, sdkControllerListener);
    }

    protected SDKControllerListener createSDKControllerListenerObject(@NonNull final InitSdkCallback callback) {
        return new SDKControllerListener() {
            @Override
            public void onError(final SDKError<SDKInitializeErrorCode> initializeError) {
                // Internal callback only.
                callback.onError(initializeError.getErrorMessage());

                // Update data layer and notify UI.
                updateState(TshInitStateEnum.INIT_FAILED, initializeError.getErrorMessage());

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
                    updateState(TshInitStateEnum.INIT_FAILED, initializeError.getErrorMessage());
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

    protected void registerDeviceCVMPreEntryReceiver(){
        final DeviceCVMPreEntryReceiver receiver = new DeviceCVMPreEntryReceiver();
        receiver.init();

        final IntentFilter filter = new IntentFilter(Intent.ACTION_USER_PRESENT);
        mContext.registerReceiver(receiver, filter);
    }

    //endregion

}
