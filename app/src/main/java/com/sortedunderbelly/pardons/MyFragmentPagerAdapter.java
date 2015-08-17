package com.sortedunderbelly.pardons;

import android.content.Context;
import android.content.res.Resources;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.google.common.collect.ImmutableList;

import java.util.List;

/**
 * Created by max.ross on 8/9/15.
 */
public class MyFragmentPagerAdapter extends FragmentPagerAdapter {
    private final Context context;

    private final List<String> fragmentClassNames = ImmutableList.of(
            OutboundRequestedPardonsFragment.class.getName(),
            InboundRequestedPardonsFragment.class.getName(),
            ReceivedPardonsFragment.class.getName(),
            SentPardonsFragment.class.getName());

    private final List<String> pageTitles;

    public MyFragmentPagerAdapter(FragmentManager fm, Context context) {
        super (fm);
        this.context = context;
        Resources resources = context.getResources();
        pageTitles = ImmutableList.of(
                resources.getString(R.string.outbound_requested_tab_title),
                resources.getString(R.string.inbound_requested_tab_title),
                resources.getString(R.string.received_tab_title),
                resources.getString(R.string.sent_tab_title));
    }

    @Override
    public Fragment getItem(int position) {
        return Fragment.instantiate(context, fragmentClassNames.get(position));
    }

    @Override
    public int getCount() {
        return fragmentClassNames.size();
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return pageTitles.get(position);
    }
}
