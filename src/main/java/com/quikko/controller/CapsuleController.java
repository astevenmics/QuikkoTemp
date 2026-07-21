package com.quikko.controller;

import com.quikko.model.dto.CapsuleClaimResponse;
import com.quikko.service.CapsuleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST retrieval endpoint for the /capsule claim page. Leaving a capsule
 * happens over the WebSocket connection at the end of a chat (see
 * {@code com.quikko.websocket.CapsuleWsController}) since that's when the
 * pairing context is still known; claiming happens independently, any time
 * after, via this REST endpoint and the token the user was given.
 */
@RestController
public class CapsuleController {

    private final CapsuleService capsuleService;

    public CapsuleController(CapsuleService capsuleService) {
        this.capsuleService = capsuleService;
    }

    @GetMapping("/api/capsules/claim")
    public CapsuleClaimResponse claim(@RequestParam String token) {
        return capsuleService.claim(token);
    }
}
