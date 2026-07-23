package com.quikko.model;

import java.time.Instant;
import java.util.Set;

/**
 * An active (in-memory) pairing between two anonymous users.
 */
public class MatchPair {

    private final String pairId;
    private final String anonIdA;
    private final String anonIdB;
    private final Set<String> sharedInterests;
    private final Instant createdAt;

    public MatchPair(String pairId, String anonIdA, String anonIdB, Set<String> sharedInterests, Instant createdAt) {
        this.pairId = pairId;
        this.anonIdA = anonIdA;
        this.anonIdB = anonIdB;
        this.sharedInterests = sharedInterests;
        this.createdAt = createdAt;
    }

    public String getPairId() {
        return pairId;
    }

    public String getAnonIdA() {
        return anonIdA;
    }

    public String getAnonIdB() {
        return anonIdB;
    }

    public Set<String> getSharedInterests() {
        return sharedInterests;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String otherOf(String anonId) {
        if (anonId.equals(anonIdA)) {
            return anonIdB;
        }
        if (anonId.equals(anonIdB)) {
            return anonIdA;
        }
        throw new IllegalArgumentException("anonId is not part of this pair: " + anonId);
    }

    public boolean contains(String anonId) {
        return anonIdA.equals(anonId) || anonIdB.equals(anonId);
    }
}
