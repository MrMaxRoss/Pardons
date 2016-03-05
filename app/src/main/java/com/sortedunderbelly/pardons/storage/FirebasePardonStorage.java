package com.sortedunderbelly.pardons.storage;

import android.content.Context;
import android.util.Log;

import com.firebase.client.AuthData;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sortedunderbelly.pardons.Accusation;
import com.sortedunderbelly.pardons.Pardons;
import com.sortedunderbelly.pardons.PardonsUIListener;
import com.sortedunderbelly.pardons.PardonsUIListenerProvider;
import com.sortedunderbelly.pardons.R;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by max.ross on 8/13/15.
 */
public class FirebasePardonStorage implements PardonStorage {
    private static final String TAG = "FirebasePardonStorage";

    private static final String USER_DATA_PATH = "users";

    private String userId;
    private final List<BaseChildEventListener> listeners = Lists.newArrayList();

    private final Firebase firebaseRef;

    private final LinkedList<Pardons> receivedPardons = Lists.newLinkedList();
    private final LinkedList<Pardons> sentPardons = Lists.newLinkedList();
    private final LinkedList<Accusation> myAccusations = Lists.newLinkedList();
    private final LinkedList<Accusation> accusationsAgainstMe = Lists.newLinkedList();

    private final PardonsUIListenerProvider uiListenerProvider;
    private final boolean useAuth;

    /* Listener for Firebase session changes */
    private Firebase.AuthStateListener mAuthStateListener;

    public FirebasePardonStorage(Context context, PardonsUIListenerProvider uiListenerProvider, boolean useAuth) {
        this.uiListenerProvider = uiListenerProvider;
        this.useAuth = useAuth;
        Firebase.setAndroidContext(context);
        Firebase.getDefaultConfig().setPersistenceEnabled(true);

        firebaseRef = new Firebase(context.getResources().getString(R.string.firebase_url));
    }

    private void clearInMemoryData() {
        receivedPardons.clear();
        sentPardons.clear();
        myAccusations.clear();
        accusationsAgainstMe.clear();
    }

    @Override
    public void start(final GoogleSignInAccount account) {
        mAuthStateListener = new Firebase.AuthStateListener() {
            @Override
            public void onAuthStateChanged(AuthData authData) {
                FirebasePardonStorage.this.onAuthStateChanged(authData);
            }
        };
        if (useAuth) {
            /* Check if the user is authenticated with Firebase already. If this is the case we can set
             * the authenticated user and hide any login buttons
             */
            firebaseRef.addAuthStateListener(mAuthStateListener);
        } else {
            mAuthStateListener.onAuthStateChanged(new AuthData(
                    account.getIdToken(), Long.MAX_VALUE, account.getId(), "Google", null, null));
        }
    }

    @Override
    public void signOut() {
        clearInMemoryData();
        firebaseRef.unauth();
    }

    @Override
    public void onDestroy() {
        signOut();
        if (useAuth) {
            // if changing configurations, stop tracking firebase session.
            firebaseRef.removeAuthStateListener(mAuthStateListener);
        }
    }

    @Override
    public void authWithOAuthToken(String provider, StorageSignInResult result) {
        firebaseRef.authWithOAuthToken(provider, result.getToken(), new AuthResultHandler(result));
    }

    /**
     * Utility class for authentication results
     */
    private class AuthResultHandler implements Firebase.AuthResultHandler {

        private final StorageSignInResult result;

        public AuthResultHandler(StorageSignInResult result) {
            this.result = result;
        }

        @Override
        public void onAuthenticated(AuthData authData) {
        }

        @Override
        public void onAuthenticationError(FirebaseError firebaseError) {
            uiListenerProvider.get().onStorageAuthenticationError(firebaseError.toString(), result.getToken());
        }
    }

    private void onAuthStateChanged(AuthData authData) {
        if (authData != null) {
            userId = authData.getUid();
            attachPardonListeners();
        } else {
            userId = null;
            clearInMemoryData();
            for (BaseChildEventListener listener : listeners) {
                listener.remove();
            }
            listeners.clear();
        }
    }

    private Firebase getUserRef() {
        return firebaseRef.child(String.format("%s/%s", USER_DATA_PATH, userId));
    }

