package com.quikko.service.store;

import com.quikko.model.ChatUser;
import com.quikko.model.MatchPair;

import java.util.List;
import java.util.Optional;

/**
 * Backing store for the waiting queue and active pairings.
 */
public interface MatchQueueStore {

    void enqueue(ChatUser user);

    void dequeue(String anonId);

    boolean isQueued(String anonId);

    /**
     * Snapshot of everyone currently waiting, oldest-joined first.
     */
    List<ChatUser> snapshot();

    void savePair(MatchPair pair);

    Optional<MatchPair> getPairForUser(String anonId);

    Optional<MatchPair> getPair(String pairId);

    void removePair(String pairId);
}
