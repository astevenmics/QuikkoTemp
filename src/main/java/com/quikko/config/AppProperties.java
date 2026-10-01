package com.quikko.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Root binder for the {@code quikko.*} configuration tree in application.yml.
 */
@ConfigurationProperties(prefix = "quikko")
public class AppProperties {

    private final Matching matching = new Matching();
    private final Moderation moderation = new Moderation();
    private final RateLimit rateLimit = new RateLimit();
    private final Security security = new Security();
    private final Webrtc webrtc = new Webrtc();
    private final Interests interests = new Interests();
    private final Capsule capsule = new Capsule();

    public Matching getMatching() {
        return matching;
    }

    public Moderation getModeration() {
        return moderation;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public Security getSecurity() {
        return security;
    }

    public Webrtc getWebrtc() {
        return webrtc;
    }

    public Interests getInterests() {
        return interests;
    }

    public Capsule getCapsule() {
        return capsule;
    }

    public static class Matching {
        private int fallbackAfterSeconds = 7;
        private long pollIntervalMs = 1000;

        public int getFallbackAfterSeconds() {
            return fallbackAfterSeconds;
        }

        public void setFallbackAfterSeconds(int fallbackAfterSeconds) {
            this.fallbackAfterSeconds = fallbackAfterSeconds;
        }

        public long getPollIntervalMs() {
            return pollIntervalMs;
        }

        public void setPollIntervalMs(long pollIntervalMs) {
            this.pollIntervalMs = pollIntervalMs;
        }
    }

    public static class Moderation {
        private int reportBanThreshold = 5;
        private int banDurationMinutes = 60;
        private String profanityWords = "";

        public int getReportBanThreshold() {
            return reportBanThreshold;
        }

        public void setReportBanThreshold(int reportBanThreshold) {
            this.reportBanThreshold = reportBanThreshold;
        }

        public int getBanDurationMinutes() {
            return banDurationMinutes;
        }

        public void setBanDurationMinutes(int banDurationMinutes) {
            this.banDurationMinutes = banDurationMinutes;
        }

        public String getProfanityWords() {
            return profanityWords;
        }

        public void setProfanityWords(String profanityWords) {
            this.profanityWords = profanityWords;
        }

        public Set<String> profanitySet() {
            if (profanityWords == null || profanityWords.isBlank()) {
                return Set.of();
            }
            return Arrays.stream(profanityWords.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(String::toLowerCase)
                    .collect(Collectors.toUnmodifiableSet());
        }
    }

    public static class RateLimit {
        private int maxJoinsPerWindow = 10;
        private int windowSeconds = 60;
        private int maxChatMessagesPerWindow = 60;
        private int maxSkipsPerWindow = 30;
        private int maxReportsPerWindow = 10;
        private int maxSignalsPerWindow = 120;
        private int maxCapsuleLeavesPerWindow = 10;
        private int maxApiRequestsPerWindow = 60;

        public int getMaxJoinsPerWindow() {
            return maxJoinsPerWindow;
        }

        public void setMaxJoinsPerWindow(int maxJoinsPerWindow) {
            this.maxJoinsPerWindow = maxJoinsPerWindow;
        }

        public int getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(int windowSeconds) {
            this.windowSeconds = windowSeconds;
        }

        public int getMaxChatMessagesPerWindow() {
            return maxChatMessagesPerWindow;
        }

        public void setMaxChatMessagesPerWindow(int maxChatMessagesPerWindow) {
            this.maxChatMessagesPerWindow = maxChatMessagesPerWindow;
        }

        public int getMaxSkipsPerWindow() {
            return maxSkipsPerWindow;
        }

        public void setMaxSkipsPerWindow(int maxSkipsPerWindow) {
            this.maxSkipsPerWindow = maxSkipsPerWindow;
        }

        public int getMaxReportsPerWindow() {
            return maxReportsPerWindow;
        }

        public void setMaxReportsPerWindow(int maxReportsPerWindow) {
            this.maxReportsPerWindow = maxReportsPerWindow;
        }

        public int getMaxSignalsPerWindow() {
            return maxSignalsPerWindow;
        }

        public void setMaxSignalsPerWindow(int maxSignalsPerWindow) {
            this.maxSignalsPerWindow = maxSignalsPerWindow;
        }

        public int getMaxCapsuleLeavesPerWindow() {
            return maxCapsuleLeavesPerWindow;
        }

        public void setMaxCapsuleLeavesPerWindow(int maxCapsuleLeavesPerWindow) {
            this.maxCapsuleLeavesPerWindow = maxCapsuleLeavesPerWindow;
        }

        public int getMaxApiRequestsPerWindow() {
            return maxApiRequestsPerWindow;
        }

        public void setMaxApiRequestsPerWindow(int maxApiRequestsPerWindow) {
            this.maxApiRequestsPerWindow = maxApiRequestsPerWindow;
        }
    }

    /**
     * Trust boundary settings: which upstream proxies may be trusted to set
     * {@code X-Forwarded-For} (empty = none — always use the raw socket
     * address), and which browser origins may open a WebSocket connection.
     */
    public static class Security {
        private String trustedProxies = "";
        private String allowedOrigins = "";

        public String getTrustedProxies() {
            return trustedProxies;
        }

        public void setTrustedProxies(String trustedProxies) {
            this.trustedProxies = trustedProxies;
        }

        public List<String> trustedProxiesList() {
            return Arrays.stream(trustedProxies.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }

        public String getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(String allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        /**
         * Empty means same-origin only (Spring's own default when no origin
         * patterns are registered at all).
         */
        public List<String> allowedOriginsList() {
            return Arrays.stream(allowedOrigins.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
    }

    public static class Webrtc {
        private String stunUrls = "stun:stun.l.google.com:19302";
        private String turnUrl = "";
        private String turnSecret = "";
        private int turnCredentialTtlSeconds = 600;

        public String getStunUrls() {
            return stunUrls;
        }

        public void setStunUrls(String stunUrls) {
            this.stunUrls = stunUrls;
        }

        public String getTurnUrl() {
            return turnUrl;
        }

        public void setTurnUrl(String turnUrl) {
            this.turnUrl = turnUrl;
        }

        /**
         * Shared secret for coturn's time-limited REST API / use-auth-secret
         * scheme — used to compute a short-lived username/credential pair
         * per request rather than handing out one static, reusable
         * credential to every caller of /api/webrtc/ice-servers.
         */
        public String getTurnSecret() {
            return turnSecret;
        }

        public void setTurnSecret(String turnSecret) {
            this.turnSecret = turnSecret;
        }

        public int getTurnCredentialTtlSeconds() {
            return turnCredentialTtlSeconds;
        }

        public void setTurnCredentialTtlSeconds(int turnCredentialTtlSeconds) {
            this.turnCredentialTtlSeconds = turnCredentialTtlSeconds;
        }

        public List<String> stunList() {
            return Arrays.stream(stunUrls.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
    }

    public static class Interests {
        private String suggested = "";

        public String getSuggested() {
            return suggested;
        }

        public void setSuggested(String suggested) {
            this.suggested = suggested;
        }

        public List<String> suggestedList() {
            return Arrays.stream(suggested.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
        }
    }

    public static class Capsule {
        private int unlockAfterDays = 7;
        private int maxLength = 280;
        private String unlockCheckCron = "0 0 3 * * *";

        public int getUnlockAfterDays() {
            return unlockAfterDays;
        }

        public void setUnlockAfterDays(int unlockAfterDays) {
            this.unlockAfterDays = unlockAfterDays;
        }

        public int getMaxLength() {
            return maxLength;
        }

        public void setMaxLength(int maxLength) {
            this.maxLength = maxLength;
        }

        public String getUnlockCheckCron() {
            return unlockCheckCron;
        }

        public void setUnlockCheckCron(String unlockCheckCron) {
            this.unlockCheckCron = unlockCheckCron;
        }
    }
}
