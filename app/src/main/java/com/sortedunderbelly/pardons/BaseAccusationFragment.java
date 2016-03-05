package com.sortedunderbelly.pardons;

/**
 * Created by maxr on 2/26/16.
 */
public abstract class BaseAccusationFragment extends BaseFragment<Accusation> {
    @Override
    protected int getLayoutListId() {
        return R.layout.accusations_list;
    }

    @Override
    protected int getLayoutListViewId() {
        return R.id.accusations_listview;
    }
}
