package com.quikko.service.store;

import java.time.Duration;

/**
 * Backing store for moderation state: per-IP report counters, the temporary
 * IP ban list, and sliding-window rate-limit counters (join attempts and
 * every other rate-limited action).
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
     * Increments the rate-limit counter for {@code key} within a rolling
     * window and returns the count after incrementing. The counter expires
     * automatically after {@code window}. {@code key} is expected to already
     * include a bucket prefix (e.g. {@code "join:1.2.3.4"}) so different
     * rate-limited actions don't share counters.
     */
    int incrementAttempts(String key, Duration window);
}
