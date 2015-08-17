package com.sortedunderbelly.pardons;

import android.content.Context;
import android.widget.ArrayAdapter;

import java.text.DateFormat;
import java.util.List;

/**
 * Created by max.ross on 8/9/15.
 */
public class ReceivedPardonsFragment extends BasePardonsFragment {
    static final String RECEIVED_PARDON_ACTION = "received_pardon";

    @Override
    protected List<Pardon> getPardons() {
        return getStorage().getReceivedPardons();
    }

    @Override
    protected String getIntentFilterAction() {
        return RECEIVED_PARDON_ACTION;
    }

    @Override
    protected ArrayAdapter<Pardon> newPardonArrayAdapter(
            Context context, int listItemResourceId, List<Pardon> pardons, DateFormat listItemDateFormat) {
        return new ReceivedPardonArrayAdapter(context, listItemResourceId, pardons, listItemDateFormat);
    }
}
