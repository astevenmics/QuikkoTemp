package com.quikko.service.store;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryModerationStoreTest {

    private final InMemoryModerationStore store = new InMemoryModerationStore();

    @Test
    void incrementAttempts_countsUpWithinTheSameWindow() {
        assertThat(store.incrementAttempts("join:1.2.3.4", Duration.ofMinutes(1))).isEqualTo(1);
        assertThat(store.incrementAttempts("join:1.2.3.4", Duration.ofMinutes(1))).isEqualTo(2);
        assertThat(store.incrementAttempts("join:1.2.3.4", Duration.ofMinutes(1))).isEqualTo(3);
    }

    @Test
    void incrementAttempts_separateBucketKeysDoNotShareCounters() {
        store.incrementAttempts("join:1.2.3.4", Duration.ofMinutes(1));
        store.incrementAttempts("join:1.2.3.4", Duration.ofMinutes(1));

        assertThat(store.incrementAttempts("chat:1.2.3.4", Duration.ofMinutes(1))).isEqualTo(1);
    }

    @Test
    void incrementAttempts_resetsOnceTheWindowExpires() throws InterruptedException {
        store.incrementAttempts("join:5.6.7.8", Duration.ofMillis(50));
        store.incrementAttempts("join:5.6.7.8", Duration.ofMillis(50));

        Thread.sleep(80);

        assertThat(store.incrementAttempts("join:5.6.7.8", Duration.ofMillis(50))).isEqualTo(1);
    }

    @Test
    void ban_marksIpBannedUntilTheDurationElapses() throws InterruptedException {
        store.ban("9.9.9.9", Duration.ofMillis(50));
        assertThat(store.isBanned("9.9.9.9")).isTrue();

        Thread.sleep(80);

        assertThat(store.isBanned("9.9.9.9")).isFalse();
    }

    @Test
    void isBanned_falseForAnIpThatWasNeverBanned() {
        assertThat(store.isBanned("1.1.1.1")).isFalse();
    }

    @Test
    void recordReport_countsUpPerIpIndependently() {
        assertThat(store.recordReport("2.2.2.2")).isEqualTo(1);
        assertThat(store.recordReport("2.2.2.2")).isEqualTo(2);
        assertThat(store.recordReport("3.3.3.3")).isEqualTo(1);
    }
}
