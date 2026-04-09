package io.sqm.playground.rest.error;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests stable error response mapping.
 */
class PlaygroundExceptionHandlerTest {

    @Test
    void handlesStableRequestExceptions() {
        var handler = new PlaygroundExceptionHandler();
        var request = request("/api/v1/examples");

        var response = handler.handlePlaygroundRequestException(new RateLimitExceededException("Too many"), request);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals("RATE_LIMIT_EXCEEDED", Objects.requireNonNull(response.getBody()).code());
        assertEquals("/api/v1/examples", response.getBody().path());
    }

    @Test
    void handlesUnreadablePayloads() {
        var handler = new PlaygroundExceptionHandler();
        var request = request("/api/v1/parse");

        @SuppressWarnings("deprecation") var response = handler.handleUnreadablePayload(new HttpMessageNotReadableException("bad payload"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_REQUEST", Objects.requireNonNull(response.getBody()).code());
        assertEquals("Malformed request payload", response.getBody().message());
    }

    @Test
    void handlesValidationExceptions() {
        var handler = new PlaygroundExceptionHandler();
        var request = request("/api/v1/render");

        var response = handler.handleValidationException(new IllegalArgumentException("dialect is required"), request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID_REQUEST", Objects.requireNonNull(response.getBody()).code());
        assertEquals("dialect is required", response.getBody().message());
    }

    @Test
    void handlesUnexpectedExceptions() {
        var handler = new PlaygroundExceptionHandler();
        var request = request("/api/v1/transpile");

        var response = handler.handleUnexpectedException(new IllegalStateException("boom"), request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("INTERNAL_ERROR", Objects.requireNonNull(response.getBody()).code());
        assertEquals("boom", response.getBody().message());
    }

    private static HttpServletRequest request(String path) {
        var request = new MockHttpServletRequest();
        request.setRequestURI(path);
        return request;
    }
}
