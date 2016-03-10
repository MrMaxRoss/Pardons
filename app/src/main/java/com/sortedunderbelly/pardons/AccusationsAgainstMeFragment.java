package com.sortedunderbelly.pardons;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.sortedunderbelly.pardons.storage.PardonStorage;

import java.util.Date;
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

                DialogFragment newFragment = new NewPardonDialogFragment();
                Bundle args = new Bundle();
                args.putSerializable(NewPardonDialogFragment.ACCUSATION, accusation);
                newFragment.setArguments(args);
                newFragment.show(getFragmentManager(), "What is this?");

//                builder.setMessage(String.format(getString(R.string.review_accusation_against_me_dialog_message),
//                        accusation.getAccuserDisplay()));

            }
        });
        return view;
    }

    Pardons createPardonsFromAccusation(Accusation accusation, int numPardons, String reason) {
        return new Pardons(accusation.getAccused(),
                accusation.getAccusedDisplay(),
                accusation.getAccuser(), accusation.getAccuserDisplay(),
                new Date(), numPardons, reason);
    }
}
