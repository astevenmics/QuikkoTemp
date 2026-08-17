package com.quikko.websocket;

import com.quikko.model.MatchPair;
import com.quikko.model.dto.MatchActionRequest;
import com.quikko.model.dto.QueueJoinRequest;
import com.quikko.model.dto.ServerEvent;
import com.quikko.service.MatchingService;
import com.quikko.service.RateLimiterService;
import com.quikko.service.ReportService;
import com.quikko.service.SessionMessenger;
import com.quikko.validation.InputValidator;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.LinkedHashSet;
import java.util.List;
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
        if (!InputValidator.isValidAnonId(req.getAnonId())) {
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

        Set<String> interests = validInterests(req.getInterests());
        boolean videoEnabled = req.getVideoEnabled() == null || req.getVideoEnabled();
        matchingService.joinQueue(req.getAnonId(), sessionId, interests, ip, videoEnabled);
    }

    @MessageMapping("/queue.leave")
    public void leave(@Payload MatchActionRequest req) {
        if (!InputValidator.isValidAnonId(req.getAnonId())) {
            return;
        }
        matchingService.leaveQueue(req.getAnonId());
        if (req.getPairId() != null && InputValidator.isValidPairId(req.getPairId())) {
            Optional<MatchPair> pair = matchingService.currentPair(req.getAnonId());
            if (pair.isPresent() && pair.get().getPairId().equals(req.getPairId())) {
                matchingService.endMatch(req.getAnonId(), req.getPairId(), ServerEvent.Type.PARTNER_LEFT);
            }
        }
    }

    /**
     * Silently drops any tag that fails the strict interest charset/length
     * check rather than rejecting the whole join — a single malformed tag
     * shouldn't block someone from entering the queue. MatchingService caps
     * the resulting set size again as defense-in-depth.
     */
    private Set<String> validInterests(List<String> raw) {
        if (raw == null) {
            return Set.of();
        }
        Set<String> valid = new LinkedHashSet<>();
        for (String tag : raw) {
            if (tag == null) {
                continue;
            }
            String trimmed = tag.trim();
            if (InputValidator.isValidInterest(trimmed)) {
                valid.add(trimmed);
            }
        }
        return valid;
    }

    private String ip(SimpMessageHeaderAccessor headerAccessor) {
        if (headerAccessor.getSessionAttributes() == null) {
            return null;
        }
        Object ip = headerAccessor.getSessionAttributes().get(ClientIpHandshakeInterceptor.IP_ATTRIBUTE);
        return ip == null ? null : ip.toString();
    }
}
