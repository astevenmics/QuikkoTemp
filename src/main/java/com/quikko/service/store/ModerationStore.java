package com.quikko.service.store;

import java.time.Duration;

/**
 * Backing store for moderation state: per-IP report counters, the temporary
 * IP ban list, and the sliding-window join-rate counters. Same
 * memory/redis split as {@link MatchQueueStore}.
 */
public interface ModerationStore {

    /**
     * Records a report against an IP and returns the new total report count
     * for that IP.
     */
    int recordReport(String ip);

    void ban(String ip, Duration duration);

    boolean isBanned(String ip);

    /**
     * Increments the join-attempt counter for {@code key} within a rolling
     * window and returns the count after incrementing. The counter expires
     * automatically after {@code window}.
     */
    int incrementJoinAttempts(String key, Duration window);
}
