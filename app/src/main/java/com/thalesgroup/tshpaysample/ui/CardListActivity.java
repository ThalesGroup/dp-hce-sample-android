/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import com.gemalto.mfs.mwsdk.mobilegateway.enrollment.AppToAppData;
import com.gemalto.mfs.mwsdk.mobilegateway.enrollment.IDVMethodSelector;
import com.gemalto.mfs.mwsdk.mobilegateway.enrollment.PendingCardActivation;
import com.thalesgroup.tshpaysample.R;
import com.thalesgroup.tshpaysample.sdk.SdkHelper;
import com.thalesgroup.tshpaysample.sdk.enrollment.TshEnrollment;
import com.thalesgroup.tshpaysample.sdk.enrollment.TshEnrollmentDelegate;
import com.thalesgroup.tshpaysample.sdk.enrollment.TshEnrollmentState;
import com.thalesgroup.tshpaysample.sdk.helpers.HceHelper;
import com.thalesgroup.tshpaysample.ui.fragments.FragmentCardEnrollment;
import com.thalesgroup.tshpaysample.ui.fragments.FragmentCardList;
import com.thalesgroup.tshpaysample.ui.fragments.FragmentSplash;
import com.thalesgroup.tshpaysample.ui.fragments.FragmentTermsAndConditions;
import com.thalesgroup.tshpaysample.utlis.AppLoggerHelper;
import com.thalesgroup.tshpaysample.utlis.CommonUtils;

import java.util.List;

public class CardListActivity extends BaseAppActivity implements TshEnrollmentDelegate {

    //region Defines
    public static final String EXTRA_START_ENROLLMENT = "com.thalesgroup.tshpaysample.EXTRA_START_ENROLLMENT";

    private static final String TAG = CardListActivity.class.getSimpleName();

    private TshEnrollmentState mLastProcessedState;

    //endregion



    //region Life Cycle



    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        super.onViewCreated();

        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {



        super.onNewIntent(intent);
        // Also handle the intent if the activity is already running and receives a new one
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        AppLoggerHelper.debug(TAG, String.format("handleIntent() %s", (intent == null ? "null" : "extras: " + CommonUtils.bundleToJson(intent.getExtras()))));

        if (intent != null && intent.getBooleanExtra(EXTRA_START_ENROLLMENT, false)) {
            // To prevent back navigation to a blank screen, we must ensure FragmentCardList is the root.
            // This mirrors the flow of clicking "Add Card" from the list.
            showFragment(new FragmentCardList(), false);
            showFragment(new FragmentCardEnrollment(), true);

            // Important: To prevent this from happening again on configuration changes (like rotation),
            // remove the extra from the intent after it has been processed.
            getIntent().removeExtra(EXTRA_START_ENROLLMENT);
        } else {
            // By default load splash screen.
            showFragment(new FragmentSplash(), false);
        }
    }

    @Override
    public void onResume() {
        AppLoggerHelper.info(TAG, "onResume() called");

        super.onResume();

        final TshEnrollment tshEnrollment = SdkHelper.getInstance().getTshEnrollment();

        AppLoggerHelper.debug(TAG, String.format("getEnrollmentState()=%s; mLastProcessedState=%s", tshEnrollment.getEnrollmentState(), mLastProcessedState));
        if (tshEnrollment.getEnrollmentState() != mLastProcessedState) {
            onStateChange(tshEnrollment.getEnrollmentState(), tshEnrollment.getEnrollmentError());
        }
        reloadFragmentData();

        HceHelper.handleForegroundPreference(this, HceHelper.LifeCycleHandler.ON_RESUME);

    }



    @Override
    protected void onPause() {
        HceHelper.handleForegroundPreference(this, HceHelper.LifeCycleHandler.ON_PAUSE);

        super.onPause();
    }

    //endregion

    //region TshEnrollmentDelegate

    public void onStateChange(final TshEnrollmentState state, final String error) {
        if (state == TshEnrollmentState.ENROLLING_FINISHED) {
            progressHide();
            displayMessageToast(state.getActionDescription());
            showFragment(new FragmentCardList(), false);
            reloadFragmentData();
        } else if (state == TshEnrollmentState.ELIGIBILITY_TERMS_AND_CONDITIONS) {
            progressHide();
            showFragment(new FragmentTermsAndConditions(), true);
        } else if (state == TshEnrollmentState.DIGITIZATION_FINISHED) {
            reloadFragmentData();
        } else if (state.isProgressState()) {
            // Any ongoing enrollment type.
            progressShow(state.getActionDescription());
        } else {
            // Enrollment not started or failed with error.
            progressHide();
            displayMessageToast(error);
        }

        mLastProcessedState = state;
    }

