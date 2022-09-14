/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.sdk.push;

public enum TshPushSender {
    UNKNOWN, CPS, MG, TNS;

    static TshPushSender senderFromString(final String value) {
        for (TshPushSender loopSender : TshPushSender.values()) {
            if (loopSender.name().equalsIgnoreCase(value)) {
                return loopSender;
            }
        }
        return UNKNOWN;
    }
}