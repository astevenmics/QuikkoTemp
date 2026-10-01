package com.quikko.service;

import com.quikko.config.AppProperties;
import com.quikko.model.MatchPair;
import com.quikko.model.dto.ServerEvent;
import com.quikko.service.store.InMemoryMatchQueueStore;
import com.quikko.service.store.MatchQueueStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class MatchingServiceTest {

    private AppProperties props;
    private SessionMessenger messenger;
    private MatchingService matchingService;

    @BeforeEach
    void setUp() {
        MatchQueueStore store = new InMemoryMatchQueueStore();
        props = new AppProperties();
        messenger = mock(SessionMessenger.class);
        matchingService = new MatchingService(store, props, new IcebreakerService(), messenger, new PairRegistry());
    }

    @Test
    void runMatchingPass_pairsUsersWithSharedInterestImmediatelyEvenWithALongFallbackWindow() {
        props.getMatching().setFallbackAfterSeconds(60);
        matchingService.joinQueue("alice", "sess-a", Set.of("Music"), "1.1.1.1", true);
        matchingService.joinQueue("bob", "sess-b", Set.of("Music", "Movies"), "2.2.2.2", true);

        matchingService.runMatchingPass();

        verify(messenger).send(eq("alice"), argThat(e -> e.getType() == ServerEvent.Type.MATCHED));
        verify(messenger).send(eq("bob"), argThat(e -> e.getType() == ServerEvent.Type.MATCHED));
        assertThat(matchingService.currentPair("alice")).isPresent();
        assertThat(matchingService.currentPair("alice").get().getSharedInterests()).containsExactly("Music");
    }

    @Test
    void runMatchingPass_doesNotPairUsersWithNoSharedInterestBeforeTheFallbackWindowElapses() {
        props.getMatching().setFallbackAfterSeconds(60);
        matchingService.joinQueue("alice", "sess-a", Set.of("Music"), "1.1.1.1", true);
        matchingService.joinQueue("bob", "sess-b", Set.of("Sports"), "2.2.2.2", true);

        matchingService.runMatchingPass();

        verify(messenger, never()).send(any(), argThat(e -> e.getType() == ServerEvent.Type.MATCHED));
        assertThat(matchingService.currentPair("alice")).isEmpty();
        assertThat(matchingService.currentPair("bob")).isEmpty();
    }

    @Test
    void runMatchingPass_pairsUsersWithNoSharedInterestOnceTheFallbackWindowHasElapsed() {
        props.getMatching().setFallbackAfterSeconds(0); // already elapsed the instant they join
        matchingService.joinQueue("alice", "sess-a", Set.of("Music"), "1.1.1.1", true);
        matchingService.joinQueue("bob", "sess-b", Set.of("Sports"), "2.2.2.2", true);

        matchingService.runMatchingPass();

        assertThat(matchingService.currentPair("alice")).isPresent();
        assertThat(matchingService.currentPair("alice").get().getSharedInterests()).isEmpty();
    }

    @Test
    void endMatch_doesNothingAndNotifiesNoOneWhenTheCallerIsNotActuallyAParticipant() {
        props.getMatching().setFallbackAfterSeconds(0);
        matchingService.joinQueue("alice", "sess-a", Set.of(), "1.1.1.1", true);
        matchingService.joinQueue("bob", "sess-b", Set.of(), "2.2.2.2", true);
        matchingService.runMatchingPass();
        String pairId = matchingService.currentPair("alice").orElseThrow().getPairId();

        Optional<MatchPair> result = matchingService.endMatch("mallory", pairId, ServerEvent.Type.PARTNER_LEFT);

        assertThat(result).isEmpty();
        assertThat(matchingService.currentPair("alice")).isPresent(); // pair left untouched
        verify(messenger, never()).send(any(), argThat(e -> e.getType() == ServerEvent.Type.PARTNER_LEFT));
    }

    @Test
    void endMatch_succeedsForAnActualParticipantAndNotifiesThePartner() {
        props.getMatching().setFallbackAfterSeconds(0);
        matchingService.joinQueue("alice", "sess-a", Set.of(), "1.1.1.1", true);
        matchingService.joinQueue("bob", "sess-b", Set.of(), "2.2.2.2", true);
        matchingService.runMatchingPass();
        String pairId = matchingService.currentPair("alice").orElseThrow().getPairId();

        Optional<MatchPair> result = matchingService.endMatch("alice", pairId, ServerEvent.Type.PARTNER_LEFT);

        assertThat(result).isPresent();
        assertThat(matchingService.currentPair("alice")).isEmpty();
        assertThat(matchingService.currentPair("bob")).isEmpty();
        verify(messenger).send(eq("bob"), argThat(e -> e.getType() == ServerEvent.Type.PARTNER_LEFT));
    }
}
