package com.quikko.config;

import com.quikko.service.RateLimiterService;
import com.quikko.validation.ClientIpResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * IP-based sliding-window rate limiting for every REST endpoint under
 * {@code /api/*} (interest list, ICE server list, Time Capsule claim) —
 * cheap, unauthenticated GETs that a bot/script could otherwise hammer or
 * scrape without limit. Over the limit, responds 429 with a Retry-After
 * hint instead of serving the request.
 */
@Component
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final AppProperties props;

    public ApiRateLimitFilter(RateLimiterService rateLimiterService, AppProperties props) {
        this.rateLimiterService = rateLimiterService;
        this.props = props;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String ip = ClientIpResolver.resolve(request);
        if (!rateLimiterService.allowApiRequest(ip)) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(props.getRateLimit().getWindowSeconds()));
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
