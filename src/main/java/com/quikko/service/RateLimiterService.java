package com.quikko.service;

import com.quikko.config.AppProperties;
import com.quikko.service.store.ModerationStore;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Basic sliding-window rate limiting on queue-join requests, keyed by IP.
 */
@Service
public class RateLimiterService {

    private final ModerationStore store;
    private final AppProperties props;

    public RateLimiterService(ModerationStore store, AppProperties props) {
        this.store = store;
        this.props = props;
    }

    public boolean allowJoin(String ip) {
        if (ip == null) {
            return true;
        }
        Duration window = Duration.ofSeconds(props.getRateLimit().getWindowSeconds());
        int count = store.incrementJoinAttempts(ip, window);
        return count <= props.getRateLimit().getMaxJoinsPerWindow();
    }
}
