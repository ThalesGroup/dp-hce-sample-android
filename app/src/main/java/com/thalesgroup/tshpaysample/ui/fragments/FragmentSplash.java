/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.thalesgroup.tshpaysample.R;
import com.thalesgroup.tshpaysample.sdk.SdkHelper;
import com.thalesgroup.tshpaysample.sdk.init.TshInitState;
import com.thalesgroup.tshpaysample.utlis.AppLoggerHelper;

public class FragmentSplash extends AbstractFragment {

    //region Defines

    private static final String TAG = FragmentSplash.class.getSimpleName();

    private TshInitState mStateHandled = TshInitState.INACTIVE;
    private Button mButtonRetry;
    private TextView mTextState;
    private ProgressBar mProgressState;

    //endregion

    //region Life Cycle

    @Override
    public int getFragmentCaption() {
        return R.string.fragment_splash_caption;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View retValue = inflater.inflate(R.layout.fragment_splash, container, false);

        mTextState = retValue.findViewById(R.id.fragment_splash_text_state);
        mProgressState = retValue.findViewById(R.id.fragment_splash_progress_bar);
        mButtonRetry = retValue.findViewById(R.id.fragment_splash_button_retry);
        mButtonRetry.setOnClickListener(this::onButtonPressedRetry);

        return retValue;
    }

    @Override
    public void onResume() {
        super.onResume();

        checkState();
    }

    @Override
    public void onInitStateChanged(@NonNull final TshInitState state,
                                   @Nullable final String error) {
        super.onInitStateChanged(state, error);

        checkState();
    }

    //endregion

    //region Private Helpers

    private void checkState() {
        final TshInitState currentState = SdkHelper.getInstance().getInit().geInitState();
        if (!mStateHandled.equals(currentState)) {
            switch (currentState) {
                case INACTIVE:
                    break;
                case INIT_IN_PROGRESS:
                    mButtonRetry.setVisibility(View.INVISIBLE);
                    mProgressState.setVisibility(View.VISIBLE);
                    mTextState.setText(R.string.fragment_splash_state_in_progress);
                    break;
                case INIT_FAILED:
                    mButtonRetry.setVisibility(View.VISIBLE);
                    mProgressState.setVisibility(View.INVISIBLE);
                    mTextState.setText(R.string.fragment_splash_state_failed);
                    break;
                case INIT_SUCCESSFUL:
                    getMainActivity().showFragment(new FragmentCardList(), false);
                    break;
                default:
                    AppLoggerHelper.error(TAG, "Unknown init state: " + currentState.toString());
                    break;
            }

            mStateHandled = currentState;
        }
    }

    //endregion

    //region User Interface

    private void onButtonPressedRetry(final View sender) {
        SdkHelper.getInstance().getInit().init(getContext());
    }

    //endregion
}
