package com.quikko.websocket;

import com.quikko.model.MatchPair;
import com.quikko.model.dto.ChatMessageRequest;
import com.quikko.model.dto.ServerEvent;
import com.quikko.service.MatchingService;
import com.quikko.service.ModerationService;
import com.quikko.service.ProfanityFilterService;
import com.quikko.service.RateLimiterService;
import com.quikko.service.SessionMessenger;
import com.quikko.validation.InputValidator;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.util.Optional;

@Controller
public class ChatController {

    private static final int MAX_MESSAGE_LENGTH = 1000;

    private final MatchingService matchingService;
    private final SessionMessenger messenger;
    private final ProfanityFilterService profanityFilterService;
    private final ModerationService moderationService;
    private final SessionRegistry sessionRegistry;
    private final RateLimiterService rateLimiterService;

    public ChatController(MatchingService matchingService, SessionMessenger messenger,
                           ProfanityFilterService profanityFilterService, ModerationService moderationService,
                           SessionRegistry sessionRegistry, RateLimiterService rateLimiterService) {
        this.matchingService = matchingService;
        this.messenger = messenger;
        this.profanityFilterService = profanityFilterService;
        this.moderationService = moderationService;
        this.sessionRegistry = sessionRegistry;
        this.rateLimiterService = rateLimiterService;
    }

    @MessageMapping("/chat.send")
    public void send(@Payload ChatMessageRequest req) {
        if (!InputValidator.isValidAnonId(req.getAnonId()) || !InputValidator.isValidPairId(req.getPairId())) {
            return;
        }
        if (!rateLimiterService.allowChatMessage(sessionRegistry.ipOf(req.getAnonId()))) {
            messenger.send(req.getAnonId(), ServerEvent.of(ServerEvent.Type.RATE_LIMITED)
                    .message("You're sending messages too quickly — slow down a bit."));
            return;
        }
        if (!InputValidator.isValidText(req.getText(), MAX_MESSAGE_LENGTH)) {
            messenger.send(req.getAnonId(), ServerEvent.of(ServerEvent.Type.ERROR)
                    .message("Message rejected — empty, too long, or contains invalid characters."));
            return;
        }
        String text = req.getText().trim();

        Optional<MatchPair> pairOpt = matchingService.currentPair(req.getAnonId());
        if (pairOpt.isEmpty() || !pairOpt.get().getPairId().equals(req.getPairId())) {
            return;
        }
        MatchPair pair = pairOpt.get();

        ModerationService.ModerationResult result = moderationService.checkText(req.getAnonId(), text);
        if (result.verdict() == ModerationService.Verdict.BLOCKED) {
            return;
        }

        String filtered = profanityFilterService.filter(text);
        String partnerId = pair.otherOf(req.getAnonId());
        messenger.send(partnerId, ServerEvent.of(ServerEvent.Type.CHAT)
                .pairId(req.getPairId())
                .text(filtered));
    }
}
