package com.sortedunderbelly.pardons;

import android.content.Context;

import java.util.List;

/**
 * Created by max.ross on 8/9/15.
 */
public class PendingPardonsArrayAdapter extends PardonsArrayAdapter {

    public PendingPardonsArrayAdapter(Context context, int resource, List<Pardons> pardons) {
        super(context, resource, pardons);
    }

    @Override
    protected String getPardonAttribution(Pardons pardons) {
        return String.format(getContext().getString(R.string.pending_pardons_attribution_format),
                pardons.getToDisplay(),
                pardons.getQuantity(),
                Utils.getPossiblyPluralPardonString(getContext().getResources(), pardons));
    }
}
