/*
 * Copyright © 2021-2022 THALES. All rights reserved.
 */

package com.thalesgroup.tshpaysample.ui.model;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.gemalto.mfs.mwsdk.dcm.DigitalizedCardManager;
import com.gemalto.mfs.mwsdk.dcm.PaymentType;
import com.gemalto.mfs.mwsdk.utils.async.AsyncResult;
import com.thalesgroup.tshpaysample.sdk.helpers.CardListHelper;
import com.thalesgroup.tshpaysample.sdk.helpers.CardWrapper;
import com.thalesgroup.tshpaysample.ui.fragments.FragmentCardPage;
import com.thalesgroup.tshpaysample.utlis.AppLoggerHelper;

import java.util.ArrayList;
import java.util.List;

public class CardListAdapter extends FragmentStateAdapter {

    //region Defines

    private static final String TAG = CardListAdapter.class.getSimpleName();

    private final List<CardWrapper> mCardList = new ArrayList<>();
    private final Context mContext;
    private final FragmentManager mFragmentManager;

    //endregion

    //region FragmentStateAdapter

    public CardListAdapter(@NonNull final FragmentManager fragmentManager,
                           @NonNull final Lifecycle lifecycle,
                           @NonNull final Context context) {
        super(fragmentManager, lifecycle);

        mFragmentManager = fragmentManager;
        mContext = context;
    }

    @NonNull
    @Override
    public Fragment createFragment(final int position) {
        final Bundle arguments = new Bundle();
        arguments.putString(FragmentCardPage.ARGUMENT_CARD_ID, mCardList.get(position).getCardId());

        final FragmentCardPage retValue = new FragmentCardPage();
        retValue.setArguments(arguments);
        return retValue;
    }

    @Override
    public int getItemCount() {
        return mCardList.size();
    }

    @Override
    public long getItemId(final int position) {
        return mCardList.get(position).getCardId().hashCode();
    }

    @Override
    public boolean containsItem(final long itemId) {
        for (final CardWrapper loopCard : mCardList) {
            if (loopCard.getCardId().hashCode() == itemId) {
                return true;
            }
        }
        return false;
    }

    //endregion

    //region Public API


    public void reloadCardList() {
        new CardListHelper(mContext, new CardListHelper.Delegate() {
            @Override
            public void onSuccess(final List<CardWrapper> cardWrappers) {
                mCardList.clear();
                mCardList.addAll(cardWrappers);

                checkAndSetDefaultCard();
                notifyPages();
            }

            @Override
            public void onError(final String error) {
                AppLoggerHelper.error(TAG, error);
                notifyPages();
            }
        }).getAllCards();
    }


    /***
     * Checks if there is a default card already set and if not it looks for the the first active card and sets it as default.
     * This makes sure that a default card is always set when possible.
     */
    private void checkAndSetDefaultCard(){

        final AsyncResult<String> defaultCardResult = DigitalizedCardManager.getDefault(PaymentType.CONTACTLESS, null).waitToComplete();

        if(defaultCardResult.isSuccessful()) {
            final String defaultCardId = defaultCardResult.getResult();

            if(defaultCardId == null || defaultCardId.isEmpty()){
                AppLoggerHelper.debug(TAG, "No default card found. Look for a card to set as default");

                for(final CardWrapper loopCard : mCardList){
                    if(loopCard.isActive()){
                        loopCard.setDefault(null);
                        break;
                    }
                }
            }
        } else {
            AppLoggerHelper.error(TAG, "Failed to get the default card: " + defaultCardResult.getErrorMessage());
        }

    }

    //endregion

    //region Private Helpers

    private void notifyPages() {
        // For simplification we will reload entire list, but we might want to load just some part.
        notifyDataSetChanged();

        // There is no easy way to update individual fragments with new FragmentStateAdapter.
        // Update them manually so we do not have to overcomplicate the sample app.
        for (final Fragment loopFragment : mFragmentManager.getFragments()) {
            if (loopFragment instanceof FragmentCardPage) {
                final FragmentCardPage pageToNotify = (FragmentCardPage) loopFragment;
                pageToNotify.updateState();
            }
        }
    }


    //endregion

}
