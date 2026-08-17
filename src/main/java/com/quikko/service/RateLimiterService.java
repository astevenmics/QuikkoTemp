package com.quikko.service;

import com.quikko.config.AppProperties;
import com.quikko.service.store.ModerationStore;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Sliding-window rate limiting for every abuse-prone action in the app,
 * keyed by caller IP (never by the client-supplied anonId, which is
 * trivially rotatable). Each action gets its own counter bucket so hitting
 * one limit doesn't affect another.
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
        AppProperties.RateLimit rl = props.getRateLimit();
        return allow("join", ip, rl.getMaxJoinsPerWindow(), rl.getWindowSeconds());
    }

    public boolean allowChatMessage(String ip) {
        AppProperties.RateLimit rl = props.getRateLimit();
        return allow("chat", ip, rl.getMaxChatMessagesPerWindow(), rl.getWindowSeconds());
    }

    public boolean allowSkip(String ip) {
        AppProperties.RateLimit rl = props.getRateLimit();
        return allow("skip", ip, rl.getMaxSkipsPerWindow(), rl.getWindowSeconds());
    }

    public boolean allowReport(String ip) {
        AppProperties.RateLimit rl = props.getRateLimit();
        return allow("report", ip, rl.getMaxReportsPerWindow(), rl.getWindowSeconds());
    }

    public boolean allowSignal(String ip) {
        AppProperties.RateLimit rl = props.getRateLimit();
        return allow("signal", ip, rl.getMaxSignalsPerWindow(), rl.getWindowSeconds());
    }

    public boolean allowCapsuleLeave(String ip) {
        AppProperties.RateLimit rl = props.getRateLimit();
        return allow("capsule", ip, rl.getMaxCapsuleLeavesPerWindow(), rl.getWindowSeconds());
    }

    public boolean allowApiRequest(String ip) {
        AppProperties.RateLimit rl = props.getRateLimit();
        return allow("api", ip, rl.getMaxApiRequestsPerWindow(), rl.getWindowSeconds());
    }

    private boolean allow(String bucket, String ip, int maxPerWindow, int windowSeconds) {
        if (ip == null) {
            return true;
        }
        Duration window = Duration.ofSeconds(windowSeconds);
        int count = store.incrementAttempts(bucket + ":" + ip, window);
        return count <= maxPerWindow;
    }
}
