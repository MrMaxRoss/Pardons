package com.sortedunderbelly.pardons.storage;

import com.google.common.collect.Lists;
import com.sortedunderbelly.pardons.Pardon;

import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by max.ross on 3/7/15.
 */
public class InMemoryPardonStorage implements PardonStorage {

    private final AtomicInteger nextId = new AtomicInteger(1);
    private final LinkedList<Pardon> sentPardons = Lists.newLinkedList();
    private final LinkedList<Pardon> receivedPardons = Lists.newLinkedList();
    private final LinkedList<Pardon> outboundRequestedPardons = Lists.newLinkedList();
    private final LinkedList<Pardon> inboundRequestedPardons = Lists.newLinkedList();

    public InMemoryPardonStorage() {
        // Seed the database.
        // Most recent comes first.
        sentPardons.add(newPardon(2015, Calendar.JUNE, 19,
                "max@example.com", "Max Ross", "daphne@example.com", "Daphne Ross",
                1, "wrong toothpaste"));

        sentPardons.add(newPardon(2015, Calendar.JUNE, 10,
                "max@example.com", "Max Ross", "daphne@example.com", "Daphne Ross",
                5, "poor laundry choices"));



        receivedPardons.add(newPardon(2014, Calendar.APRIL, 1,
                "daphne@example.com", "Daphne Ross", "max@example.com", "Max Ross",
                1000, "clogged toilet"));


        outboundRequestedPardons.add(newPardon(2015, Calendar.FEBRUARY, 18,
                "violet@example.com", "Violet Ross", "max@example.com", "Max Ross",
                23, "shoes on table"));

        outboundRequestedPardons.add(newPardon(2014, Calendar.OCTOBER, 10,
                "daphne@example.com", "Daphne Ross", "max@example.com", "Max Ross",
                4, "ate last cookie"));


        inboundRequestedPardons.add(newPardon(2015, Calendar.AUGUST, 19,
                "max@example.com", "Max Ross", "violet@example.com", "Violet Ross",
                6, "stepped on Molly"));
    }

    private Pardon newPardon(int year, int month, int dayOfMonth, String from,
                             String fromDisplayName, String to, String toDisplayName, int quantity,
                             String reason) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, dayOfMonth);
        return new Pardon(getNextId(), from, fromDisplayName, to, toDisplayName, cal.getTime(),
                quantity, reason);
    }

    @Override
    public void addSentPardon(Pardon pardon) {
        Pardon newPardon = new Pardon(
                getNextId(),
                pardon.getFrom(),
                pardon.getFromDisplay(),
                pardon.getTo(),
                pardon.getToDisplay(),
                pardon.getDate(),
                pardon.getQuantity(),
                pardon.getReason());
        // newest goes at the front
        sentPardons.addFirst(newPardon);
    }

    @Override
    public void addReceivedPardon(Pardon pardon) {
        // newest goes at the front
        receivedPardons.addFirst(pardon);
    }

    @Override
    public void addRequestedPardon(Pardon pardon) {
        Pardon newPardon = new Pardon(
                getNextId(),
                pardon.getFrom(),
                pardon.getFromDisplay(),
                pardon.getTo(),
                pardon.getToDisplay(),
                pardon.getDate(),
                pardon.getQuantity(),
                pardon.getReason());
        // newest goest at the front
        outboundRequestedPardons.addFirst(newPardon);
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
    public void deleteRequestedPardon(Pardon pardon) {
        // No need for this to be efficient
        outboundRequestedPardons.remove(pardon);
    }

    @Override
    public void grantPardon(Pardon pardon) {
        // no need to worry about transactional consistency here
        inboundRequestedPardons.remove(pardon);
        addSentPardon(pardon);
    }

    @Override
    public void denyPardon(Pardon pardon) {
        inboundRequestedPardons.remove(pardon);
    }

    @Override
    public List<Pardon> getInboundRequestedPardons() {
        return Collections.unmodifiableList(inboundRequestedPardons);


    }

    private String getNextId() {
        return Integer.valueOf(nextId.getAndIncrement()).toString();
    }
}
