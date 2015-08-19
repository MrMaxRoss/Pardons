package com.sortedunderbelly.pardons.storage;

import com.google.common.collect.Lists;
import com.sortedunderbelly.pardons.Pardons;

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

    private static final PardonStorage INSTANCE = new InMemoryPardonStorage();

    private final LinkedList<Pardons> receivedPardons = Lists.newLinkedList();
    private final LinkedList<Pardons> sentPardons = Lists.newLinkedList();
    private final LinkedList<Pardons> pendingOutboundPardonsRequests = Lists.newLinkedList();
    private final LinkedList<Pardons> deniedOutboundPardonsRequests = Lists.newLinkedList();
    private final LinkedList<Pardons> pendingInboundPardonsRequests = Lists.newLinkedList();
    private final LinkedList<Pardons> deniedInboundPardonsRequests = Lists.newLinkedList();

    public static PardonStorage getInstance() {
        return INSTANCE;
    }

    private InMemoryPardonStorage() {
        // Seed the database.
        // Most recent comes first.
        sentPardons.add(newPardon(2015, Calendar.JUNE, 19,
                "max@example.com", "Max Ross", "daphne@example.com", "Daphne Ross",
                1, "wrong toothpaste"));

        sentPardons.add(newPardon(2015, Calendar.JUNE, 10,
                "max@example.com", "Max Ross", "daphne@example.com", "Daphne Ross",
                5, "poor laundry choices"));

        pendingInboundPardonsRequests.add(newPardon(2015, Calendar.AUGUST, 19,
                "max@example.com", "Max Ross", "violet@example.com", "Violet Ross",
                6, "stepped on Molly"));

        receivedPardons.add(newPardon(2014, Calendar.APRIL, 1,
                "daphne@example.com", "Daphne Ross", "max@example.com", "Max Ross",
                1000, "clogged toilet"));

        pendingOutboundPardonsRequests.add(newPardon(2015, Calendar.FEBRUARY, 18,
                "violet@example.com", "Violet Ross", "max@example.com", "Max Ross",
                23, "shoes on table"));

        pendingOutboundPardonsRequests.add(newPardon(2014, Calendar.OCTOBER, 10,
                "daphne@example.com", "Daphne Ross", "max@example.com", "Max Ross",
                4, "ate last cookie"));
    }

    private Pardons newPardon(int year, int month, int dayOfMonth, String from,
                             String fromDisplayName, String to, String toDisplayName, int quantity,
                             String reason) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, dayOfMonth);
        return new Pardons(getNextId(), from, fromDisplayName, to, toDisplayName, cal.getTime(),
                quantity, reason);
    }

    @Override
    public List<Pardons> getReceivedPardons() {
        return Collections.unmodifiableList(receivedPardons);
    }

    @Override
    public void addReceivedPardons(Pardons pardons) {
        addToList(pardonsWithId(pardons), receivedPardons);
    }

    @Override
    public List<Pardons> getSentPardons() {
        return Collections.unmodifiableList(sentPardons);
    }

    @Override
    public void addSentPardons(Pardons pardons) {
        addToList(pardonsWithId(pardons), sentPardons);
    }

    @Override
    public List<Pardons> getPendingOutboundPardonsRequests() {
        return Collections.unmodifiableList(pendingOutboundPardonsRequests);
    }

    @Override
    public List<Pardons> getDeniedOutboundPardonsRequests() {
        return Collections.unmodifiableList(deniedOutboundPardonsRequests);
    }

    @Override
    public void addPardonsRequest(Pardons pardons) {
        addToList(pardonsWithId(pardons), pendingOutboundPardonsRequests);
    }

    @Override
    public void removePardonsRequest(Pardons pardons) {
        pendingOutboundPardonsRequests.remove(pardons);
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
    public void approvePardonsRequest(Pardons pardonsRequest) {
        pendingInboundPardonsRequests.remove(pardonsRequest);
        sentPardons.add(pardonsRequest);
    }

    @Override
    public void denyPardonsRequest(Pardons pardonsRequest) {
        pendingInboundPardonsRequests.remove(pardonsRequest);
        deniedInboundPardonsRequests.add(pardonsRequest);
    }

    private static void addToList(Pardons pardons, LinkedList<Pardons> list) {
        // newest goes at the front
        list.addFirst(pardonsWithId(pardons));
    }

    private static Pardons pardonsWithId(Pardons pardons) {
        if (pardons.getId() == null) {
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
    private static String getNextId() {
        return Integer.valueOf(nextId.getAndIncrement()).toString();
    }
}
