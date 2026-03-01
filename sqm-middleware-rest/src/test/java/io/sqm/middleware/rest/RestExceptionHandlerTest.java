package io.sqm.middleware.rest;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class RestExceptionHandlerTest {

    @Test
    void maps_rest_request_exception_to_stable_payload() {
        var handler = new RestExceptionHandler();
        var request = new MockHttpServletRequest();
        request.setRequestURI("/sqm/middleware/v1/analyze");

        var response = handler.handleRestRequestException(
            new RateLimitExceededException("Rate limit exceeded"),
            request
        );

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("RATE_LIMIT_EXCEEDED", response.getBody().code());
        assertEquals("/sqm/middleware/v1/analyze", response.getBody().path());
    }

    @Test
    void maps_unreadable_payload_to_invalid_request() {
        var handler = new RestExceptionHandler();
        var request = new MockHttpServletRequest();
        request.setRequestURI("/sqm/middleware/v1/enforce");

        var response = handler.handleUnreadablePayload(
            new HttpMessageNotReadableException("broken payload", new MockHttpInputMessage(new byte[0])),
            request
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_REQUEST", response.getBody().code());
        assertEquals("Malformed request payload", response.getBody().message());
    }

    @Test
    void maps_validation_exception_to_invalid_request() {
        var handler = new RestExceptionHandler();
        var request = new MockHttpServletRequest();
        request.setRequestURI("/sqm/middleware/v1/explain");

        var response = handler.handleValidationException(
            new IllegalArgumentException("Field 'sql' must not be blank"),
            request
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_REQUEST", response.getBody().code());
        assertEquals("Field 'sql' must not be blank", response.getBody().message());
    }

    @Test
    void maps_unexpected_exception_to_internal_error() {
        var handler = new RestExceptionHandler();
        var request = new MockHttpServletRequest();
        request.setRequestURI("/sqm/middleware/v1/analyze");

        var response = handler.handleUnexpectedException(new RuntimeException("boom"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INTERNAL_ERROR", response.getBody().code());
        assertEquals("boom", response.getBody().message());
    }
}
