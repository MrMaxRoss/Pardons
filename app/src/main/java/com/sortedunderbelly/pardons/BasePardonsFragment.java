package com.sortedunderbelly.pardons;

/**
 * Created by maxr on 2/26/16.
 */
public abstract class BasePardonsFragment extends BaseFragment<Pardons> {
    @Override
    protected int getLayoutListId() {
        return R.layout.pardons_list;
    }

    @Override
    protected int getLayoutListViewId() {
        return R.id.pardons_listview;
    }
}
