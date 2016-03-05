package com.sortedunderbelly.pardons;

import android.content.Context;

import java.util.Date;
import java.util.List;

/**
 * Created by max.ross on 8/9/15.
 */
public class AccusationsAgainstMeArrayAdapter extends BaseArrayAdapter<Accusation> {
    public AccusationsAgainstMeArrayAdapter(
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
        return String.format(getContext().getString(R.string.accusation_against_me_attribution_format),
                accusation.getAccuserDisplay());
    }
}
