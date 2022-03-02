package com.thalesgroup.tshpaysample.ui;

import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.thalesgroup.tshpaysample.R;
import com.thalesgroup.tshpaysample.sdk.helpers.InternalNotificationsUtils;
import com.thalesgroup.tshpaysample.sdk.payment.TshPaymentAuthenticationRequestData;
import com.thalesgroup.tshpaysample.sdk.payment.TshPaymentData;
import com.thalesgroup.tshpaysample.sdk.payment.TshPaymentErrorData;
import com.thalesgroup.tshpaysample.sdk.payment.TshPaymentState;
import com.thalesgroup.tshpaysample.ui.fragments.AbstractFragment;
import com.thalesgroup.tshpaysample.ui.fragments.FragmentPaymentAuthentication;
import com.thalesgroup.tshpaysample.ui.fragments.FragmentPaymentError;
import com.thalesgroup.tshpaysample.ui.fragments.FragmentPaymentReady;
import com.thalesgroup.tshpaysample.ui.fragments.FragmentPaymentStarted;
import com.thalesgroup.tshpaysample.ui.fragments.FragmentPaymentSuccess;
import com.thalesgroup.tshpaysample.utlis.AppLoggerHelper;

public class PaymentActivity extends BaseAppActivity {

    //region Defines

    private static final String TAG = PaymentActivity.class.getSimpleName();

    public static final String STATE_EXTRA_KEY = "STATE_EXTRA_KEY";
    public static final String PAYMENT_DATA_EXTRA_KEY = "PAYMENT_DATA_EXTRA_KEY";

    private BroadcastReceiver mPaymentCountdownReceiver;

    private TshPaymentErrorData mErrorData;
    private TshPaymentData mSuccessData;
    private TshPaymentAuthenticationRequestData mAuthData;
    private TshPaymentData mSecondTapData;

    //endregion

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_payment);

        super.onViewCreated();


        // Register for payment activity.
        mPaymentCountdownReceiver = InternalNotificationsUtils.registerForPaymentCountdown(this, seconds -> {
            final AbstractFragment currentFragment = getCurrentFragment();
            if (currentFragment != null) {
                currentFragment.onPaymentCountdownChanged(seconds);
            }
        });

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        onNewIntent(getIntent());
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);

        if (intent.hasExtra(PAYMENT_DATA_EXTRA_KEY) && intent.hasExtra(STATE_EXTRA_KEY)) {

            final TshPaymentState state = (TshPaymentState) intent.getSerializableExtra(STATE_EXTRA_KEY);
            final TshPaymentData paymentData = (TshPaymentData) intent.getSerializableExtra(PAYMENT_DATA_EXTRA_KEY);

            final AbstractFragment currentFragment = getCurrentFragment();
            if (currentFragment != null) {
                currentFragment.onPaymentStatusChanged(state);
            }

            switch (state) {
                case STATE_ON_TRANSACTION_STARTED:
                    showFragment(new FragmentPaymentStarted(), false);
                    break;
                case STATE_ON_AUTHENTICATION_REQUIRED:
                    mAuthData = (TshPaymentAuthenticationRequestData) paymentData;
                    showFragment(new FragmentPaymentAuthentication(), false);
                    break;
                case STATE_ON_READY_TO_TAP:
                    mSecondTapData = (TshPaymentData) paymentData;
                    showFragment(new FragmentPaymentReady(), false);
                    break;
                case STATE_ON_TRANSACTION_COMPLETED:
                    mSuccessData = (TshPaymentData) paymentData;
                    showFragment(new FragmentPaymentSuccess(), false);
                    break;
                case STATE_ON_ERROR:
                    mErrorData = (TshPaymentErrorData) paymentData;
                    showFragment(new FragmentPaymentError(), false);
                    break;
                default:
                    AppLoggerHelper.error(TAG, "Unknown transaction state: " + state.toString());
                    break;
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mPaymentCountdownReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(mPaymentCountdownReceiver);
            mPaymentCountdownReceiver = null;
        }

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
    }


    public TshPaymentAuthenticationRequestData getAuthData() {
        return mAuthData;
    }

    public TshPaymentErrorData getErrorData() {
        return mErrorData;
    }

    public TshPaymentData getSuccessData() {
        return mSuccessData;
    }

    public TshPaymentData getSecondTapData() {
        return mSecondTapData;
    }
}
