package com.sortedunderbelly.pardons;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Created by max.ross on 8/9/15.
 */
public abstract class PardonsArrayAdapter extends BaseArrayAdapter<Pardons> {
    public PardonsArrayAdapter(
            Context context, int resource, List<Pardons> pardons) {
        super(context, resource, pardons);
    }

    @Override
    protected Date getDate(Pardons obj) {
        return obj.getDate();
    }

    @Override
    protected String getReason(Pardons obj) {
        return obj.getReason();
    }
}
