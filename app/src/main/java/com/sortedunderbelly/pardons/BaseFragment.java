package com.sortedunderbelly.pardons;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.sortedunderbelly.pardons.storage.PardonStorage;

import java.util.List;

/**
 * Created by max.ross on 8/9/15.
 */
public abstract class BaseFragment<T> extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(getLayoutListId(), container, false);
        ListView listView = (ListView) v.findViewById(getLayoutListViewId());

        final ArrayAdapter<T> adapter = newArrayAdapter(
                v.getContext(), R.layout.list_item, getList());

        // Assign adapter to ListView
        listView.setAdapter(adapter);
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(v.getContext());
        IntentFilter filter = new IntentFilter(getClass().getName());
        class MyBroadcastReceiver extends BroadcastReceiver {
            @Override
            public void onReceive(Context context, Intent intent) {
                adapter.notifyDataSetChanged();
            }
        }
        lbm.registerReceiver(new MyBroadcastReceiver(), filter);
        return v;
    }

    protected abstract ArrayAdapter<T> newArrayAdapter(
            Context context, int list_item, List<T> list);

    protected abstract List<T> getList();

    protected abstract int getLayoutListId();
    protected abstract int getLayoutListViewId();

    protected PardonStorage getStorage() {
        return getMainActivity().getStorage();
    }

    protected MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }
}
