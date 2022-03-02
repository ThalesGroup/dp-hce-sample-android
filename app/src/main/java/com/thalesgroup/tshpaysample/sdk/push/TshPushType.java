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
