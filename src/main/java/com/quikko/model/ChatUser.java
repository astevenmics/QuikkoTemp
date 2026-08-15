package com.quikko.model;

import java.time.Instant;
import java.util.Set;

/**
 * Transient (non-persisted) representation of a connected, anonymous chat
 * session. Lives only in the queue store / in-memory registries for the
 * lifetime of the WebSocket connection.
 */
public class ChatUser {

    private final String anonId;
    private final String sessionId;
    private final Set<String> interests;
    private final String ip;
    private final Instant queuedAt;
    private final boolean videoEnabled;

    public ChatUser(String anonId, String sessionId, Set<String> interests, String ip, Instant queuedAt,
                    boolean videoEnabled) {
        this.anonId = anonId;
        this.sessionId = sessionId;
        this.interests = interests;
        this.ip = ip;
        this.queuedAt = queuedAt;
        this.videoEnabled = videoEnabled;
    }

    public String getAnonId() {
        return anonId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Set<String> getInterests() {
        return interests;
    }

    public String getIp() {
        return ip;
    }

    public Instant getQueuedAt() {
        return queuedAt;
    }

    public boolean isVideoEnabled() {
        return videoEnabled;
    }
}