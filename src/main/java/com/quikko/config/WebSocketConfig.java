package com.quikko.config;

import com.quikko.service.ReportService;
import com.quikko.websocket.ClientIpHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP-over-WebSocket wiring. Clients connect anonymously (no Principal/login);
 * per-user targeting is done via a random session id known only to that browser
 * tab, published on {@code /topic/session/{anonId}}.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final ReportService reportService;

    public WebSocketConfig(ReportService reportService) {
        this.reportService = reportService;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .addInterceptors(new ClientIpHandshakeInterceptor(reportService))
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}
