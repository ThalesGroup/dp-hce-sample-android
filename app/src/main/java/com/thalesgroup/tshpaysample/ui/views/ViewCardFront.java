/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.ui.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.gemalto.mfs.mwsdk.dcm.DigitalizedCardDetails;
import com.thalesgroup.tshpaysample.R;
import com.thalesgroup.tshpaysample.sdk.helpers.AsyncHelperCardDetails;
import com.thalesgroup.tshpaysample.sdk.helpers.CardWrapper;
import com.thalesgroup.tshpaysample.utlis.AppLoggerHelper;

public class ViewCardFront extends FrameLayout {

    //region Defines

    private static final String TAG = ViewCardFront.class.getSimpleName();

    private final int[] mColors;

    //endregion

    //region Life Cycle

    public ViewCardFront(final Context context, final AttributeSet attrs) {
        super(context, attrs);

        // Load visual part and store elements.
        inflate(getContext(), R.layout.view_card_front, this);

        final TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.ViewCardFront);

        setType(attributes.getString(R.styleable.ViewCardFront_type));
        setExp(attributes.getString(R.styleable.ViewCardFront_exp));
        setPan(attributes.getString(R.styleable.ViewCardFront_pan));

        mColors = new int[]{
                attributes.getColor(R.styleable.ViewCardFront_gradientEnd, ContextCompat.getColor(context, R.color.card_front_default_end)),
                attributes.getColor(R.styleable.ViewCardFront_gradientStart, ContextCompat.getColor(context, R.color.card_front_default_start))};

        loadDefaultCardGraphics();

        attributes.recycle();
    }

    //endregion

    //region Public API

    public final void setType(final String value) {
        ((TextView) findViewById(R.id.view_card_front_type)).setText(value);
    }

    public final void setExp(final String value) {
        ((TextView) findViewById(R.id.view_card_front_exp)).setText(value);
    }

    public final void setPan(final String value) {
        ((TextView) findViewById(R.id.view_card_front_pan)).setText(value);
    }

    public void loadCardDetails(final CardWrapper cardWrapper) {
        // Load card art. It can be both async or sync. Depend if we already have image downloaded.
        cardWrapper.getCardArt(getContext(), (drawable, loading) -> {
            if (drawable != null) {
                setCardDrawable(drawable);
            } else {
                loadDefaultCardGraphics();
            }

            // Company logo and scheme will be visible only if we do not have final card graphics.
            findViewById(R.id.view_card_front_thales_logo).setVisibility(drawable == null ? VISIBLE : INVISIBLE);
            findViewById(R.id.view_card_front_type).setVisibility(drawable == null ? VISIBLE : INVISIBLE);
            // Display loading bar if we are downloading card art from backend.
            findViewById(R.id.view_card_front_progressbar).setVisibility(loading ? VISIBLE : GONE);
        });

        cardWrapper.getDigitalizedCardDetails(new AsyncHelperCardDetails.Delegate() {
            @Override
            public void onSuccess(final DigitalizedCardDetails value) {
                setExp(value.getPanExpiry());
                setPan("**** **** **** " + value.getLastFourDigits());
            }

            @Override
            public void onError(final String error) {
                AppLoggerHelper.error(TAG, error);
            }
        });
    }

    //endregion

    //region Private Helpres

    private void loadDefaultCardGraphics() {
        final GradientDrawable gradientDrawable = (GradientDrawable) ContextCompat.getDrawable(getContext(), R.drawable.rounded_rectangle_gradient);
        if (gradientDrawable != null) {
            gradientDrawable.mutate();
            gradientDrawable.setColors(mColors);
            setCardDrawable(gradientDrawable);
        }
    }

    private void setCardDrawable(final Drawable drawable) {
        findViewById(R.id.view_card_front_background).setBackground(drawable);
    }

    //endregion
}
