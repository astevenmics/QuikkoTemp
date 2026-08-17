package com.quikko.validation;

import jakarta.servlet.http.HttpServletRequest;

import java.util.regex.Pattern;

/**
 * Resolves the caller's real IP for both plain HTTP requests and WebSocket
 * handshakes, used for rate limiting, bans, and report attribution. Trusts
 * the {@code X-Forwarded-For} header only when its value actually looks
 * like an IP literal — otherwise falls back to the socket's remote address
 * rather than trusting attacker-controlled text (used to evade rate limits
 * or frame another IP for a ban).
 */
public final class ClientIpResolver {

    // Deliberately loose (not a full RFC validator) — this only needs to
    // reject obviously-not-an-IP values in a client-controlled header.
    private static final Pattern IPV4 = Pattern.compile(
            "^(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}$");
    private static final Pattern IPV6 = Pattern.compile("^[0-9a-fA-F:]{2,45}$");

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        return resolve(request.getHeader("X-Forwarded-For"), request.getRemoteAddr());
    }

    /**
     * Header-value + fallback variant usable outside a servlet request
     * context (e.g. the WebSocket handshake, which exposes headers via a
     * different API).
     */
    public static String resolve(String forwardedForHeader, String fallbackIp) {
        if (forwardedForHeader != null && !forwardedForHeader.isBlank()) {
            String candidate = forwardedForHeader.split(",")[0].trim();
            if (isValidIpLiteral(candidate)) {
                return candidate;
            }
        }
        return fallbackIp;
    }

    public static boolean isValidIpLiteral(String value) {
        return value != null
                && (IPV4.matcher(value).matches()
                        || (value.contains(":") && IPV6.matcher(value).matches()));
    }
}
