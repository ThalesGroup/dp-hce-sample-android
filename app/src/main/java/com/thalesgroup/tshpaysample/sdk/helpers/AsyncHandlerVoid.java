/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.sdk.helpers;

import androidx.annotation.NonNull;

import com.gemalto.mfs.mwsdk.utils.async.AbstractAsyncHandler;
import com.gemalto.mfs.mwsdk.utils.async.AsyncResult;

public class AsyncHandlerVoid extends AbstractAsyncHandler<Void> {

    //region Defines

    public interface Delegate {
        void onSuccess();
        void onError(final String error);
    }

    private final Delegate mDelegate;

    //endregion

    //region Life Cycle

    public AsyncHandlerVoid(@NonNull final Delegate delegate) {
        super();

        mDelegate = delegate;
    }

    //endregion


    //region AbstractAsyncHandler

    @Override
    public void onComplete(final AsyncResult<Void> asyncResult) {
        if (asyncResult.isSuccessful()) {
            mDelegate.onSuccess();
        } else {
            mDelegate.onError(asyncResult.getErrorMessage());
        }
    }

    //endregion
}


