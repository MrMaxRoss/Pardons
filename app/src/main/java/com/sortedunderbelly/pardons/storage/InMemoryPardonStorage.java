package com.sortedunderbelly.pardons.storage;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.common.collect.Lists;
import com.sortedunderbelly.pardons.Accusation;
import com.sortedunderbelly.pardons.Pardons;
import com.sortedunderbelly.pardons.PardonsUIListener;
import com.sortedunderbelly.pardons.PardonsUIListenerProvider;

import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * Created by max.ross on 3/7/15.
 */
public class InMemoryPardonStorage implements PardonStorage {

    private static final AtomicInteger nextId = new AtomicInteger(1);

    private final PardonsUIListenerProvider pardonsUIListenerProvider;
    private final LinkedList<Pardons> receivedPardons = Lists.newLinkedList();
    private final LinkedList<Pardons> sentPardons = Lists.newLinkedList();
    private final LinkedList<Accusation> myAccusations = Lists.newLinkedList();
    private final LinkedList<Accusation> accusationsAgainstMe = Lists.newLinkedList();

    public InMemoryPardonStorage(PardonsUIListenerProvider pardonsUIListenerProvider) {
        this.pardonsUIListenerProvider = pardonsUIListenerProvider;
        // Seed the database.
        // Most recent comes first.
        sentPardons.add(newPardon(2015, Calendar.JUNE, 19,
                "max@example.com", "Max Ross", "daphne@example.com", "Daphne Ross",
                1, "wrong toothpaste"));

        sentPardons.add(newPardon(2015, Calendar.JUNE, 10,
                "max@example.com", "Max Ross", "daphne@example.com", "Daphne Ross",
                5, "poor laundry choices"));

        accusationsAgainstMe.add(newAccusation(2015, Calendar.AUGUST, 19,
                "max@example.com", "Max Ross", "violet@example.com", "Violet Ross",
                "stepped on Molly"));

        receivedPardons.add(newPardon(2014, Calendar.APRIL, 1,
                "daphne@example.com", "Daphne Ross", "max@example.com", "Max Ross",
                1000, "clogged toilet"));

        myAccusations.add(newAccusation(2015, Calendar.FEBRUARY, 18,
                "violet@example.com", "Violet Ross", "max@example.com", "Max Ross",
                "shoes on table"));

        myAccusations.add(newAccusation(2014, Calendar.OCTOBER, 10,
                "daphne@example.com", "Daphne Ross", "max@example.com", "Max Ross",
                "ate last cookie"));

        authWithOAuthToken("dummy", new StorageSignInResult(null, "dummy token"));
    }

    private Pardons newPardon(int year, int month, int dayOfMonth, String from,
                             String fromDisplayName, String to, String toDisplayName, int quantity,
                             String reason) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, dayOfMonth);
        return new Pardons(getNextId(), from, fromDisplayName, to, toDisplayName, cal.getTime(),
                quantity, reason);
    }

    private Accusation newAccusation(int year, int month, int dayOfMonth, String from,
                             String fromDisplayName, String to, String toDisplayName,
                             String reason) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, dayOfMonth);
        return new Accusation(getNextId(), from, fromDisplayName, to, toDisplayName, cal.getTime(),
                reason);
    }

    @Override
    public List<Pardons> getReceivedPardons() {
        return Collections.unmodifiableList(receivedPardons);
    }

    @Override
    public void receivePardons(Pardons pardons) {
        addToList(pardonsWithId(pardons), receivedPardons);
    }

    @Override
    public List<Pardons> getSentPardons() {
        return Collections.unmodifiableList(sentPardons);
    }

    @Override
    public void sendPardons(Pardons pardons, PardonsUIListener listener) {
        addToList(pardonsWithId(pardons), sentPardons);
        listener.onSentPardonsComplete(pardons);
    }

    @Override
    public List<Accusation> getMyAccusations() {
        return Collections.unmodifiableList(myAccusations);
    }

    @Override
    public void makeAccusation(Accusation accusation, PardonsUIListener listener) {
        addToList(accusationWithId(accusation), myAccusations);
        listener.onMakeAccusationComplete(accusation);
    }

    @Override
    public void retractAccusation(Accusation accusation, PardonsUIListener listener) {
        myAccusations.remove(accusation);
        listener.onRetractAccusationComplete(accusation);
    }

    @Override
    public List<Accusation> getAccusationsAgainstMe() {
        return Collections.unmodifiableList(accusationsAgainstMe);
    }

    @Override
    public void respondToAccusationAgainstMe(Accusation accusation, Pardons derivedPardons, PardonsUIListener listener) {
        accusationsAgainstMe.remove(derivedPardons);
        sentPardons.add(derivedPardons);
        listener.onRespondToAccusationAgainstMeComplete(derivedPardons);
    }

    private static void addToList(Pardons pardons, LinkedList<Pardons> list) {
        // newest goes at the front
        list.addFirst(pardonsWithId(pardons));
    }

    private static void addToList(Accusation accusation, LinkedList<Accusation> list) {
        // newest goes at the front
        list.addFirst(accusationWithId(accusation));
    }

    private static Pardons pardonsWithId(Pardons pardons) {
        if (!pardons.hasId()) {
            return new Pardons(
                    getNextId(),
                    pardons.getFrom(),
                    pardons.getFromDisplay(),
                    pardons.getTo(),
                    pardons.getToDisplay(),
                    pardons.getDate(),
                    pardons.getQuantity(),
                    pardons.getReason());
        }
        return pardons;
    }

    private static Accusation accusationWithId(Accusation accusation) {
        if (!accusation.hasId()) {
            return new Accusation(
                    getNextId(),
                    accusation.getAccuser(),
                    accusation.getAccuserDisplay(),
                    accusation.getAccused(),
                    accusation.getAccusedDisplay(),
                    accusation.getDate(),
                    accusation.getReason());
        }
        return accusation;
    }

    private static String getNextId() {
        return Integer.valueOf(nextId.getAndIncrement()).toString();
    }

    @Override
    public void authWithOAuthToken(String provider, StorageSignInResult signInResult) {
    }

    @Override
    public void start(GoogleSignInAccount account) {
    }

    @Override
    public void signOut() {
    }

    @Override
    public void onDestroy() {

    }
}
