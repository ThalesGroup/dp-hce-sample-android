/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.sdk.payment;

import com.gemalto.mfs.mwsdk.payment.AsyncHCEService;
import com.gemalto.mfs.mwsdk.payment.PaymentServiceListener;
import com.gemalto.mfs.mwsdk.payment.PaymentSettings;
import com.thalesgroup.tshpaysample.sdk.SdkHelper;

public class TshPaymentHceService extends AsyncHCEService {

    @Override
    public void onCreate() {
        super.onCreate();

        // Configure SDK's payment settings when the service is created
        // see: https://developer.dbp.thalescloud.io/docs/tsh-hce-android/2waosjpqmsz03-payment-setting-api

        // Configure SDK to allow up to 3 transaction retries when NFC connection is lost
        PaymentSettings.setTransactionRetryLimit(3);

        // Set the timeout to signal transaction error after 3 seconds to allow POS performing retries
        PaymentSettings.setTransactionRetryTimeout(3000);

        // Suspend APDU processing for up to 5s when pending for user authentication
        PaymentSettings.setApduSuspendTimeout(5000);
    }

    @Override
    public PaymentServiceListener setupListener() {
        return SdkHelper.getInstance().getTshPaymentListener();
    }

    @Override
    public boolean setupCardActivation() {
        return false;
    }

    @Override
    public void setupPluginRegistration() {
        // Not needed in scope of the sample app.
    }

}
