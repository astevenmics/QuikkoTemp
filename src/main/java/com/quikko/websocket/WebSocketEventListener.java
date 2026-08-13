package com.quikko.websocket;

import com.quikko.model.MatchPair;
import com.quikko.model.dto.ServerEvent;
import com.quikko.service.MatchingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Optional;

/**
 * Cleans up queue/pair state when a browser tab closes or the socket drops without an explicit Stop.
 * Otherwise, the other side would be left waiting forever on a partner who is gone.
 */
@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final SessionRegistry sessionRegistry;
    private final MatchingService matchingService;

    public WebSocketEventListener(SessionRegistry sessionRegistry, MatchingService matchingService) {
        this.sessionRegistry = sessionRegistry;
        this.matchingService = matchingService;
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        String anonId = sessionRegistry.anonIdFor(sessionId);
        sessionRegistry.removeSession(sessionId);
        if (anonId == null) {
            return;
        }
        matchingService.leaveQueue(anonId);
        Optional<MatchPair> pair = matchingService.currentPair(anonId);
        pair.ifPresent(p -> matchingService.endMatch(anonId, p.getPairId(), ServerEvent.Type.PARTNER_LEFT));
        log.debug("Session {} ({}) disconnected", sessionId, anonId);
    }
}
