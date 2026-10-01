package com.quikko.websocket;

import com.quikko.model.dto.MatchActionRequest;
import com.quikko.model.dto.ServerEvent;
import com.quikko.service.MatchingService;
import com.quikko.service.RateLimiterService;
import com.quikko.service.ReportService;
import com.quikko.service.SessionMessenger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Covers the session-to-anonId binding fix: a STOMP frame's claimed anonId
 * must match the anonId that queue.join established for that WS session,
 * not be trusted at face value.
 */
class MatchActionControllerTest {

    private MatchingService matchingService;
    private SessionRegistry sessionRegistry;
    private RateLimiterService rateLimiterService;
    private MatchActionController controller;

    @BeforeEach
    void setUp() {
        matchingService = mock(MatchingService.class);
        sessionRegistry = new SessionRegistry();
        ReportService reportService = mock(ReportService.class);
        SessionMessenger messenger = mock(SessionMessenger.class);
        rateLimiterService = mock(RateLimiterService.class);
        when(rateLimiterService.allowSkip(any())).thenReturn(true);
        controller = new MatchActionController(matchingService, sessionRegistry, reportService, messenger,
                rateLimiterService);
    }

    private SimpMessageHeaderAccessor headerFor(String sessionId) {
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create();
        accessor.setSessionId(sessionId);
        return accessor;
    }

    @Test
    void skip_withAnonIdMatchingTheSessionsRegisteredBinding_isProcessed() {
        sessionRegistry.register("sess-1", "alice", "1.2.3.4");
        MatchActionRequest req = new MatchActionRequest();
        req.setAnonId("alice");
        req.setPairId("11111111-1111-1111-1111-111111111111");

        controller.skip(req, headerFor("sess-1"));

        verify(matchingService).endMatch("alice", req.getPairId(), ServerEvent.Type.PARTNER_SKIPPED);
    }

    @Test
    void skip_withAnonIdNotMatchingTheSessionsRegisteredBinding_isSilentlyRejected() {
        // This WS connection's session established anonId "alice" at
        // queue.join time -- it must not be able to act as "mallory" just
        // by putting that in the payload.
        sessionRegistry.register("sess-1", "alice", "1.2.3.4");
        MatchActionRequest req = new MatchActionRequest();
        req.setAnonId("mallory");
        req.setPairId("11111111-1111-1111-1111-111111111111");

        controller.skip(req, headerFor("sess-1"));

        verifyNoInteractions(matchingService);
    }

    @Test
    void skip_whenTheSessionNeverJoinedTheQueue_isSilentlyRejected() {
        MatchActionRequest req = new MatchActionRequest();
        req.setAnonId("alice");
        req.setPairId("11111111-1111-1111-1111-111111111111");

        controller.skip(req, headerFor("never-registered-session"));

        verifyNoInteractions(matchingService);
    }
}
