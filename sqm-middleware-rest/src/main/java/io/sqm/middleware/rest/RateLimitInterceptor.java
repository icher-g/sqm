package io.sqm.middleware.rest;

import jakarta.annotation.Nonnull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Objects;

/**
 * Enforces fixed-window rate limits for REST endpoints when enabled.
 */
public final class RateLimitInterceptor implements HandlerInterceptor {

    private final RestAbuseProtectionProperties properties;
    private final FixedWindowRateLimiter limiter;

    /**
     * Creates rate-limit interceptor.
     *
     * @param properties abuse-protection properties
     * @param limiter fixed-window limiter
     */
    public RateLimitInterceptor(RestAbuseProtectionProperties properties, FixedWindowRateLimiter limiter) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.limiter = Objects.requireNonNull(limiter, "limiter must not be null");
    }

    /**
     * Applies rate-limiting check before request reaches controller.
     *
     * @param request HTTP request
     * @param response HTTP response
     * @param handler selected handler
     * @return {@code true} when request is accepted
     */
    @Override
    public boolean preHandle(@Nonnull HttpServletRequest request, @Nonnull HttpServletResponse response, @Nonnull Object handler) {
        if (!properties.isRateLimitEnabled()) {
            return true;
        }
        var key = resolveClientKey(request);
        if (!limiter.allow(key)) {
            throw new RateLimitExceededException("Rate limit exceeded");
        }
        return true;
    }

    private String resolveClientKey(HttpServletRequest request) {
        if (properties.isTrustProxyHeaders()) {
            var headerName = properties.getClientIpHeader();
            if (headerName != null && !headerName.isBlank()) {
                var headerValue = request.getHeader(headerName);
                var resolved = firstIpFromHeader(headerValue);
                if (resolved != null) {
                    return resolved;
                }
            }
        }
        return request.getRemoteAddr();
    }

    private static String firstIpFromHeader(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        var parts = value.split(",");
        for (String part : parts) {
            var candidate = part.trim();
            if (!candidate.isEmpty()) {
                return candidate;
            }
        }
        return null;
    }
}
