package com.sortedunderbelly.pardons;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.sortedunderbelly.pardons.storage.PardonStorage;

import java.util.concurrent.Callable;

/**
 * Created by max.ross on 8/9/15.
 */
public class MyFragmentPagerAdapter extends FragmentPagerAdapter {

    static final String LOG_TAG = "MyFragmentPagerAdapter";

    private final class FragmentData {
        final String tabTitle;
        final Callable<Integer> tabTitleQuantityCallable;

        public FragmentData(int tabTitleResourceId, Callable<Integer> tabTitleQuantityCallable) {
            this.tabTitle = activity.getResources().getString(tabTitleResourceId);
            this.tabTitleQuantityCallable = tabTitleQuantityCallable;
        }
    }

    private final MainActivity activity;

    private final ImmutableMap<Class<? extends BasePardonsFragment>, FragmentData> fragmentData;
    private final ImmutableList<Class<? extends BasePardonsFragment>> fragmentClassList;


    public MyFragmentPagerAdapter(FragmentManager fm, MainActivity activity) {
        super (fm);
        this.activity = activity;
        final PardonStorage storage = activity.getStorage();
        // TODO(max.ross): Stop calling size on the returned lists. This won't scale.
        // You'll need some sort of approximation provided by the storage layer.
        fragmentData = ImmutableMap.of(
                SentPardonsFragment.class,
                new FragmentData(R.string.send_tab_title,
                        new Callable<Integer>() {
                            @Override
                            public Integer call() throws Exception {
                                return storage.getSentPardons().size();
                            }
                        }),
                ReceivedPardonsFragment.class,
                new FragmentData(R.string.received_tab_title,
                        new Callable<Integer>() {
                            @Override
                            public Integer call() throws Exception {
                                return storage.getReceivedPardons().size();
                            }
                        }),
                InboundRequestedPardonsFragment.class,
                new FragmentData(R.string.inbound_requested_tab_title,
                        new Callable<Integer>() {
                            @Override
                            public Integer call() throws Exception {
                                return storage.getInboundRequestedPardons().size();
                            }
                        }),
                OutboundRequestedPardonsFragment.class,
                new FragmentData(R.string.outbound_requested_tab_title,
                        new Callable<Integer>() {
                            @Override
                            public Integer call() throws Exception {
                                return storage.getOutboundRequestedPardons().size();
                            }
                        }));
        fragmentClassList = ImmutableList.copyOf(fragmentData.keySet());
    }

    @Override
    public Fragment getItem(int position) {
        return Fragment.instantiate(activity, fragmentClassList.get(position).getName());
    }

    @Override
    public int getCount() {
        return fragmentClassList.size();
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_UNCHANGED;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        FragmentData data = fragmentData.get(fragmentClassList.get(position));
        String rawTitle = data.tabTitle;
        int tabTitleQuantity;
        try {
            tabTitleQuantity = data.tabTitleQuantityCallable.call();
        } catch (Exception e) {
            Throwables.propagateIfPossible(e);
            throw new RuntimeException(e);
        }
        if (tabTitleQuantity == 0) {
            return rawTitle;
        }
        return String.format(rawTitle + " (%d)", tabTitleQuantity);
    }
}
