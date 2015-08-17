package com.sortedunderbelly.pardons.storage;

import android.content.Context;
import android.util.Log;

import com.firebase.client.AuthData;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.google.common.collect.Maps;
import com.sortedunderbelly.pardons.MainActivity;
import com.sortedunderbelly.pardons.Pardon;
import com.sortedunderbelly.pardons.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by max.ross on 8/13/15.
 */
public class FirebasePardonStorage implements PardonStorage {
    private static final String TAG = "FirebasePardonStorage";

    private static final String USER_DATA_PATH = "users";

    private enum PardonType {
        SENT,
        RECEIVED,
        REQUESTED
    }

    private final MainActivity activity;
    private String userId;
    private final Firebase firebaseRef;

    private final List<Pardon> sentPardons = new ArrayList<>();
    private final List<Pardon> receivedPardons = new ArrayList<>();
    private final List<Pardon> outboundRequestedPardons = new ArrayList<>();
    private final List<Pardon> inboundRequestedPardons = new ArrayList<>();

    private final Firebase.AuthResultHandler authResultHandler = new AuthResultHandler();
    private ChildEventListener pardonListener;
    boolean isInitialized = false; // true after we've received our callback with all the user data


    public FirebasePardonStorage(Context context, MainActivity mainActivity) {
        this.activity = mainActivity;
        Firebase.setAndroidContext(context);
        Firebase.getDefaultConfig().setPersistenceEnabled(true);

        firebaseRef = new Firebase(context.getResources().getString(R.string.firebase_url));

        // Check if the user is authenticated with Firebase already. If this is the case we can set
        // the authenticated user and hide any login buttons
        firebaseRef.addAuthStateListener(new Firebase.AuthStateListener() {
            @Override
            public void onAuthStateChanged(AuthData authData) {
                FirebasePardonStorage.this.onAuthStateChanged(authData);
            }
        });
    }

    private void onAuthStateChanged(AuthData authData) {

    }

    private Firebase getPardonsRef() {
        return getUserRef().child("pardons");
    }

    private Firebase getUserRef() {
        return firebaseRef.child(String.format("%s/%s", USER_DATA_PATH, userId));
    }

    static Pardon toPardon(String id, Map<String, Object> pardonData) {
        return new Pardon(
                id,
                (String) pardonData.get("from"),
                (String) pardonData.get("from_display"),
                (String) pardonData.get("to"),
                (String) pardonData.get("to_display"),
                new Date((Long) pardonData.get("date")),
                ((Long) pardonData.get("quantity")).intValue(),
                (String) pardonData.get("reason"));
    }


    /**
     * Utility class for authentication results
     */
    private class AuthResultHandler implements Firebase.AuthResultHandler {

        @Override
        public void onAuthenticated(AuthData authData) {
            FirebasePardonStorage.this.onAuthStateChanged(authData);
        }

        @Override
        public void onAuthenticationError(FirebaseError firebaseError) {
//            authHelper.onAuthStateChanged(null, firebaseError.toString());
        }
    }


    private void savePardon(Pardon pardon, PardonType type) {
        Map<String, Object> pardonData = Maps.newHashMap();
        pardonData.put("from", pardon.getFrom());
        pardonData.put("from_display", pardon.getFromDisplay());
        pardonData.put("to", pardon.getTo());
        pardonData.put("to_display", pardon.getToDisplay());
        pardonData.put("date", pardon.getDate().getTime());
        pardonData.put("quantity", pardon.getQuantity());
        pardonData.put("reason", pardon.getReason());
        pardonData.put("pardon_type", type.name());

        getPardonsRef().push().setValue(pardonData);
    }

    private void deletePardon(Pardon pardon) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addSentPardon(Pardon pardon) {
        savePardon(pardon, PardonType.SENT);
        sentPardons.add(pardon);
    }

    @Override
    public void addReceivedPardon(Pardon pardon) {
        savePardon(pardon, PardonType.RECEIVED);
        receivedPardons.add(pardon);
    }

    @Override
    public void addRequestedPardon(Pardon pardon) {
        savePardon(pardon, PardonType.REQUESTED);
        outboundRequestedPardons.add(pardon);
    }

    @Override
    public void deleteRequestedPardon(Pardon pardon) {
        deletePardon(pardon);
        outboundRequestedPardons.remove(pardon);
    }

    @Override
    public void grantPardon(Pardon pardon) {
        // TODO(max.ross) Need to do this in a single txn
        deletePardon(pardon);
        inboundRequestedPardons.remove(pardon);
        addRequestedPardon(pardon);
    }

    @Override
    public List<Pardon> getSentPardons() {
        return Collections.unmodifiableList(sentPardons);
    }

    @Override
    public List<Pardon> getReceivedPardons() {
        return Collections.unmodifiableList(receivedPardons);
    }

    @Override
    public List<Pardon> getOutboundRequestedPardons() {
        return Collections.unmodifiableList(outboundRequestedPardons);
    }

    @Override
    public List<Pardon> getInboundRequestedPardons() {
        return Collections.unmodifiableList(inboundRequestedPardons);
    }

    private void attachPardonListeners() {
        Firebase pardonsRef = getPardonsRef();
        ChildEventListener pardonListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChild) {
                Map<String, Object> map = mapFromSnapshot(dataSnapshot);
                Pardon newPardon = toPardon(dataSnapshot.getKey(), mapFromSnapshot(dataSnapshot));
                switch (PardonType.valueOf((String) map.get("pardon_type"))) {
                    case SENT:
                        sentPardons.add(newPardon);
                        activity.onSentPardon(newPardon);
                    case RECEIVED:
                        receivedPardons.add(newPardon);
                        activity.onReceivedPardon(newPardon);
                    case REQUESTED:
                        outboundRequestedPardons.add(newPardon);
                        activity.onRequestedPardon(newPardon);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChild) {
                Log.w(TAG, "onChildChanged() isn't possible except via console.");
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Log.w(TAG, "onChildRemoved() isn't possible except via console.");
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChild) {
                throw new UnsupportedOperationException("child movement not supported");
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.e(TAG, "onCancelled: " + firebaseError.toString());
            }
        };
        pardonsRef.addChildEventListener(pardonListener);
    }
    @SuppressWarnings("unchecked")

    private static Map<String, Object> mapFromSnapshot(DataSnapshot snapshot) {
        return (Map<String, Object>) snapshot.getValue();
    }

}
