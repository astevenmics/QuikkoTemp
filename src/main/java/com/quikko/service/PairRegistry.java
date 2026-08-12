package com.quikko.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Remembers who was paired with whom for a while after the live match ends.
 * The active {@code MatchQueueStore} pair is deleted the instant a match ends (Skip/Stop/Report),
 * but a Time Capsule can still be left right after that — and since anonymous ids are never shared with the other browser,
 * the server is the only place that can resolve "who was I just talking to" for capsule addressing.
 * Entries are short-lived and exist purely for that purpose.
 */
@Service
public class PairRegistry {

    private static final Duration RETENTION = Duration.ofMinutes(30);

    private record Members(String anonIdA, String anonIdB, Instant createdAt) {
    }

    private final ConcurrentHashMap<String, Members> pairs = new ConcurrentHashMap<>();

    public void record(String pairId, String anonIdA, String anonIdB) {
        pairs.put(pairId, new Members(anonIdA, anonIdB, Instant.now()));
    }

    /**
     * Returns the other participant's anonymous id for this pairing, if the given anonId was actually part of it and the record hasn't expired.
     */
    public Optional<String> otherOf(String pairId, String anonId) {
        Members m = pairs.get(pairId);
        if (m == null) {
            return Optional.empty();
        }
        if (anonId.equals(m.anonIdA())) {
            return Optional.of(m.anonIdB());
        }
        if (anonId.equals(m.anonIdB())) {
            return Optional.of(m.anonIdA());
        }
        return Optional.empty();
    }

    @Scheduled(fixedRate = 5 * 60_000)
    void cleanup() {
        Instant cutoff = Instant.now().minus(RETENTION);
        pairs.entrySet().removeIf(e -> e.getValue().createdAt().isBefore(cutoff));
    }
}
