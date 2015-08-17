package com.sortedunderbelly.pardons.storage;

import com.sortedunderbelly.pardons.Pardon;

import java.util.List;

/**
 * Created by max.ross on 3/7/15.
 */
public interface PardonStorage {
    void addSentPardon(Pardon pardon);
    void addReceivedPardon(Pardon pardon);
    void addRequestedPardon(Pardon pardon);
    void deleteRequestedPardon(Pardon pardon);
    void grantPardon(Pardon pardon);
    List<Pardon> getSentPardons();
    List<Pardon> getReceivedPardons();
    List<Pardon> getOutboundRequestedPardons();
    List<Pardon> getInboundRequestedPardons();
}
