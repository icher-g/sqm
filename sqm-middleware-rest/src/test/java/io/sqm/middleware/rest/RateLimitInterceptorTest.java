package io.sqm.middleware.rest;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitInterceptorTest {

    @Test
    void allows_when_rate_limit_is_disabled() {
        var properties = new RestAbuseProtectionProperties();
        properties.setRateLimitEnabled(false);

        var interceptor = new RateLimitInterceptor(properties, new FixedWindowRateLimiter(1, 60, Clock.systemUTC()));
        assertTrue(interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object()));
    }

    @Test
    void limits_by_remote_address_when_proxy_headers_are_not_trusted() {
        var properties = new RestAbuseProtectionProperties();
        properties.setRateLimitEnabled(true);
        properties.setTrustProxyHeaders(false);

        var interceptor = new RateLimitInterceptor(properties, new FixedWindowRateLimiter(1, 60, Clock.systemUTC()));

        var first = request("10.0.0.1", "1.1.1.1, 2.2.2.2");
        var second = request("10.0.0.1", "3.3.3.3, 4.4.4.4");

        assertTrue(interceptor.preHandle(first, new MockHttpServletResponse(), new Object()));
        assertThrows(
            RateLimitExceededException.class,
            () -> interceptor.preHandle(second, new MockHttpServletResponse(), new Object())
        );
    }

    @Test
    void limits_by_forwarded_header_when_proxy_headers_are_trusted() {
        var properties = new RestAbuseProtectionProperties();
        properties.setRateLimitEnabled(true);
        properties.setTrustProxyHeaders(true);
        properties.setClientIpHeader("X-Forwarded-For");

        var interceptor = new RateLimitInterceptor(properties, new FixedWindowRateLimiter(1, 60, Clock.systemUTC()));

        var first = request("10.0.0.1", "1.1.1.1, 2.2.2.2");
        var secondSameClient = request("10.0.0.2", "1.1.1.1, 9.9.9.9");

        assertTrue(interceptor.preHandle(first, new MockHttpServletResponse(), new Object()));
        assertThrows(
            RateLimitExceededException.class,
            () -> interceptor.preHandle(secondSameClient, new MockHttpServletResponse(), new Object())
        );
    }

    @Test
    void falls_back_to_remote_address_when_configured_proxy_header_is_blank_or_missing() {
        var properties = new RestAbuseProtectionProperties();
        properties.setRateLimitEnabled(true);
        properties.setTrustProxyHeaders(true);
        properties.setClientIpHeader(" ");

        var interceptor = new RateLimitInterceptor(properties, new FixedWindowRateLimiter(1, 60, Clock.systemUTC()));

        var first = request("10.0.0.1", null);
        var second = request("10.0.0.1", null);

        assertTrue(interceptor.preHandle(first, new MockHttpServletResponse(), new Object()));
        assertThrows(
            RateLimitExceededException.class,
            () -> interceptor.preHandle(second, new MockHttpServletResponse(), new Object())
        );
    }

    private static MockHttpServletRequest request(String remoteAddr, String xForwardedFor) {
        var request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        if (xForwardedFor != null) {
            request.addHeader("X-Forwarded-For", xForwardedFor);
        }
        return request;
    }
}
