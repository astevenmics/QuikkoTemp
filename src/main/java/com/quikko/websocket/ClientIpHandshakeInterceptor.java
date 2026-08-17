package com.quikko.websocket;

import com.quikko.service.ReportService;
import com.quikko.validation.ClientIpResolver;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Captures the caller's IP at WebSocket handshake time (so later STOMP
 * frames on this session can be IP-rate-limited / attributed for reports)
 * and rejects the handshake outright if that IP is currently banned.
 */
public class ClientIpHandshakeInterceptor implements HandshakeInterceptor {

    static final String IP_ATTRIBUTE = "ip";

    private final ReportService reportService;

    public ClientIpHandshakeInterceptor(ReportService reportService) {
        this.reportService = reportService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String ip = resolveIp(request);
        if (reportService.isBanned(ip)) {
            response.setStatusCode(org.springframework.http.HttpStatus.FORBIDDEN);
            return false;
        }
        attributes.put(IP_ATTRIBUTE, ip);
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }

    private String resolveIp(ServerHttpRequest request) {
        String fallback;
        if (request instanceof ServletServerHttpRequest servletRequest) {
            fallback = servletRequest.getServletRequest().getRemoteAddr();
        } else {
            fallback = request.getRemoteAddress() != null
                    ? request.getRemoteAddress().getAddress().getHostAddress()
                    : "unknown";
        }
        return ClientIpResolver.resolve(request.getHeaders().getFirst("X-Forwarded-For"), fallback);
    }
}
