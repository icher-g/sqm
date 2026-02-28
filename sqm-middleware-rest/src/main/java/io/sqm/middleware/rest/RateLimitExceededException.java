package io.sqm.middleware.rest;

import org.springframework.http.HttpStatus;

/**
 * Exception for requests rejected by rate limiting.
 */
public final class RateLimitExceededException extends RestRequestException {

    /**
     * Creates rate-limit exception.
     *
     * @param message error message
     */
    public RateLimitExceededException(String message) {
        super("RATE_LIMIT_EXCEEDED", HttpStatus.TOO_MANY_REQUESTS, message);
    }
}
