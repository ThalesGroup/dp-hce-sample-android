/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.sdk.helpers;

import androidx.annotation.NonNull;

import com.gemalto.mfs.mwsdk.mobilegateway.enrollment.CardBitmap;
import com.gemalto.mfs.mwsdk.mobilegateway.utils.MGAbstractAsyncHandler;
import com.gemalto.mfs.mwsdk.mobilegateway.utils.MGAsyncResult;

public class AsyncHandlerCardBitmap extends MGAbstractAsyncHandler<CardBitmap>  {

    //region Defines

    public interface Delegate {
        void onSuccess(final CardBitmap value);

        void onError(final String error);
    }

    private final Delegate mDelegate;

    //endregion

    //region Life Cycle

    public AsyncHandlerCardBitmap(@NonNull final Delegate delegate) {
        super();

        mDelegate = delegate;
    }

    //endregion


    //region MGAbstractAsyncHandler

    @Override
    public void onComplete(final MGAsyncResult<CardBitmap> mgAsyncResult) {
        if (mgAsyncResult.isSuccessful()) {
            mDelegate.onSuccess(mgAsyncResult.getResult());
        } else {
            mDelegate.onError(mgAsyncResult.getErrorCode().getMessage());
        }
    }

    //endregion
}


