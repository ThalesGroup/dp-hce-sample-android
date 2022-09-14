/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.sdk.payment;

import android.os.Bundle;

import com.gemalto.mfs.mwsdk.payment.AbstractHCEService;
import com.gemalto.mfs.mwsdk.payment.PaymentServiceListener;
import com.thalesgroup.tshpaysample.sdk.SdkHelper;

public class HceService extends AbstractHCEService {

    //region Defines

    private static final boolean IS_ALLOW_PAYMENT_WHEN_SCREEN_OFF = true;
    private static final String ALLOW_PAYMENT_WHEN_SCREEN_OFF_FLAG = "Payment.allowTransactionScreenOff";

    //endregion

    //region AbstractHCEService

    @Override
    public PaymentServiceListener setupListener() {
        return SdkHelper.getInstance().getTshPaymentListener();
    }

    @Override
    public boolean setupCardActivation() {
        //If POS/Plugin (e.g Fidelity) request to change default card, we need to do it here
        return false;
    }

    @Override
    public void setupPluginRegistration() {
        // If there is plugin to be registered
    }

    @Override
    public byte[] processCommandApdu(final byte[] bytes, final Bundle bundle) {
        final Bundle usedBundle = bundle == null ? new Bundle() : bundle;

        if (IS_ALLOW_PAYMENT_WHEN_SCREEN_OFF) {
            usedBundle.putBoolean(ALLOW_PAYMENT_WHEN_SCREEN_OFF_FLAG, true);
        }

        return super.processCommandApdu(bytes, usedBundle);
    }

    //endregion
}
