package com.quikko.config;

import com.quikko.service.ReportService;
import com.quikko.validation.ClientIpResolver;
import com.quikko.websocket.ClientIpHandshakeInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

/**
 * STOMP-over-WebSocket wiring. Clients connect anonymously (no Principal/login);
 * Per-user targeting is done via a random session id known only to that browser tab, published on {@code /topic/session/{anonId}}.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final ReportService reportService;
    private final AppProperties appProperties;

    public WebSocketConfig(ReportService reportService, AppProperties appProperties) {
        this.reportService = reportService;
        this.appProperties = appProperties;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Plain WebSocket, no SockJS fallback -- every supported browser
        // (including mobile Safari) has had native WebSocket support for
        // years, so the SockJS negotiation layer (designed for IE8-era
        // browsers) is unneeded complexity and an extra point of failure.
        registry.addEndpoint("/ws")
                .addInterceptors(new ClientIpHandshakeInterceptor(reportService, appProperties))
                .setAllowedOriginPatterns(appProperties.getSecurity().allowedOriginsList().toArray(new String[0]));
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(64 * 1024);
        registration.setSendBufferSizeLimit(512 * 1024);
    }
}
