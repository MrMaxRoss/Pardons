package com.sortedunderbelly.pardons.storage;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.sortedunderbelly.pardons.Accusation;
import com.sortedunderbelly.pardons.MainActivity;
import com.sortedunderbelly.pardons.Pardons;
import com.sortedunderbelly.pardons.PardonsUIListener;

import java.util.List;

/**
 * Created by max.ross on 3/7/15.
 */
public interface PardonStorage {

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
    List<Pardons> getSentPardons();
    List<Accusation> getMyAccusations();
    List<Accusation> getAccusationsAgainstMe();

    void receivePardons(Pardons pardons);
    void sendPardons(Pardons pardons, PardonsUIListener listener);
    void makeAccusation(Accusation accusation, PardonsUIListener listener);
    void retractAccusation(Accusation accusation, PardonsUIListener listener);
    void respondToAccusationAgainstMe(Accusation accusation, Pardons derivedPardons, PardonsUIListener listener);
}
