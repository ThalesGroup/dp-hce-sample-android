package com.thalesgroup.tshpaysample.sdk.helpers;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.nfc.NfcManager;
import android.nfc.cardemulation.CardEmulation;

import androidx.annotation.NonNull;

import com.thalesgroup.tshpaysample.sdk.payment.TshPaymentHceService;
import com.thalesgroup.tshpaysample.utlis.AppLoggerHelper;

public class HceHelper {

    private static final String TAG = HceHelper.class.getSimpleName();

    public enum LifeCycleHandler { ON_RESUME, ON_PAUSE, ON_STOP}

    public static boolean doesDeviceSupportHCE(@NonNull final Context context) {
        final boolean hasNfc = context.getPackageManager().hasSystemFeature("android.hardware.nfc");
        final boolean supportsNfc = context.getPackageManager().hasSystemFeature("android.hardware.nfc.hce");

        AppLoggerHelper.debug(TAG, String.format("doesDeviceSupportHCE(): Has NFC: %b; Supports HCE: %b", hasNfc, supportsNfc));

        if (!hasNfc || !supportsNfc){
            AppLoggerHelper.warn(TAG, "doesDeviceSupportHCE(): The device does no have NFC interface or does not support HCE!");
            return false;
        }

        return true;
    }

    /***
     * Makes sure that app's HCE service is set as preferred service when the app (activity) is on the foreground.
     * The implementation checks if the device supports HCE and if it is already set as default.
     * It then handles the preference change (set/unset) based on the lifecycle handler following
     * the integration contract:
     *  A good paradigm is to call setPreferredService in Activity.onResume, and to call unsetPreferredService in Activity.onPause.
     *  ref: https://developer.android.com/reference/android/nfc/cardemulation/CardEmulation#setPreferredService(android.app.Activity,%20android.content.ComponentName)
     * @param activity
     * @param handler
     */
    public static void handleForegroundPreference(@NonNull final Activity activity, @NonNull final LifeCycleHandler handler) {
        AppLoggerHelper.debug(TAG, String.format("handleForegroundPreference(): activity=%s, handler=%s", activity.getClass().getSimpleName(), handler));

        if(!doesDeviceSupportHCE(activity)) {
            return;
        }

        if(isHceServiceSetAsDefault(activity)) {
            AppLoggerHelper.debug(TAG, "HCE service is already set as default => no need to bother with foreground preference");
            return;
        }

        final NfcManager manager = (NfcManager) activity.getSystemService(Context.NFC_SERVICE);
        final CardEmulation cardEmulation = CardEmulation.getInstance(manager.getDefaultAdapter());

        if (cardEmulation.categoryAllowsForegroundPreference(CardEmulation.CATEGORY_PAYMENT)){
            if(handler == LifeCycleHandler.ON_RESUME) {
                final ComponentName appHceComponent = new ComponentName(activity, TshPaymentHceService.class.getCanonicalName());
                final boolean success = cardEmulation.setPreferredService(activity, appHceComponent);
                if(success) {
                    AppLoggerHelper.debug(TAG, String.format("Preferred HCE service was set to %s ", appHceComponent));
                } else {
                    AppLoggerHelper.error(TAG, String.format("Failed to set app component %s as preferred HCE service", appHceComponent));
                }
            } else if (handler == LifeCycleHandler.ON_PAUSE) {
                cardEmulation.unsetPreferredService(activity);
                AppLoggerHelper.debug(TAG, "Preferred HCE service was unset");
            } else {
                // We should not reach this, but if we will, just log it as warning
                AppLoggerHelper.warn(TAG, "LifeCycleHandler type not supported!");
            }
        }

    }

    public static boolean isHceServiceSetAsDefault(@NonNull final Context context){
        final ComponentName appHceComponent = new ComponentName(context, TshPaymentHceService.class.getCanonicalName());
        final NfcManager manager = (NfcManager) context.getSystemService(Context.NFC_SERVICE);
        final CardEmulation cardEmulation = CardEmulation.getInstance(manager.getDefaultAdapter());

        return cardEmulation.isDefaultServiceForCategory(appHceComponent, CardEmulation.CATEGORY_PAYMENT);
    }
}

