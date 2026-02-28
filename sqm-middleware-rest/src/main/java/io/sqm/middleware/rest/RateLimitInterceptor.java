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
        var key = request.getRemoteAddr();
        if (!limiter.allow(key)) {
            throw new RateLimitExceededException("Rate limit exceeded");
        }
        return true;
    }
}
