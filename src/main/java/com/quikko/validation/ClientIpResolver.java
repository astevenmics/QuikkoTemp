package com.quikko.validation;

import jakarta.servlet.http.HttpServletRequest;

import java.util.regex.Pattern;

public final class ClientIpResolver {

    private static final Pattern IPV4 = Pattern.compile("^(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(\\.(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)){3}$");
    private static final Pattern IPV6 = Pattern.compile("^[0-9a-fA-F:]{2,45}$");

    private ClientIpResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        return resolve(request.getHeader("X-Forwarded-For"), request.getRemoteAddr());
    }

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