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
 * Created by maxr on 2/26/16.
 */
public abstract class BaseArrayAdapter<T> extends ArrayAdapter<T> {
    static Calendar now = null;

    static Calendar getNow() {
        return now == null ? Calendar.getInstance() : now;
    }

    static void setNow(Calendar now) {
        BaseArrayAdapter.now = now;
    }

    static void clearNow() {
        now = null;
    }


    public BaseArrayAdapter(
            Context context, int resource, List<T> pardons) {
        super(context, resource, pardons);
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if (view == null) {
            LayoutInflater vi = LayoutInflater.from(getContext());
            view = vi.inflate(R.layout.list_item, null);
        }

        T obj = getItem(position);

        if (obj != null) {
            TextView pardonDateTime = (TextView) view.findViewById(R.id.pardonListItemDateTime);
            TextView pardonSource = (TextView) view.findViewById(R.id.pardonListItemSource);
            TextView pardonReason = (TextView) view.findViewById(R.id.pardonListItemReason);

            pardonDateTime.setText(getDateTimeString(getContext(), getDate(obj)));
            pardonSource.setText(getAttribution(obj));
            pardonReason.setText(getReason(obj));
        }
        if (position % 2 == 1) {
            view.setBackgroundColor(getContext().getResources().getColor(
                    R.color.pardon_list_item_background));
        } else {
            view.setBackgroundColor(getContext().getResources().getColor(
                    R.color.pardon_list_item_background_alt));
        }
        return view;
    }

    static String getDateTimeString(Context context, Date date) {
        Calendar thenCal = new GregorianCalendar();
        thenCal.setTime(date);
        Calendar nowCal = getNow();

        if (thenCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)) {
            // same year
            if (thenCal.get(Calendar.MONTH) == nowCal.get(Calendar.MONTH) &&
                    thenCal.get(Calendar.DAY_OF_MONTH) == nowCal.get(Calendar.DAY_OF_MONTH)) {
                // same month and day so just show the time
                return java.text.DateFormat.getTimeInstance(DateFormat.SHORT).format(date);
            } else {
                // different day or month so just show the date
                int flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_NO_YEAR;
                return DateUtils.formatDateTime(context, date.getTime(), flags);
            }
        } else {
            // different year so show the full date
            return java.text.DateFormat.getDateInstance(DateFormat.SHORT).format(date);
        }
    }

    protected abstract Date getDate(T obj);
    protected abstract String getReason(T obj);
    protected abstract String getAttribution(T obj);
}
