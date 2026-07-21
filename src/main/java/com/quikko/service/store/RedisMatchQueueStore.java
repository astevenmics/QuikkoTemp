package com.quikko.service.store;

import com.quikko.model.ChatUser;
import com.quikko.model.MatchPair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Redis-backed queue store for multi-instance / production deployments.
 * Activate with the "redis" Spring profile (sets
 * quikko.matching.queue-store=redis).
 */
@Component
@ConditionalOnProperty(prefix = "quikko.matching", name = "queue-store", havingValue = "redis")
public class RedisMatchQueueStore implements MatchQueueStore {

    private static final String QUEUE_ZSET = "quikko:queue:z";
    private static final String QUEUE_USER_PREFIX = "quikko:queue:user:";
    private static final String PAIR_PREFIX = "quikko:pair:";
    private static final String PAIR_OF_PREFIX = "quikko:pairof:";

    private final StringRedisTemplate redis;

    @Autowired
    public RedisMatchQueueStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public void enqueue(ChatUser user) {
        redis.opsForZSet().add(QUEUE_ZSET, user.getAnonId(), user.getQueuedAt().toEpochMilli());
        Map<String, String> h = new HashMap<>();
        h.put("anonId", user.getAnonId());
        h.put("sessionId", nullToEmpty(user.getSessionId()));
        h.put("interests", String.join(",", user.getInterests()));
        h.put("ip", nullToEmpty(user.getIp()));
        h.put("queuedAt", String.valueOf(user.getQueuedAt().toEpochMilli()));
        redis.opsForHash().putAll(QUEUE_USER_PREFIX + user.getAnonId(), h);
    }

    @Override
    public void dequeue(String anonId) {
        redis.opsForZSet().remove(QUEUE_ZSET, anonId);
        redis.delete(QUEUE_USER_PREFIX + anonId);
    }

    @Override
    public boolean isQueued(String anonId) {
        return redis.opsForZSet().score(QUEUE_ZSET, anonId) != null;
    }

    @Override
    public List<ChatUser> snapshot() {
        Set<String> anonIds = redis.opsForZSet().range(QUEUE_ZSET, 0, -1);
        if (anonIds == null || anonIds.isEmpty()) {
            return List.of();
        }
        return anonIds.stream()
                .map(this::loadUser)
                .filter(u -> u != null)
                .collect(Collectors.toList());
    }

    private ChatUser loadUser(String anonId) {
        Map<Object, Object> h = redis.opsForHash().entries(QUEUE_USER_PREFIX + anonId);
        if (h.isEmpty()) {
            return null;
        }
        Set<String> interests = splitToSet((String) h.get("interests"));
        String ip = (String) h.get("ip");
        long queuedAt = Long.parseLong((String) h.get("queuedAt"));
        return new ChatUser(anonId, (String) h.get("sessionId"), interests,
                ip == null || ip.isBlank() ? null : ip, Instant.ofEpochMilli(queuedAt));
    }

    @Override
    public void savePair(MatchPair pair) {
        Map<String, String> h = new HashMap<>();
        h.put("pairId", pair.getPairId());
        h.put("anonIdA", pair.getAnonIdA());
        h.put("anonIdB", pair.getAnonIdB());
        h.put("sharedInterests", String.join(",", pair.getSharedInterests()));
        h.put("createdAt", String.valueOf(pair.getCreatedAt().toEpochMilli()));
        redis.opsForHash().putAll(PAIR_PREFIX + pair.getPairId(), h);
        redis.opsForValue().set(PAIR_OF_PREFIX + pair.getAnonIdA(), pair.getPairId());
        redis.opsForValue().set(PAIR_OF_PREFIX + pair.getAnonIdB(), pair.getPairId());
    }

    @Override
    public Optional<MatchPair> getPairForUser(String anonId) {
        String pairId = redis.opsForValue().get(PAIR_OF_PREFIX + anonId);
        return pairId == null ? Optional.empty() : getPair(pairId);
    }

    @Override
    public Optional<MatchPair> getPair(String pairId) {
        Map<Object, Object> h = redis.opsForHash().entries(PAIR_PREFIX + pairId);
        if (h.isEmpty()) {
            return Optional.empty();
        }
        Set<String> shared = splitToSet((String) h.get("sharedInterests"));
        long createdAt = Long.parseLong((String) h.get("createdAt"));
        return Optional.of(new MatchPair(pairId, (String) h.get("anonIdA"), (String) h.get("anonIdB"),
                shared, Instant.ofEpochMilli(createdAt)));
    }

    @Override
    public void removePair(String pairId) {
        getPair(pairId).ifPresent(pair -> {
            redis.delete(PAIR_OF_PREFIX + pair.getAnonIdA());
            redis.delete(PAIR_OF_PREFIX + pair.getAnonIdB());
        });
        redis.delete(PAIR_PREFIX + pairId);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static Set<String> splitToSet(String csv) {
        if (csv == null || csv.isBlank()) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
