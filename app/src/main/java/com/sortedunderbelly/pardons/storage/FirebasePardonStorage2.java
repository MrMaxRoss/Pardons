package com.sortedunderbelly.pardons.storage;

import android.content.Context;
import android.util.Log;

import com.firebase.client.AuthData;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.Query;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sortedunderbelly.pardons.Accusation;
import com.sortedunderbelly.pardons.Pardons;
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
public class FirebasePardonStorage2 implements PardonStorage {
    private static final String TAG = "FirebasePardonStorage";

    private static final String ACCUSATIONS = "accusations";
    private static final String PARDONS = "pardons";

    private static final String EMAIL_TO_USER_ID = "email_to_user_id";

    private GoogleSignInAccount googleSignInAccount;
    private String userId;
    private String userEmailAddress;
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

    private Map<String, String> emailToUserIdMap = Maps.newHashMap();

    public FirebasePardonStorage2(Context context, PardonsUIListenerProvider uiListenerProvider, boolean useAuth) {
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
        googleSignInAccount = account;
        mAuthStateListener = new Firebase.AuthStateListener() {
            @Override
            public void onAuthStateChanged(AuthData authData) {
                FirebasePardonStorage2.this.onAuthStateChanged(authData);
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
        userId = null;
        userEmailAddress = null;
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

    public Firebase getAccusationsRef() {
        return firebaseRef.child(ACCUSATIONS);
    }

    public Firebase getPardonsRef() {
        return firebaseRef.child(PARDONS);
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
            userEmailAddress = googleSignInAccount.getEmail();
            registerAuthenticatedUser();
            populateEmailToUserIdMap();
            attachPardonListeners();
        } else {
            userId = null;
            userEmailAddress = null;
            clearInMemoryData();
            for (BaseChildEventListener listener : listeners) {
                listener.remove();
            }
            listeners.clear();
        }
    }

    private void registerAuthenticatedUser() {
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

    private void addAccusation(Accusation accusation) {
        Firebase accusationRef;
        if (accusation.hasId()) {
            accusationRef = getAccusationsRef().child(accusation.getId());

        } else {
            accusationRef = getAccusationsRef().push();
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

    private void addPardons(Pardons pardons) {
        Firebase pardonsRef;
        if (pardons.hasId()) {
            pardonsRef = getPardonsRef().child(pardons.getId());
        } else {
            pardonsRef = getPardonsRef().push();
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

    private void removeAccusation(Accusation accusation) {
        getAccusationsRef().child(accusation.getId()).removeValue();
    }

    @Override
    public void sendPardons(Pardons pardons) {
        addPardons(pardons);
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

    @Override
    public void makeAccusation(Accusation accusation) {
        addAccusation(accusation);
    }

    private void populateEmailToUserIdMap() {
        ChildEventListener listener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                emailToUserIdMap.put(dataSnapshot.getKey(), (String) dataSnapshot.child("userId").getValue());
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                emailToUserIdMap.put(dataSnapshot.getKey(), (String) dataSnapshot.child("userId").getValue());
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                emailToUserIdMap.remove(dataSnapshot.getKey());
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                throw new UnsupportedOperationException("Cannot move elements of the email to userId Map");
            }

            @Override
            public void onCancelled(FirebaseError firebaseError) {
                Log.e(TAG, "emailToUserId onCancelled: " + firebaseError.toString());
            }
        };

        firebaseRef.child(EMAIL_TO_USER_ID).addChildEventListener(listener);
    }


    private String getOrCreateUserId(String accused) {
        String userId = emailToUserIdMap.get(accused);
        if (userId == null) {
            createEmailToUserIdMapping(accused);
        }
        return userId;
    }

    private void createEmailToUserIdMapping(String accused) {
        Firebase newEntry = firebaseRef.child(EMAIL_TO_USER_ID).child(accused);
        newEntry.setValue(accused);
    }

    @Override
    public void retractAccusation(Accusation accusation) {
        removeAccusation(accusation);
    }

    @Override
    public List<Accusation> getAccusationsAgainstMe() {
        return Collections.unmodifiableList(accusationsAgainstMe);
    }

    @Override
    public void respondToAccusationAgainstMe(Accusation accusation, Pardons derivedPardons) {
        removeAccusation(accusation);
        addPardons(derivedPardons);
    }

    abstract class BaseChildEventListener<T> implements ChildEventListener {
        private final Query query;
        private final LinkedList<T> list;

        BaseChildEventListener(Query query, LinkedList<T> list) {
            this.query = query;
            this.list = list;
            query.addChildEventListener(this);
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
            query.removeEventListener(this);
        }

        abstract T toObject(String id, Map<String, Object> data);
    }

    abstract class PardonsChildEventListener extends BaseChildEventListener<Pardons> {
        PardonsChildEventListener(Query query, LinkedList<Pardons> list) {
            super(query, list);
        }

        @Override
        Pardons toObject(String id, Map<String, Object> data) {
            return toPardons(id, data);
        }
    }

    abstract class AccusationChildEventListener extends BaseChildEventListener<Accusation> {
        AccusationChildEventListener(Query query, LinkedList<Accusation> list) {
            super(query, list);
        }

        @Override
        Accusation toObject(String id, Map<String, Object> data) {
            return toAccusation(id, data);
        }
    }

    private void attachPardonListeners() {
        listeners.add(new PardonsChildEventListener(getPardonsFromAuthenticatedUser(), sentPardons) {
            @Override
            protected void doAddCompletionCallback(Pardons pardons) {
                uiListenerProvider.get().onSentPardonsComplete(pardons);
            }

            @Override
            protected void doRemoveCompletionCallback(Pardons pardons) {
                // no way to remove a pardon once sent
            }
        });
        listeners.add(new PardonsChildEventListener(getPardonsForAuthenticatedUser(), receivedPardons) {
            @Override
            protected void doAddCompletionCallback(Pardons pardons) {
                uiListenerProvider.get().onReceivedPardonsComplete(pardons);
            }

            @Override
            protected void doRemoveCompletionCallback(Pardons pardons) {
                // no way to remove a pardon once received
            }
        });

        listeners.add(new AccusationChildEventListener(getAccusationsAgainstAuthentictedUser(), accusationsAgainstMe) {
            @Override
            protected void doAddCompletionCallback(Accusation accusation) {
                uiListenerProvider.get().onAccusationAgainstMeChangeComplete();
            }

            @Override
            protected void doRemoveCompletionCallback(Accusation accusation) {
                uiListenerProvider.get().onAccusationAgainstMeChangeComplete();
            }
        });

        listeners.add(new AccusationChildEventListener(getAccusationsFromAuthenticatedUser(), myAccusations) {
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

    private Query getAccusationsFromAuthenticatedUser() {
        return getAccusationsRef().orderByChild("accuser").equalTo(userEmailAddress);
    }

    private Query getAccusationsAgainstAuthentictedUser() {
        return getAccusationsRef().orderByChild("accused").equalTo(userEmailAddress);
    }

    private Query getPardonsForAuthenticatedUser() {
        return getPardonsRef().orderByChild("to").equalTo(userEmailAddress);
    }

    private Query getPardonsFromAuthenticatedUser() {
        return getPardonsRef().orderByChild("from").equalTo(userEmailAddress);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapFromSnapshot(DataSnapshot snapshot) {
        return (Map<String, Object>) snapshot.getValue();
    }
}
