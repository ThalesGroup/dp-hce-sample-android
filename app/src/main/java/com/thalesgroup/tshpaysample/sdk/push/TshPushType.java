/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.sdk.push;

import com.gemalto.mfs.mwsdk.provisioning.model.KnownMessageCode;

public enum TshPushType {
    UNKNOWN(null),
    REQUEST_DELETE_CARD(KnownMessageCode.REQUEST_DELETE_CARD),
    REQUEST_SUSPEND_CARD(KnownMessageCode.REQUEST_SUSPEND_CARD),
    REQUEST_RESUME_CARD(KnownMessageCode.REQUEST_RESUME_CARD),
    REQUEST_REPLENISH_KEYS(KnownMessageCode.REQUEST_REPLENISH_KEYS),
    REQUEST_INSTALL_CARD(KnownMessageCode.REQUEST_INSTALL_CARD),
    REQUEST_RENEW_CARD(KnownMessageCode.REQUEST_RENEW_CARD),
    MSG_PAYMENT_INVALID_CRYPTOGRAM(KnownMessageCode.MSG_PAYMENT_INVALID_CRYPTOGRAM),
    MSG_PAYMENT_TRANSACTION_APPROVED(KnownMessageCode.MSG_PAYMENT_TRANSACTION_APPROVED),
    MSG_PAYMENT_WRONG_PIN(KnownMessageCode.MSG_PAYMENT_WRONG_PIN),
    MSG_PAYMENT_PIN_LOCKED(KnownMessageCode.MSG_PAYMENT_PIN_LOCKED),
    MSG_PIN_CHANGE_SUCCESSFUL(KnownMessageCode.MSG_PIN_CHANGE_SUCCESSFUL);

    TshPushType(final String code) {
        mCode = code;
    }

    private final String mCode;

    public static TshPushType getTypeFromString(final String value) {
        if (value == null || value.isEmpty()) {
            return UNKNOWN;
        }

        for (final TshPushType loopType : TshPushType.values()) {
            if (value.equalsIgnoreCase(loopType.mCode)) {
                return loopType;
            }
        }

        return UNKNOWN;
    }
}
