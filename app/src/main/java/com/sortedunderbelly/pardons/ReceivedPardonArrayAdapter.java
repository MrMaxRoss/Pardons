package com.sortedunderbelly.pardons;

import android.content.Context;

import java.text.DateFormat;
import java.util.List;

/**
 * Created by max.ross on 8/9/15.
 */
public class ReceivedPardonArrayAdapter extends PardonArrayAdapter {

    public ReceivedPardonArrayAdapter(
            Context context, int resource, List<Pardon> pardons, DateFormat dateFormat) {
        super(context, resource, pardons, dateFormat);
    }

    @Override
    protected String getPardonAttribution(Pardon pardon) {
        return String.format(getContext().getString(R.string.received_pardon_attribution_format),
                pardon.getQuantity(),
                Utils.getPossiblyPluralPardonString(getContext().getResources(), pardon),
                pardon.getFromDisplay());
    }
}
