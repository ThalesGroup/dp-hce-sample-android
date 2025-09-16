/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.sdk.init;

public class TshInitState {
    private final TshInitStateEnum mState;
    private final String mError;

    public TshInitState(final TshInitStateEnum state) {
        mState = state;
        mError = null;
    }

    public TshInitState(final TshInitStateEnum state, final String error) {
        mState = state;
        mError = error;
    }

    public TshInitStateEnum getState() {
        return mState;
    }

    public String getError() {
        return mError;
    }
}


