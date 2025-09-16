/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.thalesgroup.tshpaysample.R;
import com.thalesgroup.tshpaysample.sdk.SdkHelper;
import com.thalesgroup.tshpaysample.sdk.init.TshInitState;
import com.thalesgroup.tshpaysample.sdk.init.TshInitStateEnum;
import com.thalesgroup.tshpaysample.utlis.AppLoggerHelper;

public class FragmentSplash extends AbstractFragment {

    //region Defines

    private static final String TAG = FragmentSplash.class.getSimpleName();

    private TshInitStateEnum mStateHandled = TshInitStateEnum.INACTIVE;
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

        return retValue;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Start observing for changes. Internal logic will skip if the value will remain the same.
        // We do not to reload current state, because that is automatically triggered by registration.
        SdkHelper.getInstance().getInit().getSdkInitState().observe(this, this::checkState);
    }

    //endregion

    //region Private Helpers

    private void checkState(final @Nullable TshInitState state) {
        if (state == null) {
            AppLoggerHelper.error(TAG, "SDK state should never be null.");
            return;
        }

        AppLoggerHelper.debug(TAG, String.format("stateHandled = %s, currentState = %s", mStateHandled, state));

        if (!mStateHandled.equals(state.getState())) {
            switch (state.getState()) {
                case INACTIVE:
                    break;
                case INIT_IN_PROGRESS:
                    mProgressState.setVisibility(View.VISIBLE);
                    mTextState.setText(R.string.fragment_splash_state_in_progress);
                    break;
                case INIT_FAILED:
                    mProgressState.setVisibility(View.INVISIBLE);
                    mTextState.setText(R.string.fragment_splash_state_failed);
                    break;
                case INIT_SUCCESSFUL:
                    getMainActivity().showFragment(new FragmentCardList(), false);
                    break;
                default:
                    AppLoggerHelper.error(TAG, "Unknown init state: " + state);
                    break;
            }

            mStateHandled = state.getState();
        }
    }

    //endregion

}
