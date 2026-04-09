package io.sqm.playground.rest.ratelimit;

import io.sqm.playground.rest.config.PlaygroundAbuseProtectionProperties;
import io.sqm.playground.rest.error.RateLimitExceededException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests request interception for rate limiting.
 */
class RateLimitInterceptorTest {

    @Test
    void disabledRateLimitAlwaysAllowsRequests() {
        var properties = properties(false, false, "X-Forwarded-For");
        var interceptor = new RateLimitInterceptor(properties, new FixedWindowRateLimiter(1, 60, fixedClock()));

        var request = request("10.0.0.1", null);

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        assertTrue(interceptor.preHandle(request("10.0.0.1", null), new MockHttpServletResponse(), new Object()));
    }

    @Test
    void trustedProxyHeaderUsesFirstForwardedAddress() {
        var properties = properties(true, true, "X-Forwarded-For");
        var interceptor = new RateLimitInterceptor(properties, new FixedWindowRateLimiter(1, 60, fixedClock()));

        assertTrue(interceptor.preHandle(
            request("10.0.0.1", "203.0.113.10, 10.10.10.10"),
            new MockHttpServletResponse(),
            new Object()
        ));

        assertThrows(RateLimitExceededException.class, () -> interceptor.preHandle(
            request("10.0.0.2", "203.0.113.10, 10.10.10.11"),
            new MockHttpServletResponse(),
            new Object()
        ));
    }

    @Test
    void blankTrustedHeaderFallsBackToRemoteAddress() {
        var properties = properties(true, true, "X-Forwarded-For");
        var interceptor = new RateLimitInterceptor(properties, new FixedWindowRateLimiter(1, 60, fixedClock()));

        assertDoesNotThrow(() -> interceptor.preHandle(
            request("10.0.0.1", "   "),
            new MockHttpServletResponse(),
            new Object()
        ));
        assertDoesNotThrow(() -> interceptor.preHandle(
            request("10.0.0.2", " ,  "),
            new MockHttpServletResponse(),
            new Object()
        ));
    }

    @Test
    void customTrustedHeaderNameIsSupported() {
        var properties = properties(true, true, "X-Real-IP");
        var interceptor = new RateLimitInterceptor(properties, new FixedWindowRateLimiter(1, 60, fixedClock()));
        var request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Real-IP", "198.51.100.25");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }

    private static PlaygroundAbuseProtectionProperties properties(boolean enabled, boolean trustHeaders, String headerName) {
        var properties = new PlaygroundAbuseProtectionProperties();
        properties.setRateLimitEnabled(enabled);
        properties.setTrustProxyHeaders(trustHeaders);
        properties.setClientIpHeader(headerName);
        return properties;
    }

    private static Clock fixedClock() {
        return Clock.fixed(Instant.EPOCH, ZoneId.of("UTC"));
    }

    private static MockHttpServletRequest request(String remoteAddr, String forwardedFor) {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        return request;
    }
}
