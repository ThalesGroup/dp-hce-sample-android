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

package com.thalesgroup.tshpaysample.utlis;

import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;

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
