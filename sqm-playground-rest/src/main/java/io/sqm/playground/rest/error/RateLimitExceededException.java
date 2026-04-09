package io.sqm.playground.rest.error;

import org.springframework.http.HttpStatus;

/**
 * Exception for requests rejected by rate limiting.
 */
public final class RateLimitExceededException extends PlaygroundRequestException {

    /**
     * Creates rate-limit exception.
     *
     * @param message message
     */
    public RateLimitExceededException(String message) {
        super("RATE_LIMIT_EXCEEDED", HttpStatus.TOO_MANY_REQUESTS, message);
    }
}
