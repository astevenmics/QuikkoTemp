package com.quikko.service;

import com.quikko.config.AppProperties;
import com.quikko.model.ChatUser;
import com.quikko.model.MatchPair;
import com.quikko.model.dto.ServerEvent;
import com.quikko.service.store.MatchQueueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Owns the waiting queue and the pairing algorithm: prefer a partner who shares at least one interest tag;
 * if no such partner is available within {@code quikko.matching.fallback-after-seconds}, pair with anyone waiting.
 */
@Service
public class MatchingService {

    private static final Logger log = LoggerFactory.getLogger(MatchingService.class);

    private final MatchQueueStore store;
    private final AppProperties props;
    private final IcebreakerService icebreakerService;
    private final SessionMessenger messenger;
    private final PairRegistry pairRegistry;

    public MatchingService(MatchQueueStore store, AppProperties props,
                           IcebreakerService icebreakerService, SessionMessenger messenger,
                           PairRegistry pairRegistry) {
        this.store = store;
        this.props = props;
        this.icebreakerService = icebreakerService;
        this.messenger = messenger;
        this.pairRegistry = pairRegistry;
    }

    public void joinQueue(String anonId, String sessionId, Set<String> interests, String ip) {
        if (store.getPairForUser(anonId).isPresent()) {
            return;
        }
        Set<String> normalized = normalize(interests);
        store.enqueue(new ChatUser(anonId, sessionId, normalized, ip, Instant.now()));
        messenger.send(anonId, ServerEvent.of(ServerEvent.Type.WAITING));
    }

    public void leaveQueue(String anonId) {
        store.dequeue(anonId);
    }

    public Optional<MatchPair> currentPair(String anonId) {
        return store.getPairForUser(anonId);
    }

    /**
     * Ends the pair (if the caller is actually part of it), removes it from the store, and notifies the other side.
     * The partner is never auto-requeued here — Skip/Stop/Report handlers decide what happens next for each side individually.
     */
    public Optional<MatchPair> endMatch(String anonId, String pairId, ServerEvent.Type partnerNotice) {
        Optional<MatchPair> pairOpt = store.getPair(pairId);
        if (pairOpt.isEmpty() || !pairOpt.get().contains(anonId)) {
            return Optional.empty();
        }
        MatchPair pair = pairOpt.get();
        store.removePair(pairId);
        String partnerId = pair.otherOf(anonId);
        messenger.send(partnerId, ServerEvent.of(partnerNotice).pairId(pairId));
        return Optional.of(pair);
    }

    private Set<String> normalize(Set<String> interests) {
        if (interests == null) {
            return Set.of();
        }
        Set<String> out = new LinkedHashSet<>();
        for (String i : interests) {
            if (i == null) {
                continue;
            }
            String trimmed = i.trim();
            if (!trimmed.isEmpty() && trimmed.length() <= 40) {
                out.add(trimmed);
            }
            if (out.size() >= 15) {
                break;
            }
        }
        return out;
    }

    @Scheduled(fixedDelayString = "${quikko.matching.poll-interval-ms:1000}")
    void runMatchingPass() {
        List<ChatUser> waiting = new ArrayList<>(store.snapshot());
        if (waiting.size() < 2) {
            return;
        }
        Set<String> matchedThisPass = new HashSet<>();
        int fallbackSeconds = props.getMatching().getFallbackAfterSeconds();
        Instant now = Instant.now();

        for (int i = 0; i < waiting.size(); i++) {
            ChatUser u = waiting.get(i);
            if (matchedThisPass.contains(u.getAnonId()) || !store.isQueued(u.getAnonId())) {
                continue;
            }

            ChatUser best = null;
            Set<String> bestShared = Set.of();
            for (int j = i + 1; j < waiting.size(); j++) {
                ChatUser c = waiting.get(j);
                if (matchedThisPass.contains(c.getAnonId()) || !store.isQueued(c.getAnonId())) {
                    continue;
                }
                Set<String> shared = intersect(u.getInterests(), c.getInterests());
                if (!shared.isEmpty()) {
                    best = c;
                    bestShared = shared;
                    break;
                }
                if (best == null) {
                    best = c; // fallback candidate: earliest other waiting user
                }
            }

            if (best == null) {
                continue;
            }

            boolean waitedLongEnough = Duration.between(u.getQueuedAt(), now).getSeconds() >= fallbackSeconds
                    || Duration.between(best.getQueuedAt(), now).getSeconds() >= fallbackSeconds;

            if (bestShared.isEmpty() && !waitedLongEnough) {
                continue; // hold out a little longer for an interest match
            }

            pair(u, best, bestShared);
            matchedThisPass.add(u.getAnonId());
            matchedThisPass.add(best.getAnonId());
        }
    }

    private void pair(ChatUser a, ChatUser b, Set<String> shared) {
        store.dequeue(a.getAnonId());
        store.dequeue(b.getAnonId());

        String pairId = UUID.randomUUID().toString();
        MatchPair pair = new MatchPair(pairId, a.getAnonId(), b.getAnonId(), shared, Instant.now());
        store.savePair(pair);
        pairRegistry.record(pairId, a.getAnonId(), b.getAnonId());

        String icebreaker = icebreakerService.icebreakerFor(shared);

        messenger.send(a.getAnonId(), ServerEvent.of(ServerEvent.Type.MATCHED)
                .pairId(pairId)
                .partnerInterests(List.copyOf(b.getInterests()))
                .sharedInterests(List.copyOf(shared))
                .icebreaker(icebreaker)
                .initiator(true));

        messenger.send(b.getAnonId(), ServerEvent.of(ServerEvent.Type.MATCHED)
                .pairId(pairId)
                .partnerInterests(List.copyOf(a.getInterests()))
                .sharedInterests(List.copyOf(shared))
                .icebreaker(icebreaker)
                .initiator(false));

        log.debug("Matched {} <-> {} (shared={})", a.getAnonId(), b.getAnonId(), shared);
    }

    private Set<String> intersect(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return Set.of();
        }
        Set<String> aLower = new HashSet<>();
        for (String s : a) {
            aLower.add(s.toLowerCase());
        }
        Set<String> result = new LinkedHashSet<>();
        for (String s : b) {
            if (aLower.contains(s.toLowerCase())) {
                result.add(s);
            }
        }
        return result;
    }
}
