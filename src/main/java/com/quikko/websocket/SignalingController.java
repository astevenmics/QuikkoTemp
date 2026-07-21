package com.quikko.websocket;

import com.quikko.model.MatchPair;
import com.quikko.model.dto.ServerEvent;
import com.quikko.model.dto.SignalRequest;
import com.quikko.service.MatchingService;
import com.quikko.service.SessionMessenger;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.util.Optional;

/**
 * Relays WebRTC offer/answer/ICE-candidate payloads between the two peers
 * of a live pair. The server never inspects or modifies the SDP/ICE
 * content — it only validates that the sender is actually part of the
 * pairId claimed before forwarding.
 */
@Controller
public class SignalingController {

    private final MatchingService matchingService;
    private final SessionMessenger messenger;

    public SignalingController(MatchingService matchingService, SessionMessenger messenger) {
        this.matchingService = matchingService;
        this.messenger = messenger;
    }

    @MessageMapping("/signal")
    public void signal(@Payload SignalRequest req) {
        if (req.getAnonId() == null || req.getPairId() == null) {
            return;
        }
        Optional<MatchPair> pairOpt = matchingService.currentPair(req.getAnonId());
        if (pairOpt.isEmpty() || !pairOpt.get().getPairId().equals(req.getPairId())) {
            return;
        }
        MatchPair pair = pairOpt.get();
        String partnerId = pair.otherOf(req.getAnonId());
        messenger.send(partnerId, ServerEvent.of(ServerEvent.Type.SIGNAL)
                .pairId(req.getPairId())
                .signalType(req.getSignalType())
                .payload(req.getPayload()));
    }
}
