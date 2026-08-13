package com.quikko.websocket;

import com.quikko.model.MatchPair;
import com.quikko.model.dto.MatchActionRequest;
import com.quikko.model.dto.QueueJoinRequest;
import com.quikko.model.dto.ServerEvent;
import com.quikko.service.MatchingService;
import com.quikko.service.RateLimiterService;
import com.quikko.service.ReportService;
import com.quikko.service.SessionMessenger;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

@Controller
public class QueueController {

    private final MatchingService matchingService;
    private final SessionRegistry sessionRegistry;
    private final RateLimiterService rateLimiterService;
    private final ReportService reportService;
    private final SessionMessenger messenger;

    public QueueController(MatchingService matchingService, SessionRegistry sessionRegistry,
                           RateLimiterService rateLimiterService, ReportService reportService,
                           SessionMessenger messenger) {
        this.matchingService = matchingService;
        this.sessionRegistry = sessionRegistry;
        this.rateLimiterService = rateLimiterService;
        this.reportService = reportService;
        this.messenger = messenger;
    }

    @MessageMapping("/queue.join")
    public void join(@Payload QueueJoinRequest req, SimpMessageHeaderAccessor headerAccessor) {
        if (req.getAnonId() == null || req.getAnonId().isBlank()) {
            return;
        }
        String ip = ip(headerAccessor);
        String sessionId = headerAccessor.getSessionId();
        sessionRegistry.register(sessionId, req.getAnonId(), ip);

        if (reportService.isBanned(ip)) {
            messenger.send(req.getAnonId(), ServerEvent.of(ServerEvent.Type.BANNED)
                    .message("You've been temporarily blocked due to reports. Try again later."));
            return;
        }
        if (!rateLimiterService.allowJoin(ip)) {
            messenger.send(req.getAnonId(), ServerEvent.of(ServerEvent.Type.RATE_LIMITED)
                    .message("Too many attempts — please slow down."));
            return;
        }

        Set<String> interests = req.getInterests() == null ? Set.of() : new LinkedHashSet<>(req.getInterests());
        matchingService.joinQueue(req.getAnonId(), sessionId, interests, ip);
    }

    @MessageMapping("/queue.leave")
    public void leave(@Payload MatchActionRequest req) {
        if (req.getAnonId() == null) {
            return;
        }
        matchingService.leaveQueue(req.getAnonId());
        if (req.getPairId() != null) {
            Optional<MatchPair> pair = matchingService.currentPair(req.getAnonId());
            if (pair.isPresent() && pair.get().getPairId().equals(req.getPairId())) {
                matchingService.endMatch(req.getAnonId(), req.getPairId(), ServerEvent.Type.PARTNER_LEFT);
            }
        }
    }

    private String ip(SimpMessageHeaderAccessor headerAccessor) {
        if (headerAccessor.getSessionAttributes() == null) {
            return null;
        }
        Object ip = headerAccessor.getSessionAttributes().get(ClientIpHandshakeInterceptor.IP_ATTRIBUTE);
        return ip == null ? null : ip.toString();
    }
}
