/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.sdk.payment;

public enum TshPaymentState {
    STATE_NONE,
    STATE_ON_TRANSACTION_STARTED,
    STATE_ON_AUTHENTICATION_REQUIRED,
    STATE_ON_READY_TO_TAP,
    STATE_ON_TRANSACTION_COMPLETED,
    STATE_ON_ERROR
}
