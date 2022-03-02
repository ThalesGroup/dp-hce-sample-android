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
