package com.quikko.websocket;

import com.quikko.model.dto.CapsuleLeaveRequest;
import com.quikko.model.dto.ServerEvent;
import com.quikko.service.CapsuleService;
import com.quikko.service.RateLimiterService;
import com.quikko.service.SessionMessenger;
import com.quikko.validation.InputValidator;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.util.Optional;

@Controller
public class CapsuleWsController {

    private final CapsuleService capsuleService;
    private final SessionMessenger messenger;
    private final SessionRegistry sessionRegistry;
    private final RateLimiterService rateLimiterService;

    public CapsuleWsController(CapsuleService capsuleService, SessionMessenger messenger,
                                SessionRegistry sessionRegistry, RateLimiterService rateLimiterService) {
        this.capsuleService = capsuleService;
        this.messenger = messenger;
        this.sessionRegistry = sessionRegistry;
        this.rateLimiterService = rateLimiterService;
    }

    @MessageMapping("/capsule.leave")
    public void leave(@Payload CapsuleLeaveRequest req) {
        if (!InputValidator.isValidAnonId(req.getAnonId()) || !InputValidator.isValidPairId(req.getPairId())) {
            return;
        }
        if (!rateLimiterService.allowCapsuleLeave(sessionRegistry.ipOf(req.getAnonId()))) {
            messenger.send(req.getAnonId(), ServerEvent.of(ServerEvent.Type.RATE_LIMITED)
                    .message("Too many capsule attempts — slow down a bit."));
            return;
        }
        Optional<String> token = capsuleService.leaveCapsule(req.getPairId(), req.getAnonId(), req.getMessage());
        if (token.isPresent()) {
            messenger.send(req.getAnonId(), ServerEvent.of(ServerEvent.Type.CAPSULE_SAVED)
                    .pairId(req.getPairId())
                    .token(token.get()));
        } else {
            messenger.send(req.getAnonId(), ServerEvent.of(ServerEvent.Type.ERROR)
                    .message("Couldn't save your Time Capsule — the pairing may have expired."));
        }
    }
}
