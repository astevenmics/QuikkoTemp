package com.quikko.service;

import com.quikko.model.dto.ServerEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Sends a {@link ServerEvent} to a specific anonymous session's private
 * topic. There is no Spring Security Principal in this app (no accounts),
 * so per-user targeting is done by topic name instead of
 * {@code convertAndSendToUser}.
 */
@Service
public class SessionMessenger {

    private static final String SESSION_TOPIC_PREFIX = "/topic/session/";

    private final SimpMessagingTemplate messagingTemplate;

    public SessionMessenger(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void send(String anonId, ServerEvent event) {
        messagingTemplate.convertAndSend(SESSION_TOPIC_PREFIX + anonId, event);
    }
}
