/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.ui;

import android.content.BroadcastReceiver;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.thalesgroup.tshpaysample.R;
import com.thalesgroup.tshpaysample.sdk.SdkHelper;
import com.thalesgroup.tshpaysample.sdk.helpers.InternalNotificationsUtils;
import com.thalesgroup.tshpaysample.ui.fragments.AbstractFragment;
import com.thalesgroup.tshpaysample.ui.views.ViewProgress;
import com.thalesgroup.tshpaysample.utlis.CommonUtils;

public class BaseAppActivity extends AppCompatActivity {
    private static final String FRAGMENT_TAG = AbstractFragment.class.getSimpleName();

    private boolean mProgressVisible = false;

    private ConstraintLayout mProgressViewContainer;
    private ViewProgress mProgressView;

    private BroadcastReceiver mPushReceiver;

    protected void onViewCreated() {
        // Universal loading overlay above all fragments.
        mProgressViewContainer = findViewById(R.id.activity_main_view_progress_container);
        mProgressView = findViewById(R.id.activity_main_view_progress);

        // Observe for any init errors.
        SdkHelper.getInstance().getInit().getSdkInitState().observe(this, tshInitState -> {
            // Error might be null, but method will display only if there is some.
            displayMessageToast(tshInitState.getError());
        });

        // Register for incoming push notifications.
        mPushReceiver = InternalNotificationsUtils.registerForPushMsgProcessingResult(this, (serverMessageInfoList, error) -> {
            final AbstractFragment currentFragment = getCurrentFragment();
            if (currentFragment != null) {
                currentFragment.onPushMsgProcessingResult(serverMessageInfoList, error);
            }
            displayMessageToast(error);
        });

    }

    public void reloadFragmentData() {
        final AbstractFragment currentFragment = getCurrentFragment();
        if (currentFragment != null) {
            currentFragment.onReloadData();
        }
    }

    public void progressShow(final @StringRes int value) {
        progressShow(getString(value));
    }

    public void progressShow(final String value) {
        // Caption can be change all the time.
        mProgressView.updateCaption(value);

        // Animate only in case it's not already visible.
        if (!mProgressVisible) {
            mProgressViewContainer.clearAnimation();
            mProgressViewContainer.setAnimation(CommonUtils.getFadeInAnimation(null));
            mProgressViewContainer.setVisibility(View.VISIBLE);

            // Disable all user inputs.
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

            mProgressVisible = true;
        }
    }

    public void progressHide() {
        // Animate only in case it's visible.
        if (mProgressVisible) {
            mProgressViewContainer.clearAnimation();
            mProgressViewContainer.setAnimation(CommonUtils.getFadeOutAnimation(() -> {
                mProgressViewContainer.setVisibility(View.INVISIBLE);

                // Restore user input.
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);

                mProgressVisible = false;
            }));
        }
    }

    /**
     * Shows fragment.
     *
     * @param fragment       Fragment to show.
     * @param addToBackStack {@code True} if Fragment should be added to backstack, else {@code false}.
     */
    public void showFragment(final AbstractFragment fragment, final boolean addToBackStack) {
        final FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_container, fragment, FRAGMENT_TAG);

        if (addToBackStack) {
            fragmentTransaction.addToBackStack(null);
        }

        fragmentTransaction.commit();
    }

    public void hideCurrentFragment() {
        getSupportFragmentManager().popBackStack();
    }

    protected AbstractFragment getCurrentFragment() {
        return (AbstractFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);
    }

    //endregion

    //region Public API

    public void displayMessageToast(final @StringRes int error) {
        displayMessageToast(getString(error));
    }

    public void displayMessageToast(final String error) {
        if (error != null) {
            Toast.makeText(this, error, Toast.LENGTH_LONG).show();
        }
    }

    //endregion

    //region User Interface

    @Override
    public void onBackPressed() {
        // Back button is only allowed if progress bar is not visible and current
        // fragment does allow it.
        final AbstractFragment currentFragment = getCurrentFragment();
        if (!mProgressVisible && (currentFragment == null || currentFragment.isBackButtonAllowed())) {
            super.onBackPressed();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Make sure we will clean up all receivers.
        if (mPushReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mPushReceiver);
            mPushReceiver = null;
        }
    }
}
