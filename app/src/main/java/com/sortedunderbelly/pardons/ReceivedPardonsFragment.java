package com.sortedunderbelly.pardons;

import android.content.Context;
import android.widget.ArrayAdapter;

import com.sortedunderbelly.pardons.storage.PardonStorage;

import java.util.List;

/**
 * Created by max.ross on 8/9/15.
 */
public class ReceivedPardonsFragment extends BasePardonsFragment {

    public static List<Pardons> getPardons(PardonStorage storage) {
        return storage.getReceivedPardons();
    }

    @Override
    protected List<Pardons> getList() {
        return getPardons(getStorage());
    }

    @Override
    protected ArrayAdapter<Pardons> newArrayAdapter(
            Context context, int listItemResourceId, List<Pardons> pardons) {
        return new ReceivedPardonsArrayAdapter(context, listItemResourceId, pardons);
    }
}
