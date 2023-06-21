/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample;

import androidx.multidex.MultiDexApplication;

import com.thalesgroup.tshpaysample.sdk.SdkHelper;

public class PaySampleApp extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();

        SdkHelper.getInstance().initFromAppOnCreate(this);
    }
}
