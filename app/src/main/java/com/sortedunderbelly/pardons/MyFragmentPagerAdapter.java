package com.sortedunderbelly.pardons;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.util.Log;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.sortedunderbelly.pardons.storage.PardonStorage;

/**
 * Created by max.ross on 8/9/15.
 */
public class MyFragmentPagerAdapter extends FragmentPagerAdapter {

    static final String LOG_TAG = "MyFragmentPagerAdapter";

    private final class FragmentData {
        final String tabTitle;
        int tabTitleQuantity;

        public FragmentData(int tabTitleResourceId, int tabTitleQuantity) {
            this.tabTitle = activity.getResources().getString(tabTitleResourceId);
            this.tabTitleQuantity = tabTitleQuantity;
        }
    }

    private final MainActivity activity;

    private final ImmutableMap<Class<? extends BasePardonsFragment>, FragmentData> fragmentData;
    private final ImmutableList<Class<? extends BasePardonsFragment>> fragmentClassList;


    public MyFragmentPagerAdapter(FragmentManager fm, MainActivity activity) {
        super (fm);
        this.activity = activity;
        PardonStorage storage = activity.getStorage();
        // TODO(max.ross): Stop calling size on the returned lists. This won't scale.
        // You'll need some sort of approximation provided by the storage layer.
        fragmentData = ImmutableMap.of(
                OutboundRequestedPardonsFragment.class,
                new FragmentData(R.string.outbound_requested_tab_title,
                        storage.getOutboundRequestedPardons().size()),
                InboundRequestedPardonsFragment.class,
                new FragmentData(R.string.inbound_requested_tab_title,
                        storage.getInboundRequestedPardons().size()),
                ReceivedPardonsFragment.class,
                new FragmentData(R.string.received_tab_title,
                        storage.getReceivedPardons().size()),
                SentPardonsFragment.class,
                new FragmentData(R.string.sent_tab_title,
                        storage.getSentPardons().size()));
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
        // very inefficent, right?
        return POSITION_NONE;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        FragmentData data = fragmentData.get(fragmentClassList.get(position));
        String rawTitle = data.tabTitle;
        if (data.tabTitleQuantity == 0) {
            Log.v(LOG_TAG, "quantity at position " + position + " is 0");
            return rawTitle;
        }
        Log.v(LOG_TAG, "quantity at position " + position + " is " + data.tabTitleQuantity);
        return String.format(rawTitle + " (%d)", data.tabTitleQuantity);
    }

    public void setTabTitleQuantity(Class<? extends BasePardonsFragment> aClass, int tabTitleQuantity) {
        fragmentData.get(aClass).tabTitleQuantity = tabTitleQuantity;
    }
}
