package com.quikko.websocket;

import com.quikko.model.MatchPair;
import com.quikko.model.dto.MatchActionRequest;
import com.quikko.model.dto.ServerEvent;
import com.quikko.service.MatchingService;
import com.quikko.service.ReportService;
import com.quikko.service.SessionMessenger;
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

    public MatchActionController(MatchingService matchingService, SessionRegistry sessionRegistry,
                                  ReportService reportService, SessionMessenger messenger) {
        this.matchingService = matchingService;
        this.sessionRegistry = sessionRegistry;
        this.reportService = reportService;
        this.messenger = messenger;
    }

    @MessageMapping("/match.skip")
    public void skip(@Payload MatchActionRequest req) {
        if (req.getAnonId() == null || req.getPairId() == null) {
            return;
        }
        matchingService.endMatch(req.getAnonId(), req.getPairId(), ServerEvent.Type.PARTNER_SKIPPED);
    }

    @MessageMapping("/report")
    public void report(@Payload MatchActionRequest req) {
        if (req.getAnonId() == null || req.getPairId() == null) {
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
