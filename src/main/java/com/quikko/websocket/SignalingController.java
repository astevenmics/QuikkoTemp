package com.quikko.websocket;

import com.quikko.model.MatchPair;
import com.quikko.model.dto.ServerEvent;
import com.quikko.model.dto.SignalRequest;
import com.quikko.service.MatchingService;
import com.quikko.service.SessionMessenger;
import com.quikko.validation.InputValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.util.Optional;

/**
 * Relays WebRTC offer/answer/ICE-candidate payloads between the two peers
 * of a live pair. The server never inspects the SDP/ICE content itself
 * beyond checking that signalType is one of the three expected values and
 * the sender is actually part of the pairId claimed before forwarding.
 */
@Controller
public class SignalingController {

    private static final Logger log = LoggerFactory.getLogger(SignalingController.class);

    // Real SDP offers/answers and ICE candidates are at most a few KB; this
    // is a generous ceiling meant only to stop a client from abusing the
    // relay to push arbitrarily large payloads.
    private static final int MAX_PAYLOAD_CHARS = 32_000;

    private final MatchingService matchingService;
    private final SessionMessenger messenger;

    public SignalingController(MatchingService matchingService, SessionMessenger messenger) {
        this.matchingService = matchingService;
        this.messenger = messenger;
    }

    @MessageMapping("/signal")
    public void signal(@Payload SignalRequest req) {
        if (!InputValidator.isValidAnonId(req.getAnonId()) || !InputValidator.isValidPairId(req.getPairId())) {
            return;
        }
        if (!InputValidator.isValidSignalType(req.getSignalType())) {
            log.warn("Dropped /signal message with unrecognized signalType from {}", req.getAnonId());
            return;
        }
        if (req.getPayload() == null || req.getPayload().toString().length() > MAX_PAYLOAD_CHARS) {
            log.warn("Dropped oversized/empty /signal payload from {}", req.getAnonId());
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
