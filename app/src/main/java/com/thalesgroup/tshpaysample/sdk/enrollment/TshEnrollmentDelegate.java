/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.sdk.enrollment;

import com.gemalto.mfs.mwsdk.mobilegateway.enrollment.IDVMethodSelector;
import com.gemalto.mfs.mwsdk.mobilegateway.enrollment.PendingCardActivation;

public interface TshEnrollmentDelegate {
    void onStateChange(final TshEnrollmentState state, final String error);
    void onSelectIDVMethod(final IDVMethodSelector idvMethodSelector);
    void onActivationRequired(final PendingCardActivation pendingCardActivation);
}