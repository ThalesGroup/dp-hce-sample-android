/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.thalesgroup.tshpaysample.R;

public class ViewProgress extends FrameLayout {

    //region Defines

    private TextView mTextCaption;

    //endregion

    //region Life Cycle

    public ViewProgress(@NonNull final Context context) {
        super(context);

        populateUI();
    }

    public ViewProgress(final Context context, final AttributeSet attrs) {
        super(context, attrs);

        populateUI();
    }

    //endregion

    //region Private Helpers

    private void populateUI() {
        // Load visual part and store elements.
        inflate(getContext(), R.layout.view_progress, this);

        mTextCaption = findViewById(R.id.view_progress_caption);
    }

    //endregion

    //region Public API

    public void updateCaption(final String caption) {
        mTextCaption.setText(caption);
    }

    //endregion

}
