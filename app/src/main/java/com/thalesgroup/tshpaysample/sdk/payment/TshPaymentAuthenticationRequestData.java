/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.sdk.payment;

import com.gemalto.mfs.mwsdk.payment.CHVerificationMethod;

public class TshPaymentAuthenticationRequestData extends TshPaymentData {
    private final CHVerificationMethod mMethod;

    public TshPaymentAuthenticationRequestData(final CHVerificationMethod method,
                                               final double amount,
                                               final String currencyCode,
                                               final String cardId) {
        super(amount, currencyCode, cardId);
        mMethod = method;
    }

    public CHVerificationMethod getMethod() {
        return mMethod;
    }
}
