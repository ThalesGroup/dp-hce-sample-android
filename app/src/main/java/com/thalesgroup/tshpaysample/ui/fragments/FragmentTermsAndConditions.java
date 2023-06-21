/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.thalesgroup.tshpaysample.R;
import com.thalesgroup.tshpaysample.sdk.SdkHelper;

public class FragmentTermsAndConditions extends AbstractFragment {
    
    //region Life Cycle

    @Override
    public int getFragmentCaption() {
        return R.string.fragment_terms_and_conditions_caption;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View retValue = inflater.inflate(R.layout.fragment_terms_and_conditions, container, false);

        // Load actual T&C data from enrollment helper.
        final WebView webView = retValue.findViewById(R.id.fragment_terms_and_conditions_webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadData(SdkHelper.getInstance().getTshEnrollment().getTermsAndConditionsText(),
                "text/html; charset=utf-8", "UTF-8");

        retValue.findViewById(R.id.fragment_terms_and_conditions_button_accept).setOnClickListener(this::onButtonPressedAccept);
        retValue.findViewById(R.id.fragment_terms_and_conditions_button_decline).setOnClickListener(this::onButtonPressedDecline);

        return retValue;
    }

    @Override
    public boolean isBackButtonAllowed() {
        return false;
    }

    //endregion

    //region Private Helpers

    private void handleResponse(final boolean accept) {
        // Continue or cancel enrollment
        SdkHelper.getInstance().getTshEnrollment().acceptTermsAndConditions(accept);
        // Hide terms & conditions and get back to enrollment screen.
        getMainActivity().getSupportFragmentManager().popBackStack();
    }

    //endregion

    //region User Interface

    private void onButtonPressedAccept(final View sender) {
        handleResponse(true);
    }

    private void onButtonPressedDecline(final View sender) {
        handleResponse(false);
    }

    //endregion
}
