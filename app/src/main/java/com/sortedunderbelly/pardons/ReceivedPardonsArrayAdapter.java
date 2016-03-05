package com.sortedunderbelly.pardons;

import android.content.Context;

import java.util.List;

/**
 * Created by max.ross on 8/9/15.
 */
public class ReceivedPardonsArrayAdapter extends PardonsArrayAdapter {

    public ReceivedPardonsArrayAdapter(Context context, int resource, List<Pardons> pardons) {
        super(context, resource, pardons);
    }

    @Override
    protected String getAttribution(Pardons pardons) {
        return String.format(getContext().getString(R.string.received_pardons_attribution_format),
                pardons.getQuantity(),
                Utils.getPossiblyPluralPardonString(getContext().getResources(), pardons),
                pardons.getFromDisplay());
    }
}
