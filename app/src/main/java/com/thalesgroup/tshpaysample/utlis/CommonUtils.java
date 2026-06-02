/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.utlis;

import android.os.Bundle;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.Map;

public final class CommonUtils {

    //region Defines

    private static final int ANIMATION_DURATION_MS = 500;
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public interface AnimationFinishHandler {
        void onAnimationFinished();
    }

    //endregion

    //region Public API

    public static String bytesToHex(final byte[] bytes) {
        final char[] hexChars = new char[bytes.length * 2];
        for (int loopChar = 0; loopChar < bytes.length; loopChar++) {
            final int loopByte = bytes[loopChar] & 0xFF;
            hexChars[loopChar * 2] = HEX_ARRAY[loopByte >>> 4];
            hexChars[loopChar * 2 + 1] = HEX_ARRAY[loopByte & 0x0F];
        }
        return new String(hexChars);
    }

    public static Animation getFadeInAnimation(final AnimationFinishHandler handler) {
        final Animation retValue = new AlphaAnimation(0, 1);
        retValue.setInterpolator(new DecelerateInterpolator(2.0f));
        retValue.setDuration(ANIMATION_DURATION_MS);

        setAnimationHandler(retValue, handler);

        return retValue;
    }

    public static Animation getFadeOutAnimation(final AnimationFinishHandler handler) {
        final Animation retValue = new AlphaAnimation(1, 0);
        retValue.setInterpolator(new AccelerateInterpolator(2.0f));
        retValue.setDuration(ANIMATION_DURATION_MS);

        setAnimationHandler(retValue, handler);

        return retValue;
    }

    /**
     * Converts Intent extras (Bundle) to a JSON string for troubleshooting.
     *
     * @param bundle Bundle to convert.
     * @return JSON string representation.
     */
    public static String bundleToJson(final Bundle bundle) {
        if (bundle == null) {
            return "{}";
        }

        final Map<String, Object> map = new HashMap<>();
        for (final String key : bundle.keySet()) {
            final Object value = bundle.get(key);

            // Handle nested bundles recursively
            if (value instanceof Bundle) {
                map.put(key, bundleToJson((Bundle) value));
            } else {
                map.put(key, value);
            }
        }

        return new GsonBuilder().setPrettyPrinting().create().toJson(map);
    }

    //endregion

    //region Private Helpers

    private static void setAnimationHandler(final Animation animation, final AnimationFinishHandler handler) {
        if (handler == null) {
            return;
        }

        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(final Animation animation) {
                // Ignore
            }

            @Override
            public void onAnimationEnd(final Animation animation) {
                handler.onAnimationFinished();
            }

            @Override
            public void onAnimationRepeat(final Animation animation) {
                // Ignore.
            }
        });
    }

    //endregion

}
