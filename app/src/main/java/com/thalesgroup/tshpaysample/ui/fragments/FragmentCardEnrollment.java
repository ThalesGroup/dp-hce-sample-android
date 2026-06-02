/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.ui.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.thalesgroup.tshpaysample.R;
import com.thalesgroup.tshpaysample.sdk.SdkHelper;
import com.thalesgroup.tshpaysample.ui.CardListActivity;
import com.thalesgroup.tshpaysample.ui.views.ViewCardFront;
import com.thalesgroup.tshpaysample.utlis.AppLoggerHelper;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FragmentCardEnrollment extends AbstractFragment {
    private static final String TAG =  FragmentCardEnrollment.class.getSimpleName();

    //region Defines

    private ViewCardFront mViewCardFront;
    private EditText mEditPan;
    private EditText mEditCvv;
    private EditText mEditExp;
    private Spinner mSpinnerTestCards;

    private View mTestCardSelectionView;
    private final List<TestCard> mTestCards = new ArrayList<>();

    private static class TestCard {
        String name;
        String pan;
        String exp;
        String cvv;

        @NonNull
        @Override
        public String toString() {
            return name;
        }
    }

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
        final View fragmentRootView = inflater.inflate(R.layout.fragment_card_enrollment, container, false);

        // Load UI elements and default test data.
        mViewCardFront = fragmentRootView.findViewById(R.id.fragment_card_enrollment_card_visual);
        mEditPan = initEdit(fragmentRootView, R.id.fragment_card_enrollment_pan, true);
        mEditExp = initEdit(fragmentRootView, R.id.fragment_card_enrollment_exp, true);
        mEditCvv = initEdit(fragmentRootView, R.id.fragment_card_enrollment_cvv, false);

        mTestCardSelectionView = fragmentRootView.findViewById(R.id.fragment_card_enrollment_view_test_card_selection);
        mSpinnerTestCards = fragmentRootView.findViewById(R.id.fragment_card_enrollment_spinner_test_cards);
        setupTestCardsSpinner();

        fragmentRootView.findViewById(R.id.fragment_card_enrollment_button_enroll).setOnClickListener(this::onButtonPressEnroll);

        updateCardVisual();

        return fragmentRootView;
    }

    //endregion

    //region Private Helpers

    private EditText initEdit(final View parent,
                              @IdRes final int viewId,
                              final boolean addListener) {
        final EditText retValue = parent.findViewById(viewId);

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

    private void setupTestCardsSpinner() {
        loadTestCards();

        if(mTestCards.size() <= 1){
            mTestCardSelectionView.setVisibility(View.GONE);
            return;
        }

        final ArrayAdapter<TestCard> adapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_item_test_cards, mTestCards);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerTestCards.setAdapter(adapter);

        mSpinnerTestCards.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    final TestCard selected = mTestCards.get(position);
                    mEditPan.setText(selected.pan);
                    mEditExp.setText(selected.exp);
                    mEditCvv.setText(selected.cvv);
                    updateCardVisual();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Nothing to do
            }
        });
    }

    private void loadTestCards() {
        mTestCards.clear();
        final TestCard placeholder = new TestCard();
        placeholder.name = getString(R.string.fragment_card_enrollment_test_cards_prompt);
        mTestCards.add(placeholder);

        try (Reader reader = new InputStreamReader(requireContext().getAssets().open("test_cards.json"))) {
            final TestCard[] cards = new Gson().fromJson(reader, TestCard[].class);
            if (cards != null) {
                mTestCards.addAll(Arrays.asList(cards));
            }
        } catch (final Exception e) {
            AppLoggerHelper.warn(TAG, "Failed to load test cards from asset file: " + e);
            mTestCards.clear();
        }
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