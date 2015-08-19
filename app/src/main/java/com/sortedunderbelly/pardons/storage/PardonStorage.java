package com.sortedunderbelly.pardons.storage;

import com.sortedunderbelly.pardons.Pardons;

import java.util.List;

/**
 * Created by max.ross on 3/7/15.
 */
public interface PardonStorage {
    List<Pardons> getReceivedPardons();
    void addReceivedPardons(Pardons pardons);

    List<Pardons> getSentPardons();
    void addSentPardons(Pardons pardons);
    
    // Request Management

    // Managing requests you made to your friends
    List<Pardons> getPendingOutboundPardonsRequests();
    List<Pardons> getDeniedOutboundPardonsRequests();
    
    void addPardonsRequest(Pardons pardons);
    // You retracted your request for pardons
    void removePardonsRequest(Pardons pardons);
    
    // Managing requests your friends made to you
    List<Pardons> getPendingInboundPardonsRequests();
    List<Pardons> getDeniedInboundPardonsRequests();

    void approvePardonsRequest(Pardons pardonsRequest);
    void denyPardonsRequest(Pardons pardonsRequest);
}
