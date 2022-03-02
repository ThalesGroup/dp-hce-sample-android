/*
 * MIT License
 *
 * Copyright (c) 2021 Thales DIS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.thalesgroup.tshpaysample.sdk.helpers;

import android.content.Context;

import androidx.annotation.NonNull;

import com.gemalto.mfs.mwsdk.cdcvm.BiometricsSupport;
import com.gemalto.mfs.mwsdk.cdcvm.DeviceCVMEligibilityChecker;
import com.gemalto.mfs.mwsdk.cdcvm.DeviceCVMEligibilityResult;
import com.gemalto.mfs.mwsdk.cdcvm.DeviceKeyguardSupport;
import com.gemalto.mfs.mwsdk.dcm.DigitalizedCardErrorCodes;
import com.gemalto.mfs.mwsdk.dcm.DigitalizedCardManager;
import com.gemalto.mfs.mwsdk.dcm.cdcvm.DeviceCVMManager;
import com.gemalto.mfs.mwsdk.exception.DeviceCVMException;
import com.gemalto.mfs.mwsdk.payment.CHVerificationMethod;
import com.gemalto.mfs.mwsdk.utils.async.AbstractAsyncHandler;
import com.gemalto.mfs.mwsdk.utils.async.AsyncResult;

import java.util.ArrayList;
import java.util.List;

public final class CardListHelper extends AbstractAsyncHandler<String[]> {

    //region Defines

    public interface Delegate {
        void onSuccess(final List<CardWrapper> cardWrappers);

        void onError(final String error);
    }

    private final Delegate mDelegate;
    private final Context mContext;

    //endregion

    //region Life Cycle

    public CardListHelper(@NonNull final Context context,
                          @NonNull final Delegate delegate) {
        super();

        mContext = context;
        mDelegate = delegate;
    }

    //endregion

    //region Public API

    public void getAllCards() {
        DigitalizedCardManager.getAllCards(this);
    }

    //endregion

    //region AbstractAsyncHandler

    @Override
    public void onComplete(final AsyncResult<String[]> asyncResult) {
        if (asyncResult.isSuccessful()) {
            final List<CardWrapper> retValue = new ArrayList<>();
            for (final String loopCard : asyncResult.getResult()) {
                retValue.add(new CardWrapper(loopCard));
            }
            mDelegate.onSuccess(retValue);
        } else {
            final int errorCode = asyncResult.getErrorCode();

            // Card verification method required.
            if (errorCode == DigitalizedCardErrorCodes.CD_CVM_REQUIRED) {

                final DeviceCVMEligibilityResult result = DeviceCVMEligibilityChecker.checkDeviceEligibility(mContext);

                if (result.getBiometricsSupport() == BiometricsSupport.SUPPORTED) {
                    // Biometric support.
                    try {
                        DeviceCVMManager.INSTANCE.initialize(CHVerificationMethod.BIOMETRICS);
                        getAllCards();
                    } catch (final DeviceCVMException exception) {
                        mDelegate.onError(exception.getLocalizedMessage());
                    }
                } else if (result.getDeviceKeyguardSupport() == DeviceKeyguardSupport.SUPPORTED) {
                    // Device keyguard support.
                    try {
                        DeviceCVMManager.INSTANCE.initialize(CHVerificationMethod.DEVICE_KEYGUARD);
                        getAllCards();
                    } catch (final DeviceCVMException exception) {
                        mDelegate.onError(exception.getLocalizedMessage());
                    }
                } else {
                    // Wallet pin is not in the scope!
                    throw new RuntimeException("Device not suitable for this application.");
                }
            } else {
                // Any other states than CVM required should be reported do delegate.
                mDelegate.onError(asyncResult.getErrorMessage());
            }
        }
    }
    //endregion

}