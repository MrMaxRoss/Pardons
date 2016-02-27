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
    private final List<PardonChildEventListener> listeners = Lists.newArrayList();

    private final Firebase firebaseRef;

    private final LinkedList<Pardons> receivedPardons = Lists.newLinkedList();
    private final LinkedList<Pardons> sentPardons = Lists.newLinkedList();
    private final LinkedList<Pardons> pendingOutboundPardonsRequests = Lists.newLinkedList();
    private final LinkedList<Pardons> deniedOutboundPardonsRequests = Lists.newLinkedList();
    private final LinkedList<Pardons> pendingInboundPardonsRequests = Lists.newLinkedList();
    private final LinkedList<Pardons> deniedInboundPardonsRequests = Lists.newLinkedList();

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
        pendingOutboundPardonsRequests.clear();
        deniedOutboundPardonsRequests.clear();
        pendingInboundPardonsRequests.clear();
        deniedInboundPardonsRequests.clear();
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
            for (PardonChildEventListener listener : listeners) {
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

    private void removePardons(Firebase ref, Pardons pardons) {
        Firebase child = ref.child(pardons.getId());
        child.removeValue();
    }

    @Override
    public void addReceivedPardons(Pardons pardons) {
        addPardons(getUserRef().child("received"), pardons);
    }

    private Firebase getSentRef() {
        return getUserRef().child("sent");
    }

    private Firebase getReceivedRef() {
        return getUserRef().child("received");
    }

    @Override
    public void addSentPardons(Pardons pardons, PardonsUIListener listener) {
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
    public List<Pardons> getPendingOutboundPardonsRequests() {
        return Collections.unmodifiableList(pendingOutboundPardonsRequests);
    }

    @Override
    public List<Pardons> getDeniedOutboundPardonsRequests() {
        return Collections.unmodifiableList(deniedOutboundPardonsRequests);
    }

    private Firebase getPendingOutboundRef() {
        return getUserRef().child("pending_outbound");
    }

    private Firebase getDeniedOutboundRef() {
        return getUserRef().child("denied_outbound");
    }

    private Firebase getPendingInboundRef() {
        return getUserRef().child("pending_inbound");
    }

    private Firebase getDeniedInboundRef() {
        return getUserRef().child("denied_inbound");
    }

    @Override
    public void addPardonsRequest(Pardons pardons, PardonsUIListener listener) {
        addPardons(getPendingOutboundRef(), pardons);
    }

    @Override
    public void removePardonsRequest(Pardons pardons, PardonsUIListener listener) {
        removePardons(getPendingOutboundRef(), pardons);
    }

    @Override
    public List<Pardons> getPendingInboundPardonsRequests() {
        return Collections.unmodifiableList(pendingInboundPardonsRequests);
    }

    @Override
    public List<Pardons> getDeniedInboundPardonsRequests() {
        return Collections.unmodifiableList(deniedInboundPardonsRequests);
    }

    @Override
    public void approvePardonsRequest(Pardons pardonsRequest, PardonsUIListener listener) {
        removePardons(getPendingInboundRef(), pardonsRequest);
        addPardons(getSentRef(), pardonsRequest);
    }

    @Override
    public void denyPardonsRequest(Pardons pardonsRequest, PardonsUIListener listener) {
        removePardons(getPendingInboundRef(), pardonsRequest);
        addPardons(getDeniedInboundRef(), pardonsRequest);
    }

    abstract class PardonChildEventListener implements ChildEventListener {
        private final Firebase ref;
        private final LinkedList<Pardons> list;

        PardonChildEventListener(Firebase ref, LinkedList<Pardons> list) {
            this.ref = ref;
            this.list = list;
            ref.addChildEventListener(this);
        }

        protected abstract void doAddCompletionCallback(Pardons pardons);
        protected abstract void doRemoveCompletionCallback(Pardons pardons);

        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            Pardons pardons = toPardons(dataSnapshot.getKey(), mapFromSnapshot(dataSnapshot));
            list.addFirst(pardons);
            doAddCompletionCallback(pardons);
        }

        @Override
        public void onChildChanged(DataSnapshot dataSnapshot, String previousChild) {
            Log.w(TAG, "onChildChanged() isn't possible except via console.");
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            Pardons pardons = toPardons(dataSnapshot.getKey(), mapFromSnapshot(dataSnapshot));
            list.remove(pardons);
            doRemoveCompletionCallback(pardons);
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
    }

    private void attachPardonListeners() {
        listeners.add(new PardonChildEventListener(getSentRef(), sentPardons) {
            @Override
            protected void doAddCompletionCallback(Pardons pardons) {
                uiListenerProvider.get().onAddSentPardons(pardons);
            }

            @Override
            protected void doRemoveCompletionCallback(Pardons pardons) {
                // no way to remove a pardon once sent
            }
        });
        listeners.add(new PardonChildEventListener(getReceivedRef(), receivedPardons) {
            @Override
            protected void doAddCompletionCallback(Pardons pardons) {
                uiListenerProvider.get().onAddReceivedPardons(pardons);
            }

            @Override
            protected void doRemoveCompletionCallback(Pardons pardons) {
                // no way to remove a pardon once received
            }
        });

        listeners.add(new PardonChildEventListener(getPendingInboundRef(), pendingInboundPardonsRequests) {
            @Override
            protected void doAddCompletionCallback(Pardons pardons) {
                uiListenerProvider.get().onChangePendingInboundPardonsRequests();
            }

            @Override
            protected void doRemoveCompletionCallback(Pardons pardons) {
                uiListenerProvider.get().onChangePendingInboundPardonsRequests();
            }
        });

        listeners.add(new PardonChildEventListener(getPendingOutboundRef(), pendingOutboundPardonsRequests) {
            @Override
            protected void doAddCompletionCallback(Pardons pardons) {
                uiListenerProvider.get().onChangePendingOutboundPardonsRequests();
            }

            @Override
            protected void doRemoveCompletionCallback(Pardons pardons) {
                uiListenerProvider.get().onChangePendingOutboundPardonsRequests();
            }
        });

        listeners.add(new PardonChildEventListener(getDeniedInboundRef(), deniedInboundPardonsRequests) {
            @Override
            protected void doAddCompletionCallback(Pardons pardons) {
                uiListenerProvider.get().onChangeDeniedInboundPardonsRequests();
            }

            @Override
            protected void doRemoveCompletionCallback(Pardons pardons) {
                uiListenerProvider.get().onChangeDeniedInboundPardonsRequests();
            }
        });

        listeners.add(new PardonChildEventListener(getDeniedOutboundRef(), deniedOutboundPardonsRequests) {
            @Override
            protected void doAddCompletionCallback(Pardons pardons) {
                uiListenerProvider.get().onChangeDeniedOutboundPardonsRequests();
            }

            @Override
            protected void doRemoveCompletionCallback(Pardons pardons) {
                uiListenerProvider.get().onChangeDeniedOutboundPardonsRequests();
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapFromSnapshot(DataSnapshot snapshot) {
        return (Map<String, Object>) snapshot.getValue();
    }
}
