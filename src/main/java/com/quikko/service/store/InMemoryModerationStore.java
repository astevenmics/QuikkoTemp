package com.quikko.service.store;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class InMemoryModerationStore implements ModerationStore {

    private final ConcurrentHashMap<String, AtomicInteger> reportCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Instant> bannedUntil = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Window> joinWindows = new ConcurrentHashMap<>();

    @Override
    public int recordReport(String ip) {
        if (ip == null) {
            return 0;
        }
        return reportCounts.computeIfAbsent(ip, k -> new AtomicInteger()).incrementAndGet();
    }

    @Override
    public void ban(String ip, Duration duration) {
        if (ip == null) {
            return;
        }
        bannedUntil.put(ip, Instant.now().plus(duration));
    }

    @Override
    public boolean isBanned(String ip) {
        if (ip == null) {
            return false;
        }
        Instant until = bannedUntil.get(ip);
        if (until == null) {
            return false;
        }
        if (until.isBefore(Instant.now())) {
            bannedUntil.remove(ip);
            return false;
        }
        return true;
    }

    @Override
    public int incrementJoinAttempts(String key, Duration window) {
        Instant now = Instant.now();
        Window w = joinWindows.compute(key, (k, existing) -> {
            if (existing == null || existing.resetAt.isBefore(now)) {
                return new Window(now.plus(window), new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });
        return w.count.get();
    }

    @Scheduled(fixedRate = 60_000)
    void cleanup() {
        Instant now = Instant.now();
        bannedUntil.entrySet().removeIf(e -> e.getValue().isBefore(now));
        joinWindows.entrySet().removeIf(e -> e.getValue().resetAt.isBefore(now));
    }

    private static final class Window {
        final Instant resetAt;
        final AtomicInteger count;

        Window(Instant resetAt, AtomicInteger count) {
            this.resetAt = resetAt;
            this.count = count;
        }
    }
}
