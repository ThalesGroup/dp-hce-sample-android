/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.sdk.payment;

import java.io.Serializable;

public class TshPaymentData implements Serializable {

    private static final long serialVersionUID = -8846799612852355120L;

    private final double mAmount;
    private final String mCurrency;
    private final String mDigitalizedCardId;

    public TshPaymentData(final double amount,
                          final String currency,
                          final String digitalizedCardId) {
        mAmount = amount;
        mCurrency = currency;
        mDigitalizedCardId = digitalizedCardId;
    }

    public double getAmount() {
        return mAmount;
    }

    public String getCurrency() {
        return mCurrency;
    }

    public String getDigitalizedCardId() {
        return mDigitalizedCardId;
    }

}


