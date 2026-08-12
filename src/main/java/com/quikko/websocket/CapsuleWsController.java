package com.quikko.websocket;

import com.quikko.model.dto.CapsuleLeaveRequest;
import com.quikko.model.dto.ServerEvent;
import com.quikko.service.CapsuleService;
import com.quikko.service.SessionMessenger;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.util.Optional;

@Controller
public class CapsuleWsController {

    private final CapsuleService capsuleService;
    private final SessionMessenger messenger;

    public CapsuleWsController(CapsuleService capsuleService, SessionMessenger messenger) {
        this.capsuleService = capsuleService;
        this.messenger = messenger;
    }

    @MessageMapping("/capsule.leave")
    public void leave(@Payload CapsuleLeaveRequest req) {
        if (req.getAnonId() == null || req.getPairId() == null) {
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
