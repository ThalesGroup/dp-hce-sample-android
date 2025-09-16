/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;
import com.thalesgroup.tshpaysample.R;
import com.thalesgroup.tshpaysample.sdk.SdkHelper;
import com.thalesgroup.tshpaysample.sdk.push.ServerMessageInfo;
import com.thalesgroup.tshpaysample.ui.model.CardListAdapter;
import com.thalesgroup.tshpaysample.utlis.AppLoggerHelper;
import com.thalesgroup.tshpaysample.utlis.ZoomOutPageTransformer;

import java.util.List;

public class FragmentCardList extends AbstractFragment {

    //region Defines

    private CardListAdapter mCardListAdapter;

    private static final String TAG = FragmentCardList.class.getSimpleName();

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
        retValue.findViewById(R.id.fragment_card_list_secure_log_send).setOnClickListener(this::onButtonPressedDev);

        // Display dots as page indicator
        new TabLayoutMediator(retValue.findViewById(R.id.fragment_card_list_tab_layout), pager, (tab, position) -> {

        }).attach();


        SdkHelper.getInstance().getTshPaymentListener().getDefaultCardId().observe(getViewLifecycleOwner(), cardId -> {
            AppLoggerHelper.debug(TAG, "Default cardId has changed to: " + (cardId == null ? "null" : cardId) );
            // propagate it to the CardListAdapter
            onReloadData();
        });

        return retValue;
    }

    @Override
    public void onPushMsgProcessingResult(@Nullable final List<ServerMessageInfo> serverMessageInfoList,
                                          @Nullable final String error) {
        super.onPushMsgProcessingResult(serverMessageInfoList, error);

        // Here we received push message processing result and we can react to it.
        // We will only reload all data here, but we do some additional checks in PaySampleApp#onPushMsgProcessingResult

        onReloadData();
    }

    @Override
    public void onReloadData() {
        // Real application might react only to specific states to safe performance.
        AppLoggerHelper.info(TAG, "Reloading complete card list");
        mCardListAdapter.reloadCardList();
    }

    // Do not need to override onResume just to reload card list,
    // because we observe the default cardId changes and we will received the first value
    // as soon as the fragment is visible and we will reload from there.

//    @Override
//    public void onResume() {
//        super.onResume();
//
//        onReloadData();
//    }

    //endregion

    //region User Interface

    private void onButtonPressedAdd(final View sender) {
        getMainActivity().showFragment(new FragmentCardEnrollment(), true);
    }

    private void onButtonPressedDev(final View sender) {
        SdkHelper.getInstance().getTshSecureLogger().shareSecureLog(getMainActivity());
    }

    //endregion
}
