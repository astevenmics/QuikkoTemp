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
        private String queueStore = "memory";
        private int fallbackAfterSeconds = 7;
        private long pollIntervalMs = 1000;

        public String getQueueStore() {
            return queueStore;
        }

        public void setQueueStore(String queueStore) {
            this.queueStore = queueStore;
        }

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
    }

    public static class Webrtc {
        private String stunUrls = "stun:stun.l.google.com:19302";
        private String turnUrl = "";
        private String turnUsername = "";
        private String turnCredential = "";

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

        public String getTurnUsername() {
            return turnUsername;
        }

        public void setTurnUsername(String turnUsername) {
            this.turnUsername = turnUsername;
        }

        public String getTurnCredential() {
            return turnCredential;
        }

        public void setTurnCredential(String turnCredential) {
            this.turnCredential = turnCredential;
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
