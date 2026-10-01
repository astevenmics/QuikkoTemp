package com.quikko.service;

import com.quikko.config.AppProperties;
import com.quikko.service.store.InMemoryModerationStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterServiceTest {

    private AppProperties props;
    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        props = new AppProperties();
        props.getRateLimit().setWindowSeconds(60);
        props.getRateLimit().setMaxJoinsPerWindow(3);
        props.getRateLimit().setMaxChatMessagesPerWindow(2);
        props.getRateLimit().setMaxSkipsPerWindow(1);
        rateLimiterService = new RateLimiterService(new InMemoryModerationStore(), props);
    }

    @Test
    void allowJoin_allowsUpToConfiguredMaxThenBlocks() {
        String ip = "1.2.3.4";
        assertThat(rateLimiterService.allowJoin(ip)).isTrue();
        assertThat(rateLimiterService.allowJoin(ip)).isTrue();
        assertThat(rateLimiterService.allowJoin(ip)).isTrue();
        assertThat(rateLimiterService.allowJoin(ip)).isFalse();
    }

    @Test
    void differentActionsForTheSameIpHaveIndependentBudgets() {
        String ip = "5.6.7.8";
        rateLimiterService.allowJoin(ip);
        rateLimiterService.allowJoin(ip);
        rateLimiterService.allowJoin(ip); // join budget (3) now exhausted

        assertThat(rateLimiterService.allowChatMessage(ip)).isTrue();
        assertThat(rateLimiterService.allowChatMessage(ip)).isTrue();
        assertThat(rateLimiterService.allowChatMessage(ip)).isFalse();
    }

    @Test
    void differentIpsHaveIndependentBudgetsForTheSameAction() {
        assertThat(rateLimiterService.allowSkip("1.1.1.1")).isTrue();
        assertThat(rateLimiterService.allowSkip("1.1.1.1")).isFalse();

        assertThat(rateLimiterService.allowSkip("2.2.2.2")).isTrue();
    }

    @Test
    void nullIp_isAlwaysAllowed() {
        // Defensive default for the rare case the caller IP genuinely
        // couldn't be resolved -- can't rate-limit an identity you don't
        // have, and every caller in production always has a real IP.
        for (int i = 0; i < 50; i++) {
            assertThat(rateLimiterService.allowJoin(null)).isTrue();
        }
    }
}
