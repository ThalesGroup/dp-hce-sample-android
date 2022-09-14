/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.sdk.init;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.gemalto.mfs.mwsdk.payment.experience.PaymentExperience;
import com.gemalto.mfs.mwsdk.payment.experience.PaymentExperienceSettings;
import com.thalesgroup.tshpaysample.utlis.AppLoggerHelper;

public class TshInit extends TshInitBase {

    //region Defines
    private static final String TAG = TshInit.class.getSimpleName();
    //endregion

    //region AbstractTshInit

    @Override
    public void init(@NonNull final Context context) {
        super.init(context);
        final PaymentExperience paymentExperience = PaymentExperienceSettings.getPaymentExperience(context);

        if (paymentExperience == PaymentExperience.ONE_TAP_REQUIRES_SDK_INITIALIZED) {
            // To support ONE_TAP_ENABLED we should run benchmark on the device
            runBenchmarkForOneTapAndProceedWithInit();
        } else if (paymentExperience == PaymentExperience.ONE_TAP_ENABLED) {
            executeInit();
        } else {
            // this sample variant does not support fallback resolution, please see variants oneTapForeground or twoTap to implement full experience in your app
            throw new UnsupportedOperationException();
        }

    }


    private void runBenchmarkForOneTapAndProceedWithInit() {

        updateState(TshInitState.INIT_IN_PROGRESS);

        // Benchmark should be done on a separate thread, as it could block Main UI Thread
        new Thread(() -> {

            final boolean isOneTapExperienceSupported = PaymentExperienceSettings.checkPaymentExperienceSupport(mContext, PaymentExperience.ONE_TAP_ENABLED);

            if (isOneTapExperienceSupported) {
                // ONE_TAP_ENABLED is supported by the device
                PaymentExperienceSettings.setPaymentExperience(mContext, PaymentExperience.ONE_TAP_ENABLED);

                // Proceed with SDK init
                executeInit();
            } else {
                // ONE_TAP_ENABLED is not supported by the device, make fallback
                // this sample variant does not support fallback resolution, please see variants oneTapForeground or twoTap to implement full experience in your app
                updateState(TshInitState.INIT_FAILED, "The device does not qualify for support of ONE TAP payment experience!");
            }
        }).start();
    }

    private void executeInit() {
        // CPS and MG might be initialised independently, however in order to keep
        // code simpler and not keep track for multiple states, we would do it in series.
        initCpsSdk(new InitSdkCallback() {
            @Override
            public void onSuccess() {

                registerPreFpEntry();

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

    //endregion
}
