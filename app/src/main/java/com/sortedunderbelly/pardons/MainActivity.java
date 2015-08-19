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

import com.sortedunderbelly.pardons.storage.FirebasePardonStorage;
import com.sortedunderbelly.pardons.storage.PardonStorage;

import java.util.Date;
import java.util.List;


public class MainActivity extends FragmentActivity implements PardonStorage.PardonsUIListener {

    private static PardonStorage storage;

    private GoogleApiClientHelper apiClientHelper;

    private TextView receivedPardonsText;
    private TextView sentPardonsText;
    SlidingTabsBasicFragment tabsFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        apiClientHelper = new GoogleApiClientHelper(this);
        apiClientHelper.onCreate(savedInstanceState);

        setContentView(R.layout.pardons_home);
        if (storage == null) {
            // this seems like a bad idea, but how do I keep from reiniitializing Firebase
            // every time a new activity is created?
            storage = new FirebasePardonStorage(this, this);
        }
//        storage = InMemoryPardonStorage.get();
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

    @Override
    public void onAddSentPardons(Pardons pardons) {
        updateViews(pardons.getQuantity(), sentPardonsText,
                SentPardonsFragment.class);
    }

    @Override
    public void onApprovePardonsRequest(Pardons pardonsRequest) {
        sendUpdate(PendingInboundRequestsPardonsFragment.class);
        updateViews(pardonsRequest.getQuantity(), sentPardonsText,
                SentPardonsFragment.class);
    }

    public void approvePardons(Pardons pardons) {
        storage.approvePardonsRequest(pardons, this);
        Toast.makeText(getApplicationContext(), R.string.acceptedRequestForPardonsText, Toast.LENGTH_SHORT).show();
    }

    public void sendPardonsToFriend(String recipient, String recipientDisplayName, int quantity, String reason) {
        Pardons pardons = new Pardons(getAuthenticatedUsername(), getAuthenticatedDisplayName(),
                recipient, recipientDisplayName, new Date(), quantity, reason);
        storage.addSentPardons(pardons, this);
        Toast.makeText(getApplicationContext(), R.string.pardonsSentText, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDenyPardonsRequest(Pardons pardonsRequest) {
        sendUpdate(DeniedInboundRequestsPardonsFragment.class);
    }

    public void denyPardons(Pardons pardons) {
        storage.denyPardonsRequest(pardons, this);
        Toast.makeText(getApplicationContext(), R.string.deniedRequestForPardonsText, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAddPardonsRequest(Pardons pardons) {
        sendUpdate(PendingOutboundRequestsPardonsFragment.class);
    }

    public void requestPardons(String recipient, String recipientDisplayName, int quantity,
                               String reason) {
        Pardons pardons = new Pardons(recipient, recipientDisplayName, getAuthenticatedUsername(),
                getAuthenticatedDisplayName(), new Date(), quantity, reason);
        // If approved, these pardons will come from your friend.
        storage.addPardonsRequest(pardons, this);
        Toast.makeText(getApplicationContext(), R.string.pardonsRequestedText, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRemovePardonsRequest(Pardons pardons) {
        updateViews(-pardons.getQuantity(), /* stat not displayed */ null,
                PendingOutboundRequestsPardonsFragment.class);
    }

    public void retractRequestForPardons(Pardons pardons) {
        storage.removePardonsRequest(pardons, this);
        Toast.makeText(getApplicationContext(), R.string.pardonsRetractedText, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onAddReceivedPardons(Pardons pardons) {
        // not exposed in the user's own UI - only triggered via storage
        updateViews(pardons.getQuantity(), receivedPardonsText,
                ReceivedPardonsFragment.class);
    }

    @Override
    public void onChangePendingOutboundPardonsRequests() {
        sendUpdate(PendingOutboundRequestsPardonsFragment.class);
    }

    @Override
    public void onChangePendingInboundPardonsRequests() {
        sendUpdate(PendingInboundRequestsPardonsFragment.class);
    }

    @Override
    public void onChangeDeniedOutboundPardonsRequests() {
        sendUpdate(DeniedOutboundRequestsPardonsFragment.class);
    }

    @Override
    public void onChangeDeniedInboundPardonsRequests() {
        sendUpdate(DeniedInboundRequestsPardonsFragment.class);
    }

    private int textToInt(TextView textView) {
        return Integer.parseInt(textView.getText().toString());
    }

    public PardonStorage getStorage() {
        return storage;
    }

    private void updateViews(int pardonsDelta, TextView textView, Class<?> intentActionClass) {
        int newPardonsTotal = textToInt(textView) + pardonsDelta;
        textView.setText(Integer.valueOf(newPardonsTotal).toString());
        sendUpdate(intentActionClass);
    }

    private void sendUpdate(Class<?> intentActionClass) {
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.sendBroadcast(new Intent(intentActionClass.getName()));
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
