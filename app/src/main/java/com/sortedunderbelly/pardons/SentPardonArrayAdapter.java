package com.sortedunderbelly.pardons;

import android.content.Context;

import java.text.DateFormat;
import java.util.List;

/**
 * Created by max.ross on 8/9/15.
 */
public class SentPardonArrayAdapter extends PardonArrayAdapter {

    public SentPardonArrayAdapter(
            Context context, int resource, List<Pardon> pardons, DateFormat dateFormat) {
        super(context, resource, pardons, dateFormat);
    }

    @Override
    protected String getPardonAttribution(Pardon pardon) {
        return pardon.getToDisplay();
    }
}
