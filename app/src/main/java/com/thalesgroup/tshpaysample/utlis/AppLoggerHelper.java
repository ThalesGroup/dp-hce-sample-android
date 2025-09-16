/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.utlis;

import android.util.Log;

import com.thalesgroup.tshpaysample.BuildConfig;

public class AppLoggerHelper {

    public static void debug(final String tag,
                             final String message) {
        if (BuildConfig.LOG_LEVEL <= Log.DEBUG) {
            Log.d(tag, message);
        }
    }

    public static void info(final String tag,
                            final String message) {
        if (BuildConfig.LOG_LEVEL <= Log.INFO) {
            Log.i(tag, message);
        }
    }

    public static void warn(final String tag,
                             final String message) {
        if (BuildConfig.LOG_LEVEL <= Log.WARN) {
            Log.w(tag, message);
        }
    }

    public static void error(final String tag,
                             final String message) {
        if (BuildConfig.LOG_LEVEL <= Log.ERROR) {
            Log.e(tag, message);
        }
    }

    public static void exception(final String tag,
                                 final String message,
                                 final Exception exception) {
        if (BuildConfig.LOG_LEVEL <= Log.ERROR) {
            Log.e(tag, message, exception);
        }
    }

    public static void wtf(final String tag,
                                 final String message,
                                 final Exception exception) {
        if (BuildConfig.LOG_LEVEL <= Log.ASSERT) {
            Log.wtf(tag, message, exception);
        }
    }
}
