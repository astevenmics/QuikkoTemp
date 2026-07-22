package com.quikko.service;

import com.quikko.config.AppProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A minimal, dependency-free CAPTCHA: a short-lived arithmetic challenge.
 * It's not meant to stop a determined attacker — it's meant to filter out
 * the trivial, no-JS/no-solve bots that would otherwise flood the matching
 * queue. No external service, API key, or network call required, which
 * keeps the zero-setup local dev story intact.
 *
 * Flow: the client fetches a challenge, solves it (verify), and that
 * challenge id becomes single-use proof passed along with queue.join
 * (consumeIfSolved) — so a bot that skips the landing page entirely and
 * talks to the WebSocket directly still needs a solved challenge id.
 */
@Service
public class CaptchaService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private record Challenge(int answer, Instant expiresAt, boolean solved) {
        Challenge solve() {
            return new Challenge(answer, expiresAt, true);
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    public record ChallengeView(String challengeId, String question) {
    }

    private final AppProperties props;
    private final Map<String, Challenge> challenges = new ConcurrentHashMap<>();

    public CaptchaService(AppProperties props) {
        this.props = props;
    }

    public ChallengeView newChallenge() {
        int a = RANDOM.nextInt(9) + 1;
        int b = RANDOM.nextInt(9) + 1;
        String id = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusSeconds(props.getCaptcha().getTtlSeconds());
        challenges.put(id, new Challenge(a + b, expiresAt, false));
        return new ChallengeView(id, "What is " + a + " + " + b + "?");
    }

    /**
     * Checks the answer and, if correct, marks the challenge solved (but
     * does not consume it yet — that happens once at queue.join time).
     */
    public boolean verify(String challengeId, String answer) {
        if (challengeId == null || answer == null) {
            return false;
        }
        Challenge challenge = challenges.get(challengeId);
        if (challenge == null || challenge.isExpired()) {
            challenges.remove(challengeId);
            return false;
        }
        int parsed;
        try {
            parsed = Integer.parseInt(answer.trim());
        } catch (NumberFormatException e) {
            return false;
        }
        if (parsed != challenge.answer()) {
            challenges.remove(challengeId); // force a fresh challenge, no repeated guessing
            return false;
        }
        challenges.put(challengeId, challenge.solve());
        return true;
    }

    /**
     * One-time consumption of a previously-solved challenge. Returns false
     * (and does nothing) if the challenge was never solved, has expired, or
     * was already consumed — which is exactly what a bot skipping straight
     * to the WebSocket would hit.
     */
    public boolean consumeIfSolved(String challengeId) {
        if (challengeId == null) {
            return false;
        }
        Challenge challenge = challenges.remove(challengeId);
        return challenge != null && challenge.solved() && !challenge.isExpired();
    }

    public boolean isEnabled() {
        return props.getCaptcha().isEnabled();
    }

    @Scheduled(fixedRate = 60_000)
    void cleanup() {
        challenges.entrySet().removeIf(e -> e.getValue().isExpired());
    }
}
