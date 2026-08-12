package com.quikko.service;

/**
 * Extension point for plugging in an external text/video moderation API (e.g. a third-party content-safety service) down the road.
 * The default {@link NoOpModerationService} always allows content through; swap in a real implementation once such a provider is chosen.
 * (annotated {@code @Primary} or selected via a profile/property)
 */
public interface ModerationService {

    ModerationResult checkText(String anonId, String text);

    /**
     * Stubbed hook for future frame-sampling video moderation.
     * Quikko's video is peer-to-peer WebRTC, so the server never sees frames today;
     * this exists purely so a future SFU/relay-based pipeline has a place to plug into.
     */
    ModerationResult checkVideoFrame(String anonId, byte[] frameData);

    enum Verdict {
        ALLOW,
        FLAGGED,
        BLOCKED
    }

    record ModerationResult(Verdict verdict, String reason) {
        public static ModerationResult allow() {
            return new ModerationResult(Verdict.ALLOW, null);
        }
    }
}
