/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.sdk.payment;

public class TshPaymentErrorData extends TshPaymentData {
    final String mCode;
    final String mMessage;

    public TshPaymentErrorData(final String code,
                               final String message,
                               final double amount,
                               final String currency,
                               final String cardId) {
        super(amount, currency, cardId);

        mCode = code;
        mMessage = message;
    }

    public String getCode() {
        return mCode;
    }

    public String getMessage() {
        return mMessage;
    }
}
