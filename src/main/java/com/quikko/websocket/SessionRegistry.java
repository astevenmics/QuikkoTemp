package com.quikko.websocket;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the mapping between a raw STOMP session id and the anonymous id
 * the client announced on it, plus the IP address captured at handshake
 * time. Populated on queue.join, consulted on disconnect and on Report.
 */
@Component
public class SessionRegistry {

    private final Map<String, String> sessionIdToAnonId = new ConcurrentHashMap<>();
    private final Map<String, String> anonIdToIp = new ConcurrentHashMap<>();
    private final Set<String> captchaVerifiedSessions = ConcurrentHashMap.newKeySet();

    public void register(String sessionId, String anonId, String ip) {
        sessionIdToAnonId.put(sessionId, anonId);
        if (ip != null) {
            anonIdToIp.put(anonId, ip);
        }
    }

    public String anonIdFor(String sessionId) {
        return sessionIdToAnonId.get(sessionId);
    }

    public String ipOf(String anonId) {
        return anonIdToIp.get(anonId);
    }

    /**
     * A session only needs to clear the CAPTCHA once (on its first
     * queue.join) — Skip re-joins the queue on the same STOMP connection
     * and shouldn't have to solve it again.
     */
    public boolean isCaptchaVerified(String sessionId) {
        return captchaVerifiedSessions.contains(sessionId);
    }

    public void markCaptchaVerified(String sessionId) {
        captchaVerifiedSessions.add(sessionId);
    }

    public void removeSession(String sessionId) {
        String anonId = sessionIdToAnonId.remove(sessionId);
        if (anonId != null) {
            anonIdToIp.remove(anonId);
        }
        captchaVerifiedSessions.remove(sessionId);
    }
}
