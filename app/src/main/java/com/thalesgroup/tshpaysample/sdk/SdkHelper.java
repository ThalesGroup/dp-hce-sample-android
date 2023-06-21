/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.sdk;

import android.content.Context;

import androidx.annotation.NonNull;

import com.gemalto.mfs.mwsdk.payment.engine.ContactlessPaymentServiceListener;
import com.thalesgroup.tshpaysample.R;
import com.thalesgroup.tshpaysample.sdk.enrollment.TshEnrollment;
import com.thalesgroup.tshpaysample.sdk.init.TshInit;
import com.thalesgroup.tshpaysample.sdk.logger.TshSecureLogger;
import com.thalesgroup.tshpaysample.sdk.payment.TshPaymentListener;
import com.thalesgroup.tshpaysample.sdk.push.TshPush;
import com.thalesgroup.tshpaysample.utlis.AppLoggerHelper;

public final class SdkHelper {

    //region Defines

    private static final String TAG = SdkHelper.class.getSimpleName();

    private static final SdkHelper INSTANCE = new SdkHelper();

    private boolean mInited = false;

    private final TshPush mTshPush = new TshPush();
    private final TshInit mTshInit = new TshInit();
    private final TshEnrollment mTshEnrollment = new TshEnrollment();
    private final TshPaymentListener mTshPaymentListener = new TshPaymentListener();
    private final TshSecureLogger mTshSecureLogger = new TshSecureLogger();

    //endregion


    //region Life Cycle

    public static synchronized SdkHelper getInstance() {
        return INSTANCE;
    }


    public void init(final @NonNull Context context){
        init(context, false);
    }

    public void initFromAppOnCreate(final @NonNull Context context){
        init(context, true);
    }

    /**
     * Initializes the SDK
     * @param context
     * @param fromAppOnCreate    Indicates if the request is coming from App#onCreate method.
     *                           We track that because for TWO_TAP_ALWAYS we want to postpone the
     *                           init to {@link ContactlessPaymentServiceListener#onFirstTapCompleted()}
     *                           as per the developer portal doc:
     *                           https://developer.dbp.thalescloud.io/docs/tsh-hce-android/2b6d6cbf0fc6e-payment-fast-path-pfp
     *
     */
    private void init(final @NonNull Context context, final boolean fromAppOnCreate) {
        // Do not allow multiple initializations.
        if (mInited) {
            AppLoggerHelper.debug(TAG, context.getString(R.string.sdk_helper_already_init));
            return;
        }

        // Start with secure logger so it will be always available for each app part.
        mTshSecureLogger.init(context);

        // Initialize FCM / HMS push notifications.
        mTshPush.init(context);

        // Initialize TSH SDK.
        mTshInit.init(context, fromAppOnCreate);

        // Initialize TSH Enrollment helper.
        mTshEnrollment.init(context);

        // Contactless Payment Service Listener
        mTshPaymentListener.init(context);

        mInited = true;
    }

    //endregion


    //region Public API

    public TshPush getPush() {
        return mTshPush;
    }

    public TshInit getInit() {
        return mTshInit;
    }

    public TshEnrollment getTshEnrollment() {
        return mTshEnrollment;
    }

    public TshPaymentListener getTshPaymentListener() {
        return mTshPaymentListener;
    }

    public TshSecureLogger getTshSecureLogger() {
        return mTshSecureLogger;
    }

    //endregion
}
