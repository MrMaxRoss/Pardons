package com.sortedunderbelly.pardons;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.text.DateFormat;
import java.util.List;

import static com.sortedunderbelly.pardons.Utils.getPossiblyPluralPardonString;
import static com.sortedunderbelly.pardons.Utils.simpleErrorDialog;

/**
 * Created by max.ross on 8/9/15.
 */
public class InboundRequestedPardonsFragment extends BasePardonsFragment {
    static final String INBOUND_REQUESTED_PARDON_ACTION = "inbound_requested_pardon";

    @Override
    protected List<Pardon> getPardons() {
        return getStorage().getInboundRequestedPardons();
    }

    @Override
    protected String getIntentFilterAction() {
        return INBOUND_REQUESTED_PARDON_ACTION;
    }

    @Override
    protected ArrayAdapter<Pardon> newPardonArrayAdapter(
            Context context, int listItemResourceId, List<Pardon> pardons, DateFormat listItemDateFormat) {
        // We use the Sent adapter because it's the exact same display
        return new SentPardonArrayAdapter(context, listItemResourceId, pardons, listItemDateFormat);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        final ListView listView = (ListView) view.findViewById(R.id.pardons_listview);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Pardon pardon = (Pardon) listView.getAdapter().getItem(position);
                if (pardon == null) {
                    simpleErrorDialog(getActivity(), getResources(), R.string.invalid_pardon_reference);
                    return;
                }

                // create a new AlertDialog
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                // set the AlertDialog's title
                builder.setTitle(getString(R.string.review_request_dialog_title));
                builder.setIcon(android.R.drawable.ic_dialog_alert);

                builder.setMessage(String.format(getString(R.string.grant_request_dialog_message),
                        pardon.getToDisplay(), pardon.getQuantity(),
                        getPossiblyPluralPardonString(getResources(), pardon)));

                // We map neutral to cancel because that appears on the left.
                builder.setNeutralButton(getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            // called when the "Cancel" Button is clicked
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel(); // dismiss the AlertDialog
                            }
                        }
                );

                // this is the 2nd button displayed
                builder.setNegativeButton(
                        getString(R.string.review_request_dialog_deny_button_label),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                getMainActivity().denyPardon(pardon);
                            }
                        }
                );

                // this is the 3rd button displayed
                builder.setPositiveButton(
                        getString(R.string.review_request_dialog_grant_button_label),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                getMainActivity().grantPardon(pardon);
                            }
                        }
                );
                builder.create().show(); // display the AlertDialog
            }
        });
        return view;
    }
}
