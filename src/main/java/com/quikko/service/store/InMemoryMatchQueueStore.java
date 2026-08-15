package com.quikko.service.store;

import com.quikko.model.ChatUser;
import com.quikko.model.MatchPair;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory queue store. State is lost on restart and does not fan out
 * across multiple app instances — fine for a single-instance deployment.
 */
@Component
public class InMemoryMatchQueueStore implements MatchQueueStore {

    private final Map<String, ChatUser> waiting = new ConcurrentHashMap<>();
    private final Map<String, MatchPair> pairsById = new ConcurrentHashMap<>();
    private final Map<String, String> pairIdByAnonId = new ConcurrentHashMap<>();

    @Override
    public void enqueue(ChatUser user) {
        waiting.put(user.getAnonId(), user);
    }

    @Override
    public void dequeue(String anonId) {
        waiting.remove(anonId);
    }

    @Override
    public boolean isQueued(String anonId) {
        return waiting.containsKey(anonId);
    }

    @Override
    public List<ChatUser> snapshot() {
        Collection<ChatUser> values = waiting.values();
        return values.stream()
                .sorted((a, b) -> a.getQueuedAt().compareTo(b.getQueuedAt()))
                .toList();
    }

    @Override
    public void savePair(MatchPair pair) {
        pairsById.put(pair.getPairId(), pair);
        pairIdByAnonId.put(pair.getAnonIdA(), pair.getPairId());
        pairIdByAnonId.put(pair.getAnonIdB(), pair.getPairId());
    }

    @Override
    public Optional<MatchPair> getPairForUser(String anonId) {
        String pairId = pairIdByAnonId.get(anonId);
        return pairId == null ? Optional.empty() : getPair(pairId);
    }

    @Override
    public Optional<MatchPair> getPair(String pairId) {
        return Optional.ofNullable(pairsById.get(pairId));
    }

    @Override
    public void removePair(String pairId) {
        MatchPair pair = pairsById.remove(pairId);
        if (pair != null) {
            pairIdByAnonId.remove(pair.getAnonIdA());
            pairIdByAnonId.remove(pair.getAnonIdB());
        }
    }
}
