/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.ui.fragments;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;

import com.gemalto.mfs.mwsdk.cdcvm.DeviceCVMVerifier;
import com.gemalto.mfs.mwsdk.cdcvm.DeviceCVMVerifyAdditionalErrors;
import com.gemalto.mfs.mwsdk.cdcvm.DeviceCVMVerifyListener;
import com.gemalto.mfs.mwsdk.payment.PaymentBusinessManager;
import com.gemalto.mfs.mwsdk.sdkconfig.SDKError;
import com.thalesgroup.tshpaysample.R;
import com.thalesgroup.tshpaysample.sdk.helpers.CardWrapper;
import com.thalesgroup.tshpaysample.sdk.payment.TshPaymentAuthenticationRequestData;
import com.thalesgroup.tshpaysample.ui.views.ViewCardFront;
import com.thalesgroup.tshpaysample.utlis.AppLoggerHelper;

public class FragmentPaymentAuthentication extends AbstractFragment {

    //region Defines
    private static final String TAG = FragmentPaymentAuthentication.class.getSimpleName();

    private DeviceCVMVerifier mDeviceCVMVerifier;

    private BiometricPrompt mBiometricPrompt;
    private TshPaymentAuthenticationRequestData mAuthData;

    private TextView mMessage;

    //endregion

    //region Life Cycle

    @Override
    public int getFragmentCaption() {
        return R.string.fragment_payment_caption;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View root = inflater.inflate(R.layout.fragment_payment_authentication, container, false);

        mMessage = root.findViewById(R.id.message);

        /**
         * There is a button that allows to re-try the authentication if the prompt was dismissed
         */
        final View btnAuthenticate = root.findViewById(R.id.btn_authenticate);
        btnAuthenticate.setOnClickListener(view -> doAuthenticate());

        mAuthData = getPaymentActivity().getAuthData();

        final ViewCardFront cardView = root.findViewById(R.id.fragment_payment_authentication_card_visual);
        cardView.loadCardDetails(new CardWrapper(mAuthData.getDigitalizedCardId()));

        mDeviceCVMVerifier = (DeviceCVMVerifier) PaymentBusinessManager.getPaymentBusinessService()
                .getActivatedPaymentService().getCHVerifier(mAuthData.getMethod());
        mDeviceCVMVerifier.setDeviceCVMVerifyListener(deviceCVMVerifyListener);


        /**
         * This demonstrates so called Delegated Authentication flow
         * The app performs the authentication of the end-user on its own
         * and signals the completion to the SDK through {@link DeviceCVMVerifier#onDelegatedAuthPerformed(long)}
         */
        mBiometricPrompt = new BiometricPrompt(getActivity(), new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(final int errorCode, @NonNull final CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                AppLoggerHelper.error(TAG, "onAuthenticationError() " + errorCode + ": " + errString);
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull final BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                AppLoggerHelper.debug(TAG, "onAuthenticationSucceeded() authenticationType =  " + result.getAuthenticationType());
                mDeviceCVMVerifier.onDelegatedAuthPerformed(System.currentTimeMillis());
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                AppLoggerHelper.error(TAG, "onAuthenticationFailed()");
            }
        });


