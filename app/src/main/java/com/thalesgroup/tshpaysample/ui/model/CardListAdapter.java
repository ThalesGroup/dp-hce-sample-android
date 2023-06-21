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

                // For simplification we will set default card to first enrolled card.
                checkDefaultCard();
                checkReplenishment();
                notifyPages();
            }

            @Override
            public void onError(final String error) {
                AppLoggerHelper.error(TAG, error);
                notifyPages();
            }
        }).getAllCards();
    }

    private void checkReplenishment() {
        for (final CardWrapper loopCard : mCardList) {
            loopCard.replenishKeysIfNeeded(false);
        }
    }

    private void checkDefaultCard() {
        final int cardCount = getItemCount();
        final int[] cardProcessed = {0};
        final boolean[] foundDefault = {false};

        for (int loopCardIndex = 0; loopCardIndex < cardCount; loopCardIndex++) {
            final CardWrapper loopCard = mCardList.get(loopCardIndex);
            loopCard.isDefault((value, message) -> {
                if (value) {
                    foundDefault[0] = true;
                } else {
                    ++cardProcessed[0];
                    if (cardProcessed[0] == cardCount && !foundDefault[0]) {
                        setDefaultCard(loopCard);
                    }
                }
            });
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

    private void setDefaultCard(final CardWrapper card) {
        card.setDefault((value, message) -> {
            if (value) {
                notifyPages();
            } else if (message != null) {
                AppLoggerHelper.error(TAG, message);
            }
        });
    }

    //endregion

}
