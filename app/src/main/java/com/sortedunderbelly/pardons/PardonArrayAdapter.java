package com.sortedunderbelly.pardons;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.List;

/**
 * Created by max.ross on 8/9/15.
 */
public abstract class PardonArrayAdapter extends ArrayAdapter<Pardon> {
    private final DateFormat dateFormat;

    public PardonArrayAdapter(
            Context context, int resource, List<Pardon> pardons, DateFormat dateFormat) {
        super(context, resource, pardons);
        this.dateFormat = dateFormat;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;

        if (view == null) {
            LayoutInflater vi = LayoutInflater.from(getContext());
            view = vi.inflate(R.layout.list_item, null);
        }

        Pardon pardon = getItem(position);

        if (pardon != null) {
            TextView pardonDateAndQuantity = (TextView) view.findViewById(
                    R.id.pardonDateAndQuantityListItem);
            TextView pardonSource = (TextView) view.findViewById(R.id.pardonSourceListItem);
            TextView pardonReason = (TextView) view.findViewById(R.id.pardonReasonListItem);

            pardonDateAndQuantity.setText(String.format("%s pardon%s on %s",
                    pardon.getQuantity(),
                    pardon.getQuantity() > 1 ? "s" : "",
                    dateFormat.format(pardon.getDate())));
            pardonSource.setText(getPardonAttribution(pardon));
            pardonReason.setText(pardon.getReason());
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

    protected abstract String getPardonAttribution(Pardon pardon);

}
