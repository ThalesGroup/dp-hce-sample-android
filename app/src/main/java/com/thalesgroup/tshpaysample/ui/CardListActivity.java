/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcManager;
import android.nfc.cardemulation.CardEmulation;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import com.gemalto.mfs.mwsdk.mobilegateway.enrollment.IDVMethodSelector;
import com.gemalto.mfs.mwsdk.mobilegateway.enrollment.PendingCardActivation;
import com.thalesgroup.tshpaysample.R;
import com.thalesgroup.tshpaysample.sdk.SdkHelper;
import com.thalesgroup.tshpaysample.sdk.enrollment.TshEnrollment;
import com.thalesgroup.tshpaysample.sdk.enrollment.TshEnrollmentDelegate;
import com.thalesgroup.tshpaysample.sdk.enrollment.TshEnrollmentState;
import com.thalesgroup.tshpaysample.sdk.payment.TshPaymentHceService;
import com.thalesgroup.tshpaysample.ui.fragments.FragmentSplash;
import com.thalesgroup.tshpaysample.ui.fragments.FragmentTermsAndConditions;
import com.thalesgroup.tshpaysample.utlis.AppLoggerHelper;

public class CardListActivity extends BaseAppActivity implements TshEnrollmentDelegate {

    //region Defines

    private static final String TAG = CardListActivity.class.getSimpleName();

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
    }

    @Override
    public void onResume() {
        super.onResume();

        final TshEnrollment tshEnrollment = SdkHelper.getInstance().getTshEnrollment();
        if (tshEnrollment.getEnrollmentState() != mLastProcessedState) {
            onStateChange(tshEnrollment.getEnrollmentState(), tshEnrollment.getEnrollmentError());
        }
        reloadFragmentData();
    }

    //endregion

    //region TshEnrollmentDelegate

    public void onStateChange(final TshEnrollmentState state, final String error) {
        if (state == TshEnrollmentState.ENROLLING_FINISHED_WAITING_FOR_SERVER) {
            progressHide();
            displayMessageToast(state.getActionDescription());
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
            case APP2APP_NEEDED:
                // Not in the scope of sample app.
                displayMessageToast(R.string.sdk_not_in_scope);
                break;
            default:
                displayMessageToast(R.string.sdk_idv_method_not_handled);
                break;
        }

    }

    //endregion


    //region Private helpers
    private void checkAndSetDefaultForTapAndPay() {
        boolean hasNfc = getPackageManager().hasSystemFeature("android.hardware.nfc");
        boolean supportsNfc = getPackageManager().hasSystemFeature("android.hardware.nfc.hce");

        AppLoggerHelper.debug(TAG, String.format("Has NFC: %b; Supports HCE: %b", hasNfc, supportsNfc));

        if (!hasNfc || !supportsNfc){
            AppLoggerHelper.error(TAG, "The device does no have NFC interface or does not support HCE!");
            return;
        }

        final ComponentName appHceComponent = new ComponentName(this, TshPaymentHceService.class.getCanonicalName());
        final NfcManager manager = (NfcManager) this.getSystemService(Context.NFC_SERVICE);
        final CardEmulation cardEmulation = CardEmulation.getInstance(manager.getDefaultAdapter());

        if(cardEmulation.isDefaultServiceForCategory(appHceComponent, CardEmulation.CATEGORY_PAYMENT)){
            AppLoggerHelper.debug(TAG, "App's service is already set as default for payment");
        } else {
            AppLoggerHelper.debug(TAG, "App's service is NOT set as default for payment => prompting the system to set it");

            final Intent activate = new Intent();
            activate.setAction(CardEmulation.ACTION_CHANGE_DEFAULT);
            activate.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activate.putExtra(CardEmulation.EXTRA_SERVICE_COMPONENT, appHceComponent);
            activate.putExtra(CardEmulation.EXTRA_CATEGORY, CardEmulation.CATEGORY_PAYMENT);

            // It is also possible to use startActivityForResult and react e.g. with a confirmation dialog when user chose "no"
            this.startActivity(activate);
        }
    }

    //endregion

}