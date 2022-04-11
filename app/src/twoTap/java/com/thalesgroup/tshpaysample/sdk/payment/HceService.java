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
