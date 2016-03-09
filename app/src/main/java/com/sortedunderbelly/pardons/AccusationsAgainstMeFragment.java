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

import static com.sortedunderbelly.pardons.Utils.simpleErrorDialog;

/**
 * Created by max.ross on 8/9/15.
 */
public class AccusationsAgainstMeFragment extends BaseAccusationFragment {

    public static List<Accusation> getAccusations(PardonStorage storage) {
        return storage.getAccusationsAgainstMe();
    }

    @Override
    protected List<Accusation> getList() {
        return getAccusations(getStorage());
    }

    @Override
    protected ArrayAdapter<Accusation> newArrayAdapter(
            Context context, int listItemResourceId, List<Accusation> accusation) {
        return new AccusationsAgainstMeArrayAdapter(context, listItemResourceId, accusation);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        final ListView listView = (ListView) view.findViewById(R.id.accusations_listview);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Accusation accusation = (Accusation) listView.getAdapter().getItem(position);
                if (accusation == null) {
                    simpleErrorDialog(getActivity(), getResources(), R.string.invalid_accusation_reference);
                    return;
                }

                // create a new AlertDialog
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                // set the AlertDialog's title
                builder.setTitle(getString(R.string.review_accusation_dialog_title));
                builder.setIcon(android.R.drawable.ic_dialog_alert);

                builder.setMessage(String.format(getString(R.string.review_accusation_against_me_dialog_message),
                        accusation.getAccuserDisplay()));

                // We map neutral to cancel because that appears on the left.
                builder.setNeutralButton(getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            // called when the "Cancel" Button is clicked
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel(); // dismiss the AlertDialog
                            }
                        }
                );

                // TODO(max.ross): Give people a way to deny accusations and explain why.
                // this is the 2nd button displayed
                builder.setNegativeButton(
                        getString(R.string.review_accusation_dialog_deny_button_label),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                getMainActivity().respondToAccusation(accusation, pardons);
                            }
                        }
                );

                // this is the 3rd button displayed
                builder.setPositiveButton(
                        getString(R.string.review_accusation_dialog_send_button_label),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // TODO(max.ross): Create pardons from the accusation
                                getMainActivity().respondToAccusation(accusation, null);
                            }
                        }
                );
                builder.create().show(); // display the AlertDialog
            }
        });
        return view;
    }
}