    @Override
    public void onSelectIDVMethod(final IDVMethodSelector idvMethodSelector) {
        if (idvMethodSelector.getIdvMethodList().length == 0) {
            AppLoggerHelper.error(TAG, "IDVMethodSelector is empty");
            return;
        }

        // Prepare list of possible options.
        final String[] methods = new String[idvMethodSelector.getIdvMethodList().length];
        for (int loopIndex = 0; loopIndex < idvMethodSelector.getIdvMethodList().length; loopIndex++) {
            methods[loopIndex] = idvMethodSelector.getIdvMethodList()[loopIndex].getType();
        }

        // Display selection dialog.
        final AlertDialog.Builder builder = new AlertDialog.Builder(CardListActivity.this);
        builder.setTitle(getString(R.string.sdk_idv_selection_dialog));
        builder.setSingleChoiceItems(methods, 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                idvMethodSelector.select(idvMethodSelector.getIdvMethodList()[which].getId());
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(getString(R.string.common_word_cancel), null);
        builder.create().show();
    }

    public void onActivationRequired(final PendingCardActivation pendingCardActivation) {
        switch (pendingCardActivation.getState()) {
            case IDV_METHOD_NOT_SELECTED:
                // Not relevant for this method.
                break;
            case OTP_NEEDED:
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.sdk_otp_entry_dialog);
                final EditText input = new EditText(this);
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                builder.setView(input);
                builder.setPositiveButton(R.string.common_word_ok, (dialog, which) -> {
                    final String enteredValue = input.getText().toString();
                    if (!enteredValue.isEmpty()) {
                        pendingCardActivation.activate(enteredValue.getBytes(), SdkHelper.getInstance().getTshEnrollment());
                    } else {
                        // Invalid entry. Display message and re-try.
                        displayMessageToast(R.string.sdk_otp_entry_empty_string);
                        onActivationRequired(pendingCardActivation);
                    }
                });
                builder.setNegativeButton(getString(R.string.common_word_cancel), null);
                builder.show();

                break;
            case WEB_3DS_NEEDED:
                // Not in the scope of sample app at all
                displayMessageToast(R.string.sdk_not_in_scope);
                break;

            case APP2APP_NEEDED:
                handleApp2App(pendingCardActivation.getAppToAppData());
                break;

            default:
                displayMessageToast(R.string.sdk_idv_method_not_handled);
                break;
        }

    }

    /**
     * Tries to parse the AppToAppData and start the bank app for APP2APP ID&V flow
     *
     * This demo implementation assumes that the application details are provided in AppToAppData.getSource()
     * and follows the structure "packageName|action".
     * For example: com.test.bankingapp|com.test.bankingapp.activate
     *
     * In practice the values are coming from the card issuer in a reply to a tokenization request
     * or are configured in the TSP system (VCMM or MDES manager).
     *
     * @param appToAppData App to app data obtained from the PendingCardActivation object
     */
    private void handleApp2App(final AppToAppData appToAppData) {

        if(appToAppData == null){
            throw new IllegalArgumentException("appToAppData can't be null");
        }

        AppLoggerHelper.info(TAG, String.format("handleApp2App(): scheme=%s; source=%s; payload=%s", appToAppData.getScheme(), appToAppData.getSource(), appToAppData.getPayLoad()));

        final String pkgAndAction = appToAppData.getSource();

        if (pkgAndAction == null || !pkgAndAction.contains("|")) {
            displayMessageToast("AppToAppData source is invalid");
            return;
        }

        final String[] parts = pkgAndAction.split("\\|", 2);
        if (parts.length != 2) {
            displayMessageToast("AppToAppData source is invalid");
            return;
        }

        final String packageName = parts[0];
        final String action = parts[1];

        final Intent intent = new Intent();
        intent.setPackage(packageName);
        intent.setAction(action);
        intent.putExtra("SCHEME", appToAppData.getScheme());
        intent.putExtra("PAYLOAD", appToAppData.getPayLoad());

        AppLoggerHelper.debug(TAG, "AppToApp Intent: " + intent);

        final PackageManager packageManager = getPackageManager();
        final List<ResolveInfo> activities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);

        if (activities != null && !activities.isEmpty()) {
            AppLoggerHelper.info(TAG, "Starting the bank app");
            startActivity(intent);
        } else {
            AppLoggerHelper.warn(TAG, "Bank application is not installed!");
            displayMessageToast("Bank application is not installed!");
        }
    }

    //endregion



}