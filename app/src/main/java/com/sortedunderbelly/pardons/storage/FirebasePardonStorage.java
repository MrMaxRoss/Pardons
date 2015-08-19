package com.sortedunderbelly.pardons.storage;

import android.content.Context;
import android.util.Log;

import com.firebase.client.AuthData;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.sortedunderbelly.pardons.Pardons;
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

    private static PardonStorage INSTANCE;

    private static final String USER_DATA_PATH = "users";

    private String userId;
    private final Firebase firebaseRef;

    private final LinkedList<Pardons> receivedPardons = Lists.newLinkedList();
    private final LinkedList<Pardons> sentPardons = Lists.newLinkedList();
    private final LinkedList<Pardons> pendingOutboundPardonsRequests = Lists.newLinkedList();
    private final LinkedList<Pardons> deniedOutboundPardonsRequests = Lists.newLinkedList();
    private final LinkedList<Pardons> pendingInboundPardonsRequests = Lists.newLinkedList();
    private final LinkedList<Pardons> deniedInboundPardonsRequests = Lists.newLinkedList();

    private final PardonsUIListener listener;

    private final Firebase.AuthResultHandler authResultHandler = new AuthResultHandler();
    private ChildEventListener pardonListener;
    boolean isInitialized = false; // true after we've received our callback with all the user data


    public FirebasePardonStorage(Context context, PardonsUIListener listener) {
        this.listener = listener;
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

        // fake login
        onAuthStateChanged(new AuthData("dummy token", -1, "dummy uid", "dummy", null, null));
    }

    private void onAuthStateChanged(AuthData authData) {
        if (authData != null) {
            userId = authData.getUid();
            attachPardonListeners();
        }
//        authHelper.onAuthStateChanged(authDataToAuthStruct(authData), null);
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


    private static Pardons pardonsWithId(String id, Pardons pardons) {
        return new Pardons(
                id,
                pardons.getFrom(),
                pardons.getFromDisplay(),
                pardons.getTo(),
                pardons.getToDisplay(),
                pardons.getDate(),
                pardons.getQuantity(),
                pardons.getReason());
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
        private final LinkedList<Pardons> list;

        PardonChildEventListener(LinkedList<Pardons> list) {
            this.list = list;
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
    }

    private void attachPardonListeners() {
        Firebase sentRef = getSentRef();
        ChildEventListener pardonListener = new PardonChildEventListener(sentPardons) {
            @Override
            protected void doAddCompletionCallback(Pardons pardons) {
                listener.onAddSentPardons(pardons);
            }

            @Override
            protected void doRemoveCompletionCallback(Pardons pardons) {
                // no way to remove a pardon once sent
            }
        };
        sentRef.addChildEventListener(pardonListener);
        Firebase receivedRef = getReceivedRef();
        pardonListener = new PardonChildEventListener(receivedPardons) {
            @Override
            protected void doAddCompletionCallback(Pardons pardons) {
                listener.onAddReceivedPardons(pardons);
            }

            @Override
            protected void doRemoveCompletionCallback(Pardons pardons) {
                // no way to remove a pardon once received
            }
        };
        receivedRef.addChildEventListener(pardonListener);

        Firebase pendingInboundRef = getPendingInboundRef();
        pardonListener = new PardonChildEventListener(pendingInboundPardonsRequests) {
            @Override
            protected void doAddCompletionCallback(Pardons pardons) {
                listener.onChangePendingInboundPardonsRequests();
            }

            @Override
            protected void doRemoveCompletionCallback(Pardons pardons) {
                listener.onChangePendingInboundPardonsRequests();
            }
        };
        pendingInboundRef.addChildEventListener(pardonListener);

        Firebase pendingOutboundRef = getPendingOutboundRef();
        pardonListener = new PardonChildEventListener(pendingOutboundPardonsRequests) {
            @Override
            protected void doAddCompletionCallback(Pardons pardons) {
                listener.onChangePendingOutboundPardonsRequests();
            }

            @Override
            protected void doRemoveCompletionCallback(Pardons pardons) {
                listener.onChangePendingOutboundPardonsRequests();
            }
        };
        pendingOutboundRef.addChildEventListener(pardonListener);

        Firebase deniedInboundRef = getDeniedInboundRef();
        pardonListener = new PardonChildEventListener(deniedInboundPardonsRequests) {
            @Override
            protected void doAddCompletionCallback(Pardons pardons) {
                listener.onChangeDeniedInboundPardonsRequests();
            }

            @Override
            protected void doRemoveCompletionCallback(Pardons pardons) {
                listener.onChangeDeniedInboundPardonsRequests();
            }
        };
        deniedInboundRef.addChildEventListener(pardonListener);

        Firebase deniedOutboundRef = getDeniedOutboundRef();
        pardonListener = new PardonChildEventListener(deniedOutboundPardonsRequests) {
            @Override
            protected void doAddCompletionCallback(Pardons pardons) {
                listener.onChangeDeniedOutboundPardonsRequests();
            }

            @Override
            protected void doRemoveCompletionCallback(Pardons pardons) {
                listener.onChangeDeniedOutboundPardonsRequests();
            }
        };
        deniedOutboundRef.addChildEventListener(pardonListener);
    }

    @SuppressWarnings("unchecked")

    private static Map<String, Object> mapFromSnapshot(DataSnapshot snapshot) {
        return (Map<String, Object>) snapshot.getValue();
    }
}
