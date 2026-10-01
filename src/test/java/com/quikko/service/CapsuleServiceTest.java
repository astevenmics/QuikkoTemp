package com.quikko.service;

import com.quikko.config.AppProperties;
import com.quikko.model.Capsule;
import com.quikko.model.dto.CapsuleClaimResponse;
import com.quikko.repository.CapsuleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CapsuleServiceTest {

    private CapsuleRepository repository;
    private PairRegistry pairRegistry;
    private CapsuleService capsuleService;

    @BeforeEach
    void setUp() {
        repository = mock(CapsuleRepository.class);
        AppProperties props = new AppProperties();
        pairRegistry = new PairRegistry();
        capsuleService = new CapsuleService(repository, props, pairRegistry);
    }

    @Test
    void leaveCapsule_withNoKnownPairing_returnsEmptyAndNeverSaves() {
        Optional<String> token = capsuleService.leaveCapsule("unknown-pair", "alice", "hi there");

        assertThat(token).isEmpty();
        verify(repository, never()).save(any());
    }

    @Test
    void leaveCapsule_rejectsBlankAndOversizedMessagesWithoutSaving() {
        pairRegistry.record("pair-1", "alice", "bob");

        assertThat(capsuleService.leaveCapsule("pair-1", "alice", "")).isEmpty();
        assertThat(capsuleService.leaveCapsule("pair-1", "alice", "x".repeat(281))).isEmpty();
        verify(repository, never()).save(any());
    }

    @Test
    void leaveCapsule_whenAlreadyLeftForThisPair_returnsExistingTokenWithoutSavingAgain() {
        pairRegistry.record("pair-1", "alice", "bob");
        Capsule existing = new Capsule("existing-token-1234567890", "pair-1", "alice", "bob",
                "hi", Instant.now(), Instant.now().plusSeconds(60));
        when(repository.findBySenderAnonIdAndRecipientAnonIdAndPairId("alice", "bob", "pair-1"))
                .thenReturn(Optional.of(existing));

        Optional<String> token = capsuleService.leaveCapsule("pair-1", "alice", "a different message");

        assertThat(token).contains("existing-token-1234567890");
        verify(repository, never()).save(any());
    }

    @Test
    void leaveCapsule_withAValidNewMessage_savesAndReturnsAToken() {
        pairRegistry.record("pair-1", "alice", "bob");
        when(repository.findBySenderAnonIdAndRecipientAnonIdAndPairId("alice", "bob", "pair-1"))
                .thenReturn(Optional.empty());

        Optional<String> token = capsuleService.leaveCapsule("pair-1", "alice", "great chat!");

        assertThat(token).isPresent();
        verify(repository).save(any(Capsule.class));
    }

    @Test
    void claim_withAnUnknownToken_returnsNotFound() {
        when(repository.findByToken("nope")).thenReturn(Optional.empty());

        CapsuleClaimResponse response = capsuleService.claim("nope");

        assertThat(response.getStatus()).isEqualTo(CapsuleClaimResponse.Status.NOT_FOUND);
    }

    @Test
    void claim_withNoReciprocalCapsuleAndUnlockWindowStillOpen_isPending() {
        Capsule mine = new Capsule("tok-a", "pair-1", "alice", "bob", "hi",
                Instant.now(), Instant.now().plusSeconds(600));
        when(repository.findByToken("tok-a")).thenReturn(Optional.of(mine));
        when(repository.findBySenderAnonIdAndRecipientAnonIdAndPairId("bob", "alice", "pair-1"))
                .thenReturn(Optional.empty());

        CapsuleClaimResponse response = capsuleService.claim("tok-a");

        assertThat(response.getStatus()).isEqualTo(CapsuleClaimResponse.Status.PENDING);
    }

    @Test
    void claim_withNoReciprocalCapsuleAndUnlockWindowPassed_isExpired() {
        Capsule mine = new Capsule("tok-a", "pair-1", "alice", "bob", "hi",
                Instant.now().minusSeconds(1000), Instant.now().minusSeconds(1));
        when(repository.findByToken("tok-a")).thenReturn(Optional.of(mine));
        when(repository.findBySenderAnonIdAndRecipientAnonIdAndPairId("bob", "alice", "pair-1"))
                .thenReturn(Optional.empty());

        CapsuleClaimResponse response = capsuleService.claim("tok-a");

        assertThat(response.getStatus()).isEqualTo(CapsuleClaimResponse.Status.EXPIRED);
    }

    @Test
    void claim_withBothSidesLeftButUnlockWindowNotYetPassed_isPending() {
        Instant future = Instant.now().plusSeconds(600);
        Capsule mine = new Capsule("tok-a", "pair-1", "alice", "bob", "hi from alice", Instant.now(), future);
        Capsule reciprocal = new Capsule("tok-b", "pair-1", "bob", "alice", "hi from bob", Instant.now(), future);
        when(repository.findByToken("tok-a")).thenReturn(Optional.of(mine));
        when(repository.findBySenderAnonIdAndRecipientAnonIdAndPairId("bob", "alice", "pair-1"))
                .thenReturn(Optional.of(reciprocal));

        CapsuleClaimResponse response = capsuleService.claim("tok-a");

        assertThat(response.getStatus()).isEqualTo(CapsuleClaimResponse.Status.PENDING);
        verify(repository, never()).save(any());
    }

    @Test
    void claim_withBothSidesLeftAndUnlockWindowPassed_unlocksWithThePartnersMessage() {
        Instant past = Instant.now().minusSeconds(1);
        Capsule mine = new Capsule("tok-a", "pair-1", "alice", "bob", "hi from alice",
                Instant.now().minusSeconds(1000), past);
        Capsule reciprocal = new Capsule("tok-b", "pair-1", "bob", "alice", "hi from bob",
                Instant.now().minusSeconds(1000), past);
        when(repository.findByToken("tok-a")).thenReturn(Optional.of(mine));
        when(repository.findBySenderAnonIdAndRecipientAnonIdAndPairId("bob", "alice", "pair-1"))
                .thenReturn(Optional.of(reciprocal));

        CapsuleClaimResponse response = capsuleService.claim("tok-a");

        assertThat(response.getStatus()).isEqualTo(CapsuleClaimResponse.Status.UNLOCKED);
        assertThat(response.getMessage()).isEqualTo("hi from bob");
        assertThat(reciprocal.isDelivered()).isTrue();
        verify(repository).save(reciprocal);
    }
}
