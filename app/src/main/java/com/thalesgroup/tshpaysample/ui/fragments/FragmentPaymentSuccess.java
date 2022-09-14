/*
 * Copyright © 2021-2022 THALES. All rights reserved.
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
