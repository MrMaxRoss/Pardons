package com.sortedunderbelly.pardons;

import android.content.Context;

import java.util.Date;
import java.util.List;

/**
 * Created by max.ross on 8/9/15.
 */
public class AccusationsByMeArrayAdapter extends BaseArrayAdapter<Accusation> {
    public AccusationsByMeArrayAdapter(
            Context context, int resource, List<Accusation> accusations) {
        super(context, resource, accusations);
    }

    @Override
    protected Date getDate(Accusation obj) {
        return obj.getDate();
    }

    @Override
    protected String getReason(Accusation obj) {
        return obj.getReason();
    }

    @Override
    protected String getAttribution(Accusation accusation) {
        return String.format(getContext().getString(R.string.accused_format),
                accusation.getAccusedDisplay());
    }
}
