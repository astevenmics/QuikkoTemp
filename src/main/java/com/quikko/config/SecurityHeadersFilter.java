package com.quikko.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Sets a baseline set of defensive response headers on every request. This
 * app has no Spring Security (intentionally — see the "no accounts" design),
 * so these headers are applied directly rather than via a security filter
 * chain.
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    // script-src/style-src are 'self'-only (plus 'unsafe-inline' for style,
    // since nothing here uses inline <style>/style="" today but keeping it
    // off would be one extra constraint to remember if that changes) —
    // every page's JS lives in /js or /webjars, served same-origin.
    // connect-src allows wss:/https: for the STOMP WebSocket and same-origin
    // REST/XHR (SockJS's http-streaming/polling fallbacks and the /api/*
    // endpoints).
    private static final String CSP = "default-src 'self'; "
            + "connect-src 'self' wss: https:; "
            + "img-src 'self' data:; "
            + "script-src 'self'; "
            + "style-src 'self' 'unsafe-inline'";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        response.setHeader("Content-Security-Policy", CSP);
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        if (request.isSecure()) {
            // Only set over HTTPS — sending this on a plain-HTTP local-dev
            // request would make the browser try to upgrade localhost to
            // HTTPS on the next visit, breaking local dev.
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        }
        chain.doFilter(request, response);
    }
}
