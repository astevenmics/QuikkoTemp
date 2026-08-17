package com.quikko.service;

import com.quikko.config.AppProperties;
import com.quikko.model.Capsule;
import com.quikko.model.dto.CapsuleClaimResponse;
import com.quikko.repository.CapsuleRepository;
import com.quikko.validation.InputValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Optional;

/**
 * Time Capsule: an optional note left for the specific stranger you were
 * just matched with. Double opt-in — it only ever unlocks and is
 * retrievable if BOTH sides left one for each other in that pairing, and
 * only after the 7-day unlock window. No usernames/contact info are ever
 * exchanged; everything is addressed purely by anonymous session id and
 * retrieved purely by a random claim token.
 */
@Service
public class CapsuleService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final CapsuleRepository repository;
    private final AppProperties props;
    private final PairRegistry pairRegistry;

    public CapsuleService(CapsuleRepository repository, AppProperties props, PairRegistry pairRegistry) {
        this.repository = repository;
        this.props = props;
        this.pairRegistry = pairRegistry;
    }

    /**
     * Leaves (or re-returns the existing token for) a capsule addressed to
     * whoever {@code senderAnonId} was just paired with in {@code pairId}.
     * Returns empty if the pairing can no longer be resolved (e.g. too much
     * time has passed since the match ended).
     */
    @Transactional
    public Optional<String> leaveCapsule(String pairId, String senderAnonId, String rawMessage) {
        Optional<String> recipientOpt = pairRegistry.otherOf(pairId, senderAnonId);
        if (recipientOpt.isEmpty()) {
            return Optional.empty();
        }
        int maxLength = props.getCapsule().getMaxLength();
        if (!InputValidator.isValidText(rawMessage, maxLength)) {
            // Blank, too long, or contains control characters — reject
            // outright rather than silently truncating/mutating it.
            return Optional.empty();
        }
        String recipientAnonId = recipientOpt.get();
        String message = rawMessage.trim();

        Optional<Capsule> existing = repository.findBySenderAnonIdAndRecipientAnonIdAndPairId(
                senderAnonId, recipientAnonId, pairId);
        if (existing.isPresent()) {
            return Optional.of(existing.get().getToken());
        }

        Instant now = Instant.now();
        Instant unlockAt = now.plus(java.time.Duration.ofDays(props.getCapsule().getUnlockAfterDays()));
        String token = generateToken();
        Capsule capsule = new Capsule(token, pairId, senderAnonId, recipientAnonId, message, now, unlockAt);
        repository.save(capsule);
        return Optional.of(token);
    }

    @Transactional
    public CapsuleClaimResponse claim(String token) {
        if (token == null || token.isBlank()) {
            return CapsuleClaimResponse.of(CapsuleClaimResponse.Status.NOT_FOUND);
        }
        Optional<Capsule> mineOpt = repository.findByToken(token.trim());
        if (mineOpt.isEmpty()) {
            return CapsuleClaimResponse.of(CapsuleClaimResponse.Status.NOT_FOUND);
        }
        Capsule mine = mineOpt.get();

        Optional<Capsule> reciprocalOpt = repository.findBySenderAnonIdAndRecipientAnonIdAndPairId(
                mine.getRecipientAnonId(), mine.getSenderAnonId(), mine.getPairId());

        Instant now = Instant.now();

        if (reciprocalOpt.isEmpty()) {
            if (!now.isBefore(mine.getUnlockAt())) {
                return CapsuleClaimResponse.of(CapsuleClaimResponse.Status.EXPIRED);
            }
            return CapsuleClaimResponse.of(CapsuleClaimResponse.Status.PENDING)
                    .unlockAt(DateTimeFormatter.ISO_INSTANT.format(mine.getUnlockAt()));
        }

        Capsule reciprocal = reciprocalOpt.get();
        Instant unlockTime = mine.getUnlockAt().isAfter(reciprocal.getUnlockAt())
                ? mine.getUnlockAt() : reciprocal.getUnlockAt();

        if (now.isBefore(unlockTime)) {
            return CapsuleClaimResponse.of(CapsuleClaimResponse.Status.PENDING)
                    .unlockAt(DateTimeFormatter.ISO_INSTANT.format(unlockTime));
        }

        if (!reciprocal.isDelivered()) {
            reciprocal.setDelivered(true);
            repository.save(reciprocal);
        }
        return CapsuleClaimResponse.of(CapsuleClaimResponse.Status.UNLOCKED)
                .message(reciprocal.getMessage());
    }

    private String generateToken() {
        byte[] bytes = new byte[18];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
