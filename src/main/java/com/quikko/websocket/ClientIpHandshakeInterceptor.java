package com.quikko.websocket;

import com.quikko.service.ReportService;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Captures the caller's IP at WebSocket handshake time (so later STOMP
 * frames on this session can be IP-rate-limited / attributed for reports)
 * and rejects the handshake outright if that IP is currently banned.
 */
public class ClientIpHandshakeInterceptor implements HandshakeInterceptor {

    static final String IP_ATTRIBUTE = "ip";

    // Deliberately loose (not a full RFC validator) — this only needs to
    // reject obviously-not-an-IP values in a client-controlled header
    // (arbitrary strings, injected junk, oversized values used to evade
    // rate-limits/bans or frame another IP). The socket's real remote
    // address is always used as a safe fallback when this doesn't match.
    private static final Pattern IPV4 = Pattern.compile(
            "^(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}$");
    private static final Pattern IPV6 = Pattern.compile("^[0-9a-fA-F:]{2,45}$");

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
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String candidate = forwarded.split(",")[0].trim();
            if (isValidIpLiteral(candidate)) {
                return candidate;
            }
            // Malformed/spoofed header value — fall through to the real
            // socket address instead of trusting attacker-controlled text.
        }
        if (request instanceof ServletServerHttpRequest servletRequest) {
            return servletRequest.getServletRequest().getRemoteAddr();
        }
        return request.getRemoteAddress() != null
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }

    private boolean isValidIpLiteral(String value) {
        return IPV4.matcher(value).matches()
                || (value.contains(":") && IPV6.matcher(value).matches());
    }
}
