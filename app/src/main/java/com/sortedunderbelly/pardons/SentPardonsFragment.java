package com.sortedunderbelly.pardons;

import android.content.Context;
import android.widget.ArrayAdapter;

import com.sortedunderbelly.pardons.storage.PardonStorage;

import java.util.List;

/**
 * Fragment that displays pardons that the authenticated user has sent.
 *
 * Created by max.ross on 8/9/15.
 */
public class SentPardonsFragment extends BasePardonsFragment {

    public static List<Pardons> getPardons(PardonStorage storage) {
        return storage.getSentPardons();
    }

    @Override
    protected List<Pardons> getList() {
        return getPardons(getStorage());
    }

    @Override
    protected ArrayAdapter<Pardons> newArrayAdapter(
            Context context, int listItemResourceId, List<Pardons> pardons) {
        return new SentPardonsArrayAdapter(context, listItemResourceId, pardons);
    }
}
