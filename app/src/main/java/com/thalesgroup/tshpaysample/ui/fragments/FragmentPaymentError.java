/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.ui.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.thalesgroup.tshpaysample.R;
import com.thalesgroup.tshpaysample.sdk.helpers.CardWrapper;
import com.thalesgroup.tshpaysample.sdk.payment.TshPaymentErrorData;
import com.thalesgroup.tshpaysample.ui.views.ViewCardFront;

public class FragmentPaymentError extends AbstractFragment {

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
        final View root = inflater.inflate(R.layout.fragment_payment_error, container, false);
        final TextView messageTextView = root.findViewById(R.id.message);
        final ViewCardFront cardFrontView = root.findViewById(R.id.fragment_payment_error_card_visual);

        final TshPaymentErrorData data = getPaymentActivity().getErrorData();
        if (data != null) {
            messageTextView.setText(data.getMessage());
            if(!TextUtils.isEmpty(data.getDigitalizedCardId())) {
                cardFrontView.loadCardDetails(new CardWrapper(data.getDigitalizedCardId()));
            }
        }
        return root;
    }

    //endregion

}
