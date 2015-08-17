package com.sortedunderbelly.pardons;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
    private TextView outboundRequestedPardonsText;
    private TextView inboundRequestedPardonsText;
    private TextView sentPardonsText;
    private PardonStorage storage;
    SlidingTabsBasicFragment tabsFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        apiClientHelper = new GoogleApiClientHelper(this);
        apiClientHelper.onCreate(savedInstanceState);

        setContentView(R.layout.pardons_home);
        storage = new InMemoryPardonStorage();
        receivedPardonsText = (TextView) findViewById(R.id.receivedPardonsValTextView);
        outboundRequestedPardonsText = (TextView) findViewById(R.id.outboundRequestedPardonsValTextView);
        inboundRequestedPardonsText = (TextView) findViewById(R.id.inboundRequestedPardonsValTextView);
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

        int totalOutboundRequestedPardons = calcPardonSum(storage.getOutboundRequestedPardons());
        outboundRequestedPardonsText.setText(Integer.valueOf(totalOutboundRequestedPardons).toString());

        int totalInboundRequestedPardons = calcPardonSum(storage.getInboundRequestedPardons());
        inboundRequestedPardonsText.setText(Integer.valueOf(totalInboundRequestedPardons).toString());
    }

    private int calcPardonSum(List<Pardon> pardons) {
        int total = 0;
        for (Pardon p : pardons) {
            total += p.getQuantity();
        }
        return total;
    }


    private String getAuthenticatedUsername() {
        return "me@sortedunderbelly.com";
    }

    private String getAuthenticatedDisplayName() { return "Me"; }

    public void sendPardon(String recipient, String recipientDisplayName, int quantity, String reason) {
        Pardon pardon = new Pardon(getAuthenticatedUsername(), getAuthenticatedDisplayName(),
                recipient, recipientDisplayName, new Date(), quantity, reason);
        storage.addSentPardon(pardon);
        onSentPardon(pardon);
        Toast.makeText(getApplicationContext(), R.string.pardonSentText,
                Toast.LENGTH_SHORT).show();
    }

    public void requestPardon(String recipient, String recipientDisplayName, int quantity, String reason) {
        Pardon pardon = new Pardon(recipient, recipientDisplayName, getAuthenticatedUsername(), getAuthenticatedDisplayName(),
                new Date(), quantity, reason);
        storage.addRequestedPardon(pardon);
        onRequestedPardon(pardon);
        Toast.makeText(getApplicationContext(), R.string.pardonRequestedText,
                Toast.LENGTH_SHORT).show();
    }

    private void receivePardon(String id, String sender, String senderDisplayName, Date date,
                               int quantity, String reason) {
        Pardon pardon = new Pardon(id, sender, senderDisplayName, getAuthenticatedUsername(),
                getAuthenticatedDisplayName(), date, quantity, reason);
        storage.addReceivedPardon(pardon);
        onReceivedPardon(pardon);
    }

    public void retractPardon(Pardon pardon) {
        storage.deleteRequestedPardon(pardon);
        onRetractedPardon(pardon);
    }

    public void grantPardon(Pardon pardon) {
        storage.grantPardon(pardon);
        onGrantedPardon(pardon);
    }

    private int textToInt(TextView textView) {
        return Integer.parseInt(textView.getText().toString());
    }

    public PardonStorage getStorage() {
        return storage;
    }

    private void updateViews(int pardonsDelta, TextView textView, String intentAction) {
        int newPardonsTotal = textToInt(textView) + pardonsDelta;
        textView.setText(Integer.valueOf(newPardonsTotal).toString());
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.sendBroadcast(new Intent(intentAction));
    }

    public void onSentPardon(Pardon newPardon) {
        updateViews(newPardon.getQuantity(), sentPardonsText,
                SentPardonsFragment.SENT_PARDON_ACTION);
    }

    public void onReceivedPardon(Pardon newPardon) {
        updateViews(newPardon.getQuantity(), receivedPardonsText,
                ReceivedPardonsFragment.RECEIVED_PARDON_ACTION);
    }

    public void onRequestedPardon(Pardon newPardon) {
        updateViews(newPardon.getQuantity(), outboundRequestedPardonsText,
                OutboundRequestedPardonsFragment.OUTBOUND_REQUESTED_PARDON_ACTION);
    }

    public void onRetractedPardon(Pardon pardon) {
        updateViews(-pardon.getQuantity(), outboundRequestedPardonsText,
                OutboundRequestedPardonsFragment.OUTBOUND_REQUESTED_PARDON_ACTION);
    }

    public void onGrantedPardon(Pardon pardon) {
        updateViews(-pardon.getQuantity(), inboundRequestedPardonsText,
                InboundRequestedPardonsFragment.INBOUND_REQUESTED_PARDON_ACTION);
        updateViews(pardon.getQuantity(), sentPardonsText,
                SentPardonsFragment.SENT_PARDON_ACTION);
    }

    public SlidingTabsBasicFragment getTabsFragment() {
        return tabsFragment;
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
