package com.sortedunderbelly.pardons;

import android.content.Context;
import android.widget.ArrayAdapter;

import java.text.DateFormat;
import java.util.List;

/**
 * Fragment that displays pardons that the authenticated user has sent.
 *
 * Created by max.ross on 8/9/15.
 */
public class SentPardonsFragment extends BasePardonsFragment {

    static final String SENT_PARDON_ACTION = "sent_pardon";

    @Override
    protected List<Pardon> getPardons() {
        return getStorage().getSentPardons();
    }

    @Override
    protected String getIntentFilterAction() {
        return SENT_PARDON_ACTION;
    }

    @Override
    protected ArrayAdapter<Pardon> newPardonArrayAdapter(
            Context context, int listItemResourceId, List<Pardon> pardons, DateFormat listItemDateFormat) {
        return new SentPardonArrayAdapter(context, listItemResourceId, pardons, listItemDateFormat);
    }
}
