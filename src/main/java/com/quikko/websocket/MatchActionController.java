package com.quikko.websocket;

import com.quikko.model.MatchPair;
import com.quikko.model.dto.MatchActionRequest;
import com.quikko.model.dto.ServerEvent;
import com.quikko.service.MatchingService;
import com.quikko.service.RateLimiterService;
import com.quikko.service.ReportService;
import com.quikko.service.SessionMessenger;
import com.quikko.validation.InputValidator;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.util.Optional;

/**
 * Skip (move to a new stranger) and Report (flag + disconnect) actions on
 * a live pair.
 */
@Controller
public class MatchActionController {

    private final MatchingService matchingService;
    private final SessionRegistry sessionRegistry;
    private final ReportService reportService;
    private final SessionMessenger messenger;
    private final RateLimiterService rateLimiterService;

    public MatchActionController(MatchingService matchingService, SessionRegistry sessionRegistry,
                                  ReportService reportService, SessionMessenger messenger,
                                  RateLimiterService rateLimiterService) {
        this.matchingService = matchingService;
        this.sessionRegistry = sessionRegistry;
        this.reportService = reportService;
        this.messenger = messenger;
        this.rateLimiterService = rateLimiterService;
    }

    @MessageMapping("/match.skip")
    public void skip(@Payload MatchActionRequest req) {
        if (!InputValidator.isValidAnonId(req.getAnonId()) || !InputValidator.isValidPairId(req.getPairId())) {
            return;
        }
        if (!rateLimiterService.allowSkip(sessionRegistry.ipOf(req.getAnonId()))) {
            messenger.send(req.getAnonId(), ServerEvent.of(ServerEvent.Type.RATE_LIMITED)
                    .message("You're skipping too quickly — slow down a bit."));
            return;
        }
        matchingService.endMatch(req.getAnonId(), req.getPairId(), ServerEvent.Type.PARTNER_SKIPPED);
    }

    @MessageMapping("/report")
    public void report(@Payload MatchActionRequest req) {
        if (!InputValidator.isValidAnonId(req.getAnonId()) || !InputValidator.isValidPairId(req.getPairId())) {
            return;
        }
        if (!rateLimiterService.allowReport(sessionRegistry.ipOf(req.getAnonId()))) {
            messenger.send(req.getAnonId(), ServerEvent.of(ServerEvent.Type.RATE_LIMITED)
                    .message("Too many reports — slow down a bit."));
            return;
        }
        Optional<MatchPair> pairOpt = matchingService.currentPair(req.getAnonId());
        if (pairOpt.isEmpty() || !pairOpt.get().getPairId().equals(req.getPairId())) {
            return;
        }
        MatchPair pair = pairOpt.get();
        String partnerId = pair.otherOf(req.getAnonId());
        String partnerIp = sessionRegistry.ipOf(partnerId);
        reportService.report(partnerIp);

        matchingService.endMatch(req.getAnonId(), req.getPairId(), ServerEvent.Type.PARTNER_LEFT);
        messenger.send(req.getAnonId(), ServerEvent.of(ServerEvent.Type.REPORT_ACK).pairId(req.getPairId()));
    }
}
