package com.sortedunderbelly.pardons;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.sortedunderbelly.pardons.storage.InMemoryPardonStorage;
import com.sortedunderbelly.pardons.storage.PardonStorage;

import java.util.Date;
import java.util.List;


public class MainActivity extends FragmentActivity {

    private GoogleApiClientHelper apiClientHelper;

    private TextView receivedPardonsText;
    private TextView sentPardonsText;
    private PardonStorage storage;
    SlidingTabsBasicFragment tabsFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        apiClientHelper = new GoogleApiClientHelper(this);
        apiClientHelper.onCreate(savedInstanceState);

        setContentView(R.layout.pardons_home);
        storage = InMemoryPardonStorage.getInstance();
        receivedPardonsText = (TextView) findViewById(R.id.receivedPardonsValTextView);
        sentPardonsText = (TextView) findViewById(R.id.sentPardonsValTextView);
        Button sendPardonsButton = (Button) findViewById(R.id.sendPardonButton);
        sendPardonsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment newFragment = new SendPardonDialogFragment();
                newFragment.show(getSupportFragmentManager(), "What is this?");
            }
        });

        Button requestPardonsButton = (Button) findViewById(R.id.requestPardonButton);
        requestPardonsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment newFragment = new RequestPardonDialogFragment();
                newFragment.show(getSupportFragmentManager(), "What is this?");
            }
        });

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            tabsFragment = new SlidingTabsBasicFragment();
            transaction.replace(R.id.tabbed_fragment, tabsFragment);
            transaction.commit();
        }
        refresh();
    }

    @Override
    protected void onStart() {
        super.onStart();
        apiClientHelper.onStart();
    }

    @Override
    protected void onStop() {
        apiClientHelper.onStop();
        super.onStop();
    }

    private void refresh() {
        int totalReceivedPardons = calcPardonSum(storage.getReceivedPardons());
        receivedPardonsText.setText(Integer.valueOf(totalReceivedPardons).toString());

        int totalSentPardons = calcPardonSum(storage.getSentPardons());
        sentPardonsText.setText(Integer.valueOf(totalSentPardons).toString());
    }

    private int calcPardonSum(List<Pardons> pardons) {
        int total = 0;
        for (Pardons p : pardons) {
            total += p.getQuantity();
        }
        return total;
    }


    private String getAuthenticatedUsername() {
        return "me@sortedunderbelly.com";
    }

    private String getAuthenticatedDisplayName() { return "Me"; }

    public void sendPardonsToFriend(String recipient, String recipientDisplayName, int quantity, String reason) {
        Pardons pardons = new Pardons(getAuthenticatedUsername(), getAuthenticatedDisplayName(),
                recipient, recipientDisplayName, new Date(), quantity, reason);
        storage.addSentPardons(pardons);
        updateViews(pardons.getQuantity(), sentPardonsText,
                SentPardonsFragment.SENT_PARDONS_ACTION);
        Toast.makeText(getApplicationContext(), R.string.pardonsSentText, Toast.LENGTH_SHORT).show();
    }

    public void approvePardons(Pardons pardons) {
        storage.approvePardonsRequest(pardons);
        updateViews(-pardons.getQuantity(), /* stat not displayed */ null,
                PendingInboundRequestsPardonsFragment.PENDING_INBOUND_REQUESTS);
        updateViews(pardons.getQuantity(), sentPardonsText,
                SentPardonsFragment.SENT_PARDONS_ACTION);
        Toast.makeText(getApplicationContext(), R.string.acceptedRequestForPardonsText, Toast.LENGTH_SHORT).show();
    }

    public void denyPardons(Pardons pardons) {
        storage.denyPardonsRequest(pardons);
        updateViews(-pardons.getQuantity(), /* stat not displayed */ null,
                PendingInboundRequestsPardonsFragment.PENDING_INBOUND_REQUESTS);
        Toast.makeText(getApplicationContext(), R.string.deniedRequestForPardonsText, Toast.LENGTH_SHORT).show();
    }

    public void requestPardons(String recipient, String recipientDisplayName, int quantity,
                               String reason) {
        Pardons pardons = new Pardons(recipient, recipientDisplayName, getAuthenticatedUsername(),
                getAuthenticatedDisplayName(), new Date(), quantity, reason);
        // If approved, these pardons will come from your friend.
        storage.addPardonsRequest(pardons);
        updateViews(pardons.getQuantity(), /* stat not displayed */ null,
                PendingOutboundRequestsPardonsFragment.PENDING_OUTBOUND_REQUESTS);
        Toast.makeText(getApplicationContext(), R.string.pardonsRequestedText, Toast.LENGTH_SHORT).show();
    }

    public void retractRequestForPardons(Pardons pardons) {
        storage.removePardonsRequest(pardons);
        updateViews(-pardons.getQuantity(), /* stat not displayed */ null,
                PendingOutboundRequestsPardonsFragment.PENDING_OUTBOUND_REQUESTS);
        Toast.makeText(getApplicationContext(), R.string.pardonsRetractedText, Toast.LENGTH_SHORT).show();
    }

    public void receivePardonsFromFriend(Pardons pardons) {
        // not updating storage because this event is triggered by the sender of the pardon
        // making the storage change herself
        updateViews(pardons.getQuantity(), receivedPardonsText,
                ReceivedPardonsFragment.PARDONS_FROM_FRIENDS_ACTION);
    }

    private int textToInt(TextView textView) {
        return Integer.parseInt(textView.getText().toString());
    }

    public PardonStorage getStorage() {
        return storage;
    }

    private void updateViews(int pardonsDelta, @Nullable TextView textView, String intentAction) {
        if (textView != null) {
            int newPardonsTotal = textToInt(textView) + pardonsDelta;
            textView.setText(Integer.valueOf(newPardonsTotal).toString());
        }
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.sendBroadcast(new Intent(intentAction));
        lbm.sendBroadcast(new Intent(SlidingTabLayout.UPDATE_TAB_TITLES_INTENT_ACTION));
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.pardons_home, container, false);
        }
    }

    /**
     * Saves the resolution state.
     */
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        apiClientHelper.onSaveInstanceState(outState);
    }

    /**
     * Handles Google Play Services resolution callbacks.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        apiClientHelper.onActivityResult(requestCode, resultCode, data);
    }
}
