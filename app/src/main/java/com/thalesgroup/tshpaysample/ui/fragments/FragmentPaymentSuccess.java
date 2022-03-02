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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.thalesgroup.tshpaysample.R;
import com.thalesgroup.tshpaysample.sdk.payment.TshPaymentData;
import com.thalesgroup.tshpaysample.sdk.helpers.CardWrapper;
import com.thalesgroup.tshpaysample.ui.views.ViewCardFront;

import java.util.Locale;

public class FragmentPaymentSuccess extends AbstractFragment {

    //region Life Cycle

    @Override
    public int getFragmentCaption() {
        return R.string.fragment_payment_caption;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View root = inflater.inflate(R.layout.fragment_payment_success, container, false);
        final TextView amountTextView = root.findViewById(R.id.amount);
        final ViewCardFront cardFrontView = root.findViewById(R.id.fragment_payment_success_card_visual);

        final TshPaymentData data = getPaymentActivity().getSuccessData();
        if (data != null) {
            amountTextView.setText(String.format(Locale.getDefault(), "%s %s", data.getAmount(), data.getCurrency()));
            cardFrontView.loadCardDetails(new CardWrapper(data.getDigitalizedCardId()));
        }
        return root;
    }

    //endregion

}
