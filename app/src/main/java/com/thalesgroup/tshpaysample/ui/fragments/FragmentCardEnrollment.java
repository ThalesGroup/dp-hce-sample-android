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
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.IdRes;
import androidx.annotation.StringRes;

import com.thalesgroup.tshpaysample.R;
import com.thalesgroup.tshpaysample.sdk.SdkHelper;
import com.thalesgroup.tshpaysample.ui.CardListActivity;
import com.thalesgroup.tshpaysample.ui.views.ViewCardFront;

public class FragmentCardEnrollment extends AbstractFragment {

    //region Defines

    private ViewCardFront mViewCardFront;
    private EditText mEditPan;
    private EditText mEditCvv;
    private EditText mEditExp;

    //endregion

    //region Life Cycle

    @Override
    public int getFragmentCaption() {
        return R.string.fragment_card_enrollment_caption;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View retValue = inflater.inflate(R.layout.fragment_card_enrollment, container, false);

        // Load UI elements and default test data.
        mViewCardFront = retValue.findViewById(R.id.fragment_card_enrollment_card_visual);
        mEditPan = initEdit(retValue, R.id.fragment_card_enrollment_pan, R.string.test_data_yellow_pan, true);
        mEditExp = initEdit(retValue, R.id.fragment_card_enrollment_exp, R.string.test_data_exp, true);
        mEditCvv = initEdit(retValue, R.id.fragment_card_enrollment_cvv, R.string.test_data_cvv, false);

        retValue.findViewById(R.id.fragment_card_enrollment_button_enroll).setOnClickListener(this::onButtonPressEnroll);

        updateCardVisual();

        return retValue;
    }

    //endregion

    //region Private Helpers

    private EditText initEdit(final View parent,
                              @IdRes final int viewId,
                              @StringRes final int textId,
                              final boolean addListener) {
        final EditText retValue = parent.findViewById(viewId);
        retValue.setText(getText(textId));

        if (addListener) {
            retValue.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(final CharSequence sequence,
                                              final int start,
                                              final int count,
                                              final int after) {
                    // We are not interested about this event.
                }

                @Override
                public void onTextChanged(final CharSequence sequence,
                                          final int start,
                                          final int before,
                                          final int count) {
                    // Value of some important text field was changed. Update card graphics.
                    updateCardVisual();
                }

                @Override
                public void afterTextChanged(final Editable sequence) {
                    // We are not interested about this event.
                }
            });
        }

        return retValue;
    }

    private void updateCardVisual() {
        mViewCardFront.setPan(mEditPan.getText().toString());
        mViewCardFront.setExp(mEditExp.getText().toString());
    }

    //endregion

    //region User Interface

    private void onButtonPressEnroll(final View sender) {
        // Clearing focus make more visual sense and it will also trigger UI reload.
        mEditExp.clearFocus();
        mEditCvv.clearFocus();
        mEditPan.clearFocus();

        // Main activity is also acting as delegate for enrollment.
        // Full application might want additional activity just for this reason.
        final CardListActivity cardListActivity = getMainActivity();

        // Extract entered values and make sure they are not empty.
        final String cardPan = mEditPan.getText().toString();
        if (cardPan.isEmpty()) {
            cardListActivity.displayMessageToast(R.string.fragment_card_enrollment_empty_pan);
            return;
        }
        final String cardExp = mEditExp.getText().toString();
        if (cardExp.isEmpty()) {
            cardListActivity.displayMessageToast(R.string.fragment_card_enrollment_empty_exp);
            return;
        }
        final String cardCvv = mEditCvv.getText().toString();
        if (cardCvv.isEmpty()) {
            cardListActivity.displayMessageToast(R.string.fragment_card_enrollment_empty_cvv);
            return;
        }

        // Trigger enrollment, display new progress view and wait for sdk notifications.
        cardListActivity.progressShow(R.string.enrollment_state_inactive);
        SdkHelper.getInstance().getTshEnrollment().enrollCard(cardPan, cardExp, cardCvv, cardListActivity);

        // Hide enrollemnt fragment.
        getMainActivity().hideCurrentFragment();
    }

    //endregion
}