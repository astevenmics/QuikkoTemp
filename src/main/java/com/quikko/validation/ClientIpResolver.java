package com.quikko.validation;

import jakarta.servlet.http.HttpServletRequest;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Resolves the caller's real IP for both plain HTTP requests and WebSocket
 * handshakes, used for rate limiting, bans, and report attribution. Trusts
 * the {@code X-Forwarded-For} header only when the request actually arrived
 * from a configured trusted proxy (see {@code quikko.security.trusted-proxies})
 * and its value looks like an IP literal — otherwise falls back to the
 * socket's remote address rather than trusting attacker-controlled text
 * (used to evade rate limits, bans, or frame another IP for a ban).
 */
public final class ClientIpResolver {

    // Deliberately loose (not a full RFC validator) — this only needs to
    // reject obviously-not-an-IP values in a client-controlled header.
    private static final Pattern IPV4 = Pattern.compile(
            "^(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}$");
    private static final Pattern IPV6 = Pattern.compile("^[0-9a-fA-F:]{2,45}$");

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request, List<String> trustedProxies) {
        return resolve(request.getHeader("X-Forwarded-For"), request.getRemoteAddr(), trustedProxies);
    }

    /**
     * Header-value + fallback variant usable outside a servlet request
     * context (e.g. the WebSocket handshake, which exposes headers via a
     * different API). {@code remoteAddr} is always the raw socket address —
     * both the trust check and the final fallback are evaluated against it.
     */
    public static String resolve(String forwardedForHeader, String remoteAddr, List<String> trustedProxies) {
        if (isTrustedProxy(remoteAddr, trustedProxies)
                && forwardedForHeader != null && !forwardedForHeader.isBlank()) {
            String candidate = forwardedForHeader.split(",")[0].trim();
            if (isValidIpLiteral(candidate)) {
                return candidate;
            }
        }
        return remoteAddr;
    }

    public static boolean isValidIpLiteral(String value) {
        return value != null
                && (IPV4.matcher(value).matches()
                        || (value.contains(":") && IPV6.matcher(value).matches()));
    }

    /**
     * Whether {@code remoteAddr} (the literal TCP peer address — never
     * client-controlled text) falls within one of the configured
     * trusted-proxy CIDR ranges / exact IPs. An empty allowlist (the
     * default) means no proxy is trusted and X-Forwarded-For is always
     * ignored.
     */
    private static boolean isTrustedProxy(String remoteAddr, List<String> trustedProxies) {
        if (remoteAddr == null || trustedProxies == null || trustedProxies.isEmpty()) {
            return false;
        }
        InetAddress remote;
        try {
            remote = InetAddress.getByName(remoteAddr);
        } catch (UnknownHostException e) {
            return false;
        }
        for (String entry : trustedProxies) {
            if (matchesCidr(remote, entry)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesCidr(InetAddress remote, String cidrOrIp) {
        String[] parts = cidrOrIp.split("/", 2);
        InetAddress network;
        try {
            network = InetAddress.getByName(parts[0].trim());
        } catch (UnknownHostException e) {
            return false;
        }
        byte[] remoteBytes = remote.getAddress();
        byte[] networkBytes = network.getAddress();
        if (remoteBytes.length != networkBytes.length) {
            return false; // address family mismatch (IPv4 vs IPv6)
        }
        int prefixBits = remoteBytes.length * 8;
        if (parts.length == 2) {
            try {
                prefixBits = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return matchesPrefix(remoteBytes, networkBytes, prefixBits);
    }

    private static boolean matchesPrefix(byte[] a, byte[] b, int prefixBits) {
        if (prefixBits < 0 || prefixBits > a.length * 8) {
            return false;
        }
        int fullBytes = prefixBits / 8;
        int remainingBits = prefixBits % 8;
        for (int i = 0; i < fullBytes; i++) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        if (remainingBits > 0) {
            int mask = (0xFF << (8 - remainingBits)) & 0xFF;
            if ((a[fullBytes] & mask) != (b[fullBytes] & mask)) {
                return false;
            }
        }
        return true;
    }
}