        // If user hits the back button when fragment is visible then tear down the authentication and payment completely
        getActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                AppLoggerHelper.debug(TAG,"OnBackPressedCallback.handleOnBackPressed()");
                mDeviceCVMVerifier.onDelegatedAuthCancelled();
                PaymentBusinessManager.getPaymentBusinessService().deactivate();
                setEnabled(false);
                getActivity().onBackPressed();
            }
        });

        doAuthenticate();


        return root;
    }

    private void doAuthenticate() {

        String description;
        if(mAuthData.getAmount() == -1.0){
            description = getString(R.string.authentication_prompt_info_description_wo_amount);
        } else {
            description = getString(R.string.authentication_prompt_info_description, mAuthData.getAmount(), mAuthData.getCurrency());
        }

        final BiometricPrompt.PromptInfo promptInfo = buildPromptInfo(getString(R.string.authentication_prompt_info_title),
                getString(R.string.authentication_prompt_info_subtitle),
                description
        );

        mMessage.setText(null);

        new Handler(Looper.getMainLooper()).post(() -> {
            AppLoggerHelper.debug(TAG, "Calling BiometricPrompt#authenticate()");
            mBiometricPrompt.authenticate(promptInfo);
        });
    }



    BiometricPrompt.PromptInfo buildPromptInfo(final CharSequence title, final CharSequence subTitle, final CharSequence description){

        final BiometricPrompt.PromptInfo.Builder builder = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subTitle)
                .setDescription(description);

        AppLoggerHelper.debug(TAG, "buildPromptInfo() SDK_INT: " + Build.VERSION.SDK_INT);

        if (Build.VERSION.SDK_INT >= 30) {
            /*
            Note that not all combinations of authenticator types are supported prior to Android 11 (API 30).
            Specifically, DEVICE_CREDENTIAL alone is unsupported prior to API 30, and BIOMETRIC_STRONG | DEVICE_CREDENTIAL is unsupported on API 28-29.
            Setting an unsupported value on an affected Android version will result in an error when calling build().

            ref: https://developer.android.com/reference/androidx/biometric/BiometricPrompt.PromptInfo.Builder#setAllowedAuthenticators(int)
            */
            builder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL);
        } else if(!isOnTopOfLockScreen()){
            /*
             * Allows device credentials only if not on top of lock screen due to known bug on Android 10.
             * See: https://issuetracker.google.com/issues/145231213
             */
            builder.setDeviceCredentialAllowed(true);
        } else {
            /*
                Note that this option is incompatible with device credential authentication
                and must NOT be set if the latter is enabled via setAllowedAuthenticators(int) or setDeviceCredentialAllowed(boolean).

                ref: https://developer.android.com/reference/androidx/biometric/BiometricPrompt.PromptInfo.Builder#setNegativeButtonText(java.lang.CharSequence)
            */
            builder.setNegativeButtonText(getText(R.string.authentication_prompt_info_negative_button_text));
        }


        return builder.build();
    }


    /**
     * This listener receives signals from TSH Pay SDK if the delegated authentication result was
     * processed fine or not
     */
    final DeviceCVMVerifyListener deviceCVMVerifyListener = new DeviceCVMVerifyListener() {
        @Override
        public void onVerifySuccess() {
            AppLoggerHelper.debug(TAG, "DeviceCVMVerifyListener.onVerifySuccess()");
        }

        @Override
        public void onVerifyError(final SDKError<Integer> sdkErrorInteger) {
            AppLoggerHelper.debug(TAG, "DeviceCVMVerifyListener.onVerifyError(): " + sdkErrorInteger.getErrorCode() + " : " + sdkErrorInteger.getErrorMessage());

            if(sdkErrorInteger.getErrorCode() == DeviceCVMVerifyAdditionalErrors.PAYMENT_AUTHENTICATION_STATE_MANAGEMENT_ERROR){
                /**
                 * This is known to happen on device which provide face recognition as biometric
                 * authentication method, but it is considered WEAK and thus not provide access
                 * to Android Keystore.
                 *
                 * The recommendation is to use only BIOMETRIC_STRONG authentication methods,
                 * but this is supported only on Android 11 (API 30) and above.
                 */
                mMessage.setText(R.string.authentication_prompt_message_hint_auth_failed);
            } else {
                mMessage.setText(sdkErrorInteger.getErrorMessage());
            }
        }

        @Override
        public void onVerifyFailed() {
            AppLoggerHelper.debug(TAG, "DeviceCVMVerifyListener.onVerifyFailed()");
        }

        @Override
        public void onVerifyHelp(final int helpCode, final CharSequence charSequence) {
            AppLoggerHelper.debug(TAG, "DeviceCVMVerifyListener.onVerifyHelp(): " + helpCode + " : " + charSequence);
        }
    };



    /**
     * @return True means the Activity & Fragment is on top of the lock screen
     */
    private boolean isOnTopOfLockScreen() {
        final KeyguardManager keyguardManager = (KeyguardManager) getActivity().getSystemService(Context.KEYGUARD_SERVICE);
        return keyguardManager != null && keyguardManager.inKeyguardRestrictedInputMode();
    }

    //endregion

}
