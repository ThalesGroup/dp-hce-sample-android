/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.utlis;

import android.util.Log;

import com.thalesgroup.tshpaysample.BuildConfig;

public class AppLoggerHelper {
    public static void info(final String tag,
                            final String message) {
        if (BuildConfig.DEBUG) {
            Log.i(tag, message);
        }
    }

    public static void debug(final String tag,
                             final String message) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, message);
        }
    }

    public static void error(final String tag,
                             final String message) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, message);
        }
    }

    public static void exception(final String tag,
                                 final String message,
                                 final Exception exception) {
        if (BuildConfig.DEBUG) {
            Log.e(tag, message, exception);
        }
    }
}
