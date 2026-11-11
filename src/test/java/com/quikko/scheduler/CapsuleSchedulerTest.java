package com.quikko.scheduler;

import com.quikko.config.AppProperties;
import com.quikko.repository.CapsuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CapsuleSchedulerTest {

    private CapsuleRepository repository;
    private CapsuleScheduler scheduler;

    @BeforeEach
    void setUp() {
        repository = mock(CapsuleRepository.class);
        AppProperties props = new AppProperties();
        props.getCapsule().setRetentionDays(90);
        scheduler = new CapsuleScheduler(repository, props);
    }

    @Test
    void purgeExpiredCapsules_deletesEverythingCreatedBeforeTheRetentionCutoff() {
        when(repository.deleteByCreatedAtBefore(any(Instant.class))).thenReturn(3L);

        scheduler.purgeExpiredCapsules();

        verify(repository).deleteByCreatedAtBefore(any(Instant.class));
    }

    @Test
    void purgeExpiredCapsules_withNothingToDelete_doesNotThrow() {
        when(repository.deleteByCreatedAtBefore(any(Instant.class))).thenReturn(0L);

        scheduler.purgeExpiredCapsules();

        verify(repository).deleteByCreatedAtBefore(any(Instant.class));
    }
}
