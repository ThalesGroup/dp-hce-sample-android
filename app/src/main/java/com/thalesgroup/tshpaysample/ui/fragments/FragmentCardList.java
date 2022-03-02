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

package com.thalesgroup.tshpaysample.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;
import com.thalesgroup.tshpaysample.R;
import com.thalesgroup.tshpaysample.sdk.push.TshPushType;
import com.thalesgroup.tshpaysample.ui.model.CardListAdapter;
import com.thalesgroup.tshpaysample.utlis.ZoomOutPageTransformer;

public class FragmentCardList extends AbstractFragment {

    //region Defines

    private CardListAdapter mCardListAdapter;

    //endregion

    //region Life Cycle

    @Override
    public int getFragmentCaption() {
        return R.string.fragment_card_list_caption;
    }

    @Override
    public View onCreateView(final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        // Table data layer with list of available cards.
        mCardListAdapter = new CardListAdapter(getChildFragmentManager(), getLifecycle(), getContext());

        // Inflate the layout for this fragment
        final View retValue = inflater.inflate(R.layout.fragment_card_list, container, false);

        // Card list pager
        final ViewPager2 pager = retValue.findViewById(R.id.fragment_card_list_pager);
        pager.setOrientation(ViewPager2.ORIENTATION_HORIZONTAL);
        pager.setOffscreenPageLimit(3);
        pager.setPageTransformer(new ZoomOutPageTransformer());
        pager.setAdapter(mCardListAdapter);

        // Floating button to add new card.
        retValue.findViewById(R.id.fragment_card_list_add_button).setOnClickListener(this::onButtonPressedAdd);

        // Display dots as page indicator
        new TabLayoutMediator(retValue.findViewById(R.id.fragment_card_list_tab_layout), pager, (tab, position) -> {

        }).attach();

        return retValue;
    }

    @Override
    public void onPushReceived(@NonNull final TshPushType state,
                               @Nullable final String error) {
        super.onPushReceived(state, error);

        onReloadData();
    }

    @Override
    public void onReloadData() {
        // Real application might react only to specific states to safe performance.
        // It's not relevant in sample.
        mCardListAdapter.reloadCardList();
    }

    @Override
    public void onResume() {
        super.onResume();

        // UI is not handling notification in background.
        mCardListAdapter.reloadCardList();
    }

    //endregion

    //region User Interface

    private void onButtonPressedAdd(final View sender) {
        getMainActivity().showFragment(new FragmentCardEnrollment(), true);
    }

    //endregion
}
