/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.ui.fragments;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.thalesgroup.tshpaysample.sdk.init.TshInitState;
import com.thalesgroup.tshpaysample.sdk.payment.TshPaymentState;
import com.thalesgroup.tshpaysample.sdk.push.TshPushType;
import com.thalesgroup.tshpaysample.ui.CardListActivity;
import com.thalesgroup.tshpaysample.ui.PaymentActivity;

public abstract class AbstractFragment extends Fragment {

    @StringRes
    public abstract int getFragmentCaption();

    public void onInitStateChanged(@NonNull final TshInitState state,
                                   @Nullable final String error) {
        // Optional for other fragments.
    }

    public void onPushReceived(@NonNull final TshPushType state,
                               @Nullable final String error) {
        // Optional for other fragments.
    }

    public void onPaymentStatusChanged(@NonNull final TshPaymentState state) {
        // Optional for other fragments.
    }

    public void onPaymentCountdownChanged(@NonNull final int remainingSeconds) {
        // Optional for other fragments.
    }

    public void onReloadData() {
        // Optional for other fragments.
    }

    public CardListActivity getMainActivity() {
        return (CardListActivity) getActivity();
    }

    public PaymentActivity getPaymentActivity() {
        return (PaymentActivity) getActivity();
    }

    public boolean isBackButtonAllowed() {
        return true;
    }
}