    static Pardons toPardons(String id, Map<String, Object> pardonData) {
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

    static Accusation toAccusation(String id, Map<String, Object> accusationData) {
        return new Accusation(
                id,
                (String) accusationData.get("accuser"),
                (String) accusationData.get("accuser_display"),
                (String) accusationData.get("accused"),
                (String) accusationData.get("accused_display"),
                new Date((Long) accusationData.get("date")),
                (String) accusationData.get("reason"));
    }

    private void addAccusation(Firebase ref, Accusation accusation) {
        Firebase accusationRef;
        if (accusation.hasId()) {
            accusationRef = ref.child(accusation.getId());
        } else {
            accusationRef = ref.push();
        }

        Map<String, Object> accusationData = Maps.newHashMap();
        accusationData.put("accuser", accusation.getAccuser());
        accusationData.put("accuser_display", accusation.getAccuserDisplay());
        accusationData.put("accused", accusation.getAccused());
        accusationData.put("accused_display", accusation.getAccusedDisplay());
        accusationData.put("date", accusation.getDate().getTime());
        accusationData.put("reason", accusation.getReason());

        accusationRef.setValue(accusationData);
    }

    private void addPardons(Firebase ref, Pardons pardons) {
        Firebase pardonsRef;
        if (pardons.hasId()) {
            pardonsRef = ref.child(pardons.getId());
        } else {
            pardonsRef = ref.push();
        }
        Map<String, Object> pardonData = Maps.newHashMap();
        pardonData.put("from", pardons.getFrom());
        pardonData.put("from_display", pardons.getFromDisplay());
        pardonData.put("to", pardons.getTo());
        pardonData.put("to_display", pardons.getToDisplay());
        pardonData.put("date", pardons.getDate().getTime());
        pardonData.put("quantity", pardons.getQuantity());
        pardonData.put("reason", pardons.getReason());

        pardonsRef.setValue(pardonData);
    }

    private void removeAccusation(Firebase ref, Accusation accusation) {
        Firebase child = ref.child(accusation.getId());
        child.removeValue();
    }

    @Override
    public void receivePardons(Pardons pardons) {
        addPardons(getUserRef().child("received"), pardons);
    }

    private Firebase getSentRef() {
        return getUserRef().child("sent");
    }

    private Firebase getReceivedRef() {
        return getUserRef().child("received");
    }

    @Override
    public void sendPardons(Pardons pardons, PardonsUIListener listener) {
        addPardons(getSentRef(), pardons);
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
    public List<Accusation> getMyAccusations() {
        return Collections.unmodifiableList(myAccusations);
    }

    private Firebase getMyAccusationsRef() {
        return getUserRef().child("accusations_by_me");
    }

    private Firebase getAccusationsAgainstMeRef() {
        return getUserRef().child("accusations_against_me");
    }

    @Override
    public void makeAccusation(Accusation accusation, PardonsUIListener listener) {
        addAccusation(getMyAccusationsRef(), accusation);
    }

    @Override
    public void retractAccusation(Accusation accusation, PardonsUIListener listener) {
        removeAccusation(getMyAccusationsRef(), accusation);
    }

    @Override
    public List<Accusation> getAccusationsAgainstMe() {
        return Collections.unmodifiableList(accusationsAgainstMe);
    }

    @Override
    public void respondToAccusationAgainstMe(Accusation accusation, Pardons derivedPardons, PardonsUIListener listener) {
        removeAccusation(getAccusationsAgainstMeRef(), accusation);
        addPardons(getSentRef(), derivedPardons);
    }

    abstract class BaseChildEventListener<T> implements ChildEventListener {
        private final Firebase ref;
        private final LinkedList<T> list;

        BaseChildEventListener(Firebase ref, LinkedList<T> list) {
            this.ref = ref;
            this.list = list;
            ref.addChildEventListener(this);
        }

        protected abstract void doAddCompletionCallback(T obj);
        protected abstract void doRemoveCompletionCallback(T obj);

        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            T obj = toObject(dataSnapshot.getKey(), mapFromSnapshot(dataSnapshot));
            list.addFirst(obj);
            doAddCompletionCallback(obj);
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String previousChild) {
            Log.w(TAG, "onChildChanged() isn't possible except via console.");
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            T obj = toObject(dataSnapshot.getKey(), mapFromSnapshot(dataSnapshot));
            list.remove(obj);
            doRemoveCompletionCallback(obj);
        }

        @Override
        public void onChildMoved(DataSnapshot dataSnapshot, String previousChild) {
            throw new UnsupportedOperationException("child movement not supported");
        }

        @Override
        public void onCancelled(FirebaseError firebaseError) {
            Log.e(TAG, "onCancelled: " + firebaseError.toString());
        }

        public void remove() {
            ref.removeEventListener(this);
        }

        abstract T toObject(String id, Map<String, Object> data);
    }

    abstract class PardonsChildEventListener extends BaseChildEventListener<Pardons> {
        PardonsChildEventListener(Firebase ref, LinkedList<Pardons> list) {
            super(ref, list);
        }

        @Override
        Pardons toObject(String id, Map<String, Object> data) {
            return toPardons(id, data);
        }
    }

    abstract class AccusationChildEventListener extends BaseChildEventListener<Accusation> {
        AccusationChildEventListener(Firebase ref, LinkedList<Accusation> list) {
            super(ref, list);
        }

        @Override
        Accusation toObject(String id, Map<String, Object> data) {
            return toAccusation(id, data);
        }
    }

    private void attachPardonListeners() {
        listeners.add(new PardonsChildEventListener(getSentRef(), sentPardons) {
            @Override
            protected void doAddCompletionCallback(Pardons pardons) {
                uiListenerProvider.get().onSentPardonsComplete(pardons);
            }

            @Override
            protected void doRemoveCompletionCallback(Pardons pardons) {
                // no way to remove a pardon once sent
            }
        });
        listeners.add(new PardonsChildEventListener(getReceivedRef(), receivedPardons) {
            @Override
            protected void doAddCompletionCallback(Pardons pardons) {
                uiListenerProvider.get().onReceivedPardonsComplete(pardons);
            }

            @Override
            protected void doRemoveCompletionCallback(Pardons pardons) {
                // no way to remove a pardon once received
            }
        });

        listeners.add(new AccusationChildEventListener(getAccusationsAgainstMeRef(), accusationsAgainstMe) {
            @Override
            protected void doAddCompletionCallback(Accusation accusation) {
                uiListenerProvider.get().onAccusationAgainstMeChangeComplete();
            }

            @Override
            protected void doRemoveCompletionCallback(Accusation accusation) {
                uiListenerProvider.get().onAccusationAgainstMeChangeComplete();
            }
        });

        listeners.add(new AccusationChildEventListener(getMyAccusationsRef(), myAccusations) {
            @Override
            protected void doAddCompletionCallback(Accusation accusation) {
                uiListenerProvider.get().onChangeMyAccusations();
            }

            @Override
            protected void doRemoveCompletionCallback(Accusation accusation) {
                uiListenerProvider.get().onChangeMyAccusations();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapFromSnapshot(DataSnapshot snapshot) {
        return (Map<String, Object>) snapshot.getValue();
    }
}
