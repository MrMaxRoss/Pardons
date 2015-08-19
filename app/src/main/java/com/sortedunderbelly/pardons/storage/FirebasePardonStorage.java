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
import com.sortedunderbelly.pardons.Pardons;
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

    private final MainActivity activity;
    private String userId;
    private final Firebase firebaseRef;

    private final List<Pardons> sentPardons = new ArrayList<>();
    private final List<Pardons> receivedPardons = new ArrayList<>();

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

    static Pardons toPardon(String id, Map<String, Object> pardonData) {
        return new Pardons(
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


    private void savePardon(Pardons pardons) {
        Map<String, Object> pardonData = Maps.newHashMap();
        pardonData.put("from", pardons.getFrom());
        pardonData.put("from_display", pardons.getFromDisplay());
        pardonData.put("to", pardons.getTo());
        pardonData.put("to_display", pardons.getToDisplay());
        pardonData.put("date", pardons.getDate().getTime());
        pardonData.put("quantity", pardons.getQuantity());
        pardonData.put("reason", pardons.getReason());
    }

    @Override
    public void addReceivedPardons(Pardons pardons) {
    }

    @Override
    public void addSentPardons(Pardons pardons) {
    }

    @Override
    public List<Pardons> getSentPardons() {
        return Collections.unmodifiableList(sentPardons);
    }

    @Override
    public List<Pardons> getReceivedPardons() {
        return Collections.unmodifiableList(receivedPardons);
    }

    @Override
    public List<Pardons> getPendingOutboundPardonsRequests() {
        return null;
    }

    @Override
    public List<Pardons> getDeniedOutboundPardonsRequests() {
        return null;
    }

    @Override
    public void addPardonsRequest(Pardons pardons) {

    }

    @Override
    public void removePardonsRequest(Pardons pardons) {

    }

    @Override
    public List<Pardons> getPendingInboundPardonsRequests() {
        return null;
    }

    @Override
    public List<Pardons> getDeniedInboundPardonsRequests() {
        return null;
    }

    @Override
    public void approvePardonsRequest(Pardons pardonsRequest) {

    }

    @Override
    public void denyPardonsRequest(Pardons pardonsRequest) {

    }

    private void attachPardonListeners() {
        Firebase pardonsRef = getPardonsRef();
        ChildEventListener pardonListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChild) {
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
