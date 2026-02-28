package io.sqm.middleware.rest;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ApiKeyAuthInterceptorTest {

    @Test
    void allows_request_when_api_key_is_disabled() {
        var properties = new RestSecurityProperties();
        properties.setApiKeyEnabled(false);
        var interceptor = new ApiKeyAuthInterceptor(properties);

        assertTrue(interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object()));
    }

    @Test
    void rejects_when_header_configuration_is_missing() {
        var properties = new RestSecurityProperties();
        properties.setApiKeyEnabled(true);
        properties.setApiKeyHeader("  ");
        properties.setApiKeys(List.of("k1"));

        var interceptor = new ApiKeyAuthInterceptor(properties);
        assertThrows(
            UnauthorizedRequestException.class,
            () -> interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object())
        );
    }

    @Test
    void rejects_when_api_keys_are_missing() {
        var properties = new RestSecurityProperties();
        properties.setApiKeyEnabled(true);
        properties.setApiKeyHeader("X-API-Key");
        properties.setApiKeys(java.util.Arrays.asList(" ", null));

        var interceptor = new ApiKeyAuthInterceptor(properties);
        assertThrows(
            UnauthorizedRequestException.class,
            () -> interceptor.preHandle(new MockHttpServletRequest(), new MockHttpServletResponse(), new Object())
        );
    }

    @Test
    void rejects_when_provided_key_is_invalid() {
        var properties = new RestSecurityProperties();
        properties.setApiKeyEnabled(true);
        properties.setApiKeyHeader("X-API-Key");
        properties.setApiKeys(List.of("abc"));

        var request = new MockHttpServletRequest();
        request.addHeader("X-API-Key", "wrong");

        var interceptor = new ApiKeyAuthInterceptor(properties);
        assertThrows(
            UnauthorizedRequestException.class,
            () -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object())
        );
    }

    @Test
    void allows_when_valid_key_is_provided() {
        var properties = new RestSecurityProperties();
        properties.setApiKeyEnabled(true);
        properties.setApiKeyHeader("X-API-Key");
        properties.setApiKeys(List.of("abc"));

        var request = new MockHttpServletRequest();
        request.addHeader("X-API-Key", "abc");

        var interceptor = new ApiKeyAuthInterceptor(properties);
        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }
}
