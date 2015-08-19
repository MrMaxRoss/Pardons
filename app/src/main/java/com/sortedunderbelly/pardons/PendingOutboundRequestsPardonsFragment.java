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

import com.sortedunderbelly.pardons.storage.PardonStorage;

import java.util.List;

import static com.sortedunderbelly.pardons.Utils.getPossiblyPluralPardonString;
import static com.sortedunderbelly.pardons.Utils.simpleErrorDialog;

/**
 * Created by max.ross on 8/9/15.
 */
public class PendingOutboundRequestsPardonsFragment extends BasePardonsFragment {
    static final String PENDING_OUTBOUND_REQUESTS = "pending_outbound_requests";

    public static List<Pardons> getPardons(PardonStorage storage) {
        return storage.getPendingOutboundPardonsRequests();
    }

    @Override
    protected List<Pardons> getPardons() {
        return getPardons(getStorage());
    }

    @Override
    protected String getIntentFilterAction() {
        return PENDING_OUTBOUND_REQUESTS;
    }

    @Override
    protected ArrayAdapter<Pardons> newPardonArrayAdapter(
            Context context, int listItemResourceId, List<Pardons> pardons) {
        // We use the Received adapter because it's the exact same display
        return new ReceivedPardonsArrayAdapter(context, listItemResourceId, pardons);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        final ListView listView = (ListView) view.findViewById(R.id.pardons_listview);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Pardons pardons = (Pardons) listView.getAdapter().getItem(position);
                if (pardons == null) {
                    simpleErrorDialog(getActivity(), getResources(), R.string.invalid_pardon_reference);
                    return;
                }

                // create a new AlertDialog
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                // set the AlertDialog's title
                builder.setTitle(getString(R.string.retract_request_dialog_title));
                builder.setIcon(android.R.drawable.ic_dialog_alert);
                builder.setMessage(String.format(getString(R.string.retract_request_dialog_message),
                        pardons.getQuantity(),
                        getPossiblyPluralPardonString(getResources(), pardons),
                        pardons.getFromDisplay()));

                // set the AlertDialog's negative Button
                builder.setNegativeButton(getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            // called when the "Cancel" Button is clicked
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel(); // dismiss the AlertDialog
                            }
                        }
                );
                // set the AlertDialog's positive Button
                builder.setPositiveButton(
                        getString(R.string.retract_request_dialog_positive_button_label),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                getMainActivity().retractRequestForPardons(pardons);
                            }
                        }
                );
                builder.create().show(); // display the AlertDialog
            }
        });
        return view;
    }
}
