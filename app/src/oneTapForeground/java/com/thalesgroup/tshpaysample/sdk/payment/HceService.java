/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.sdk.payment;

import com.gemalto.mfs.mwsdk.payment.AbstractHCEService;
import com.gemalto.mfs.mwsdk.payment.PaymentServiceListener;
import com.thalesgroup.tshpaysample.sdk.SdkHelper;

public class HceService extends AbstractHCEService {

    @Override
    public PaymentServiceListener setupListener() {
        return SdkHelper.getInstance().getTshPaymentListener();
    }

    @Override
    public boolean setupCardActivation() {
        //If POS/Plugin (e.g Fidelity) request to change default card, we need to do it here
        //Otherwise, just return false.
        return false;
    }

    @Override
    public void setupPluginRegistration() {
        //If there is plugin to be registered
    }
}
