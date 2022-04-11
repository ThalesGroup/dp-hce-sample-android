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

package com.thalesgroup.tshpaysample.sdk.init;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.cardemulation.CardEmulation;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.gemalto.mfs.mwsdk.mobilegateway.MGConnectionConfiguration;
import com.gemalto.mfs.mwsdk.mobilegateway.MGSDKConfigurationState;
import com.gemalto.mfs.mwsdk.mobilegateway.MGTransactionHistoryConfiguration;
import com.gemalto.mfs.mwsdk.mobilegateway.MGWalletConfiguration;
import com.gemalto.mfs.mwsdk.mobilegateway.MobileGatewayManager;
import com.gemalto.mfs.mwsdk.mobilegateway.exception.MGConfigurationException;
import com.gemalto.mfs.mwsdk.payment.cdcvm.DeviceCVMPreEntryReceiver;
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
import com.thalesgroup.tshpaysample.sdk.helpers.InternalNotificationsUtils;
import com.thalesgroup.tshpaysample.sdk.payment.HceService;
import com.thalesgroup.tshpaysample.utlis.AppLoggerHelper;
import com.thalesgroup.tshpaysample.utlis.NotificationUtil;

public class TshInitBase {

    //region Defines

    private static final String TAG = TshInitBase.class.getSimpleName();

    protected static final int INIT_DELAY_MS = 700;
    protected static final int INIT_RETRAY_DELAY_MS = 2500;
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

    //endregion

    //region Public API

    public MGSDKConfigurationState getMgSdkState() {
        return MobileGatewayManager.INSTANCE.getConfigurationState();
    }

    public void init(@NonNull final Context context) {
        mContext = context;
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

    protected void registerPreFpEntry() {
        AppLoggerHelper.debug(TAG, "registerPreFpEntry");
        if (mPreEntryReceiver != null) {
            mContext.unregisterReceiver(mPreEntryReceiver);
            mPreEntryReceiver = null;
        }
        final IntentFilter filter = new IntentFilter(Intent.ACTION_USER_PRESENT);
        mPreEntryReceiver = new DeviceCVMPreEntryReceiver();
        mPreEntryReceiver.init();
        mContext.registerReceiver(mPreEntryReceiver, filter);
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
        SDKInitializer.INSTANCE.initialize(mContext, sdkControllerListener,
                NotificationUtil.getNotification(mContext,
                        mContext.getString(R.string.sdk_cps_notification_message),
                        mContext.getString(R.string.sdk_cps_notification_channel_id)));
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
                        }, INIT_RETRAY_DELAY_MS);
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

    public void checkTapAndPay() {
        final Intent activate = new Intent();
        activate.setAction(CardEmulation.ACTION_CHANGE_DEFAULT);
        activate.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activate.putExtra(CardEmulation.EXTRA_SERVICE_COMPONENT, new ComponentName(mContext, HceService.class.getCanonicalName()));
        activate.putExtra(CardEmulation.EXTRA_CATEGORY, CardEmulation.CATEGORY_PAYMENT);

        // It is also possible to use startActivityForResult and react e.g. with a confirmation dialog when user chose "no"
        mContext.startActivity(activate);
    }

    //endregion

}
