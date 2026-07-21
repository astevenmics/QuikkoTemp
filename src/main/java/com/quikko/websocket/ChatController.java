package com.quikko.websocket;

import com.quikko.model.MatchPair;
import com.quikko.model.dto.ChatMessageRequest;
import com.quikko.model.dto.ServerEvent;
import com.quikko.service.MatchingService;
import com.quikko.service.ModerationService;
import com.quikko.service.ProfanityFilterService;
import com.quikko.service.SessionMessenger;
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

    public ChatController(MatchingService matchingService, SessionMessenger messenger,
                           ProfanityFilterService profanityFilterService, ModerationService moderationService) {
        this.matchingService = matchingService;
        this.messenger = messenger;
        this.profanityFilterService = profanityFilterService;
        this.moderationService = moderationService;
    }

    @MessageMapping("/chat.send")
    public void send(@Payload ChatMessageRequest req) {
        if (req.getAnonId() == null || req.getPairId() == null || req.getText() == null) {
            return;
        }
        String text = req.getText().trim();
        if (text.isEmpty() || text.length() > MAX_MESSAGE_LENGTH) {
            return;
        }

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
