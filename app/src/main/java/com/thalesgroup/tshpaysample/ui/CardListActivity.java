/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.ui;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.nfc.NfcManager;
import android.nfc.cardemulation.CardEmulation;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.widget.EditText;

import androidx.annotation.NonNull;
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
import com.thalesgroup.tshpaysample.sdk.payment.TshPaymentHceService;
import com.thalesgroup.tshpaysample.ui.fragments.FragmentSplash;
import com.thalesgroup.tshpaysample.ui.fragments.FragmentTermsAndConditions;
import com.thalesgroup.tshpaysample.utlis.AppLoggerHelper;

import java.util.List;

public class CardListActivity extends BaseAppActivity implements TshEnrollmentDelegate {

    //region Defines

    private static final String TAG = CardListActivity.class.getSimpleName();
    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 19641;
    private TshEnrollmentState mLastProcessedState;

    //endregion



    //region Life Cycle

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        super.onViewCreated();

        // By default load splash screen.
        showFragment(new FragmentSplash(), false);

        checkAndSetDefaultForTapAndPay();
        requestNotificationPermissionIfNeeded();
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


    //region Private helpers


    private void checkAndSetDefaultForTapAndPay() {
        if (!HceHelper.doesDeviceSupportHCE(this)) {
            return;
        }

        final ComponentName appHceComponent = new ComponentName(this, TshPaymentHceService.class.getCanonicalName());
        final NfcManager manager = (NfcManager) this.getSystemService(Context.NFC_SERVICE);
        final CardEmulation cardEmulation = CardEmulation.getInstance(manager.getDefaultAdapter());

        if(cardEmulation.isDefaultServiceForCategory(appHceComponent, CardEmulation.CATEGORY_PAYMENT)){
            AppLoggerHelper.debug(TAG, "App's service is already set as default for payment");
        } else if (cardEmulation.categoryAllowsForegroundPreference(CardEmulation.CATEGORY_PAYMENT)){
            AppLoggerHelper.debug(TAG, "Payments with a foreground apps are allowed");
            AppLoggerHelper.warn(TAG, "Payments will be processed ONLY when application is on the foreground!");
        }else {
            AppLoggerHelper.debug(TAG, "App's service is NOT set as default for payment AND foreground app payments are not allowed  => prompting the user to set either option");

            final AlertDialog.Builder builder = new AlertDialog.Builder(CardListActivity.this);
            builder.setTitle("Allow app to use NFC");
            builder.setMessage("The app is not set as default payment app AND payments with foreground apps are not allowed.\nPlease enable either of the options so you could use it for payments.");
            builder.setPositiveButton("Let's do it!", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(final DialogInterface dialogInterface, final int identifier) {
                    final Intent activate = new Intent();
                    activate.setAction(Settings.ACTION_NFC_PAYMENT_SETTINGS);
                    startActivity(activate);
                }
            });
            builder.setNegativeButton("No", null);
            builder.create().show();
        }
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                // Permission not granted, request it
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_POST_NOTIFICATIONS);
            } else {
                AppLoggerHelper.info(TAG, "POST_NOTIFICATIONS permission already granted");
            }
        } else {
            AppLoggerHelper.info(TAG, "No runtime permission needed for notifications on Android < 13");
        }
    }

    // Handle the permission request result
    @Override
    public void onRequestPermissionsResult(final int requestCode,
                                           @NonNull final String[] permissions,
                                           @NonNull final int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                AppLoggerHelper.info(TAG, "Permission granted - you can now show notifications");
            } else {
                AppLoggerHelper.info(TAG, "Permission denied - notify user or disable notifications feature");
            }
        }
    }

    //endregion

}