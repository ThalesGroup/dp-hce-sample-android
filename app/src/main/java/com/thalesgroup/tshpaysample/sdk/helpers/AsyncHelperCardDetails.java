/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.sdk.helpers;

import androidx.annotation.NonNull;

import com.gemalto.mfs.mwsdk.dcm.DigitalizedCardDetails;
import com.gemalto.mfs.mwsdk.utils.async.AbstractAsyncHandler;
import com.gemalto.mfs.mwsdk.utils.async.AsyncResult;

public class AsyncHelperCardDetails extends AbstractAsyncHandler<DigitalizedCardDetails> {

    //region Defines

    public interface Delegate {
        void onSuccess(final DigitalizedCardDetails value);

        void onError(final String error);
    }

    private final Delegate mDelegate;

    //endregion

    //region Life Cycle

    public AsyncHelperCardDetails(@NonNull final Delegate delegate) {
        super();

        mDelegate = delegate;
    }

    //endregion


    //region MGAbstractAsyncHandler

    @Override
    public void onComplete(final AsyncResult<DigitalizedCardDetails> asyncResult) {
        if (asyncResult.isSuccessful()) {
            mDelegate.onSuccess(asyncResult.getResult());
        } else {
            mDelegate.onError(asyncResult.getErrorMessage());
        }
    }

    //endregion
}
