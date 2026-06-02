/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.ui.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
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
        final ImageView errorIcon = root.findViewById(R.id.iv_error_icon);

        final TshPaymentErrorData error = getPaymentActivity().getErrorData();
        if (error != null) {
            final StringBuilder sb = new StringBuilder();

            if(!TextUtils.isEmpty(error.getCode())) {
                sb.append(error.getCode());
            }

            if(!TextUtils.isEmpty(error.getCode()) && !TextUtils.isEmpty(error.getMessage())){
                sb.append(": ");
            }

            if(!TextUtils.isEmpty(error.getMessage())){
                sb.append(error.getMessage());
            }

            messageTextView.setText(sb.toString());

            if(!TextUtils.isEmpty(error.getDigitalizedCardId())) {
                cardFrontView.loadCardDetails(new CardWrapper(error.getDigitalizedCardId()));
            }
        }

        // Animate icon
        final Animation shake = AnimationUtils.loadAnimation(getContext(), R.anim.shake);
        errorIcon.startAnimation(shake);

        root.findViewById(R.id.btn_retry).setOnClickListener(v -> requireActivity().finish());

        return root;
    }

    //endregion

}
