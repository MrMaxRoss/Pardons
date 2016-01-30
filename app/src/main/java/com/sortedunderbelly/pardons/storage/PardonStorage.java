package com.sortedunderbelly.pardons.storage;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.sortedunderbelly.pardons.Pardons;

import java.util.List;

/**
 * Created by max.ross on 3/7/15.
 */
public interface PardonStorage {

    interface PardonsUIListener {
        void onApprovePardonsRequest(Pardons pardonsRequest);

        void onDenyPardonsRequest(Pardons pardonsRequest);

        void onRemovePardonsRequest(Pardons pardons);

        void onAddPardonsRequest(Pardons pardons);

        void onAddSentPardons(Pardons pardons);
        void onAddReceivedPardons(Pardons pardons);
        void onChangePendingOutboundPardonsRequests();
        void onChangePendingInboundPardonsRequests();
        void onChangeDeniedOutboundPardonsRequests();
        void onChangeDeniedInboundPardonsRequests();

        void onStorageAuthStateChanged(GoogleSignInAccount account);
        void onStorageAuthenticationError(String errorStr, String token);
    }

    class StorageSignInResult {
        private final GoogleSignInAccount account;
        private final String token;

        public StorageSignInResult(GoogleSignInAccount account, String token) {
            this.account = account;
            this.token = token;
        }

        public GoogleSignInAccount getAccount() {
            return account;
        }

        public String getToken() {
            return token;
        }
    }

    void start(GoogleSignInAccount account);
    void authWithOAuthToken(String provider, StorageSignInResult signInResult);
    void signOut();
    void onDestroy();


    List<Pardons> getReceivedPardons();
    void addReceivedPardons(Pardons pardons);

    List<Pardons> getSentPardons();
    void addSentPardons(Pardons pardons, PardonsUIListener listener);
    
    // Request Management

    // Managing requests you made to your friends
    List<Pardons> getPendingOutboundPardonsRequests();
    List<Pardons> getDeniedOutboundPardonsRequests();
    
    void addPardonsRequest(Pardons pardons, PardonsUIListener listener);
    // You retracted your request for pardons
    void removePardonsRequest(Pardons pardons, PardonsUIListener listener);
    
    // Managing requests your friends made to you
    List<Pardons> getPendingInboundPardonsRequests();
    List<Pardons> getDeniedInboundPardonsRequests();

    void approvePardonsRequest(Pardons pardonsRequest, PardonsUIListener listener);
    void denyPardonsRequest(Pardons pardonsRequest, PardonsUIListener listener);
}
