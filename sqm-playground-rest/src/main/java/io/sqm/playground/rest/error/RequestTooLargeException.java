package io.sqm.playground.rest.error;

import org.springframework.http.HttpStatus;

/**
 * Exception for oversized request payloads.
 */
public final class RequestTooLargeException extends PlaygroundRequestException {

    /**
     * Creates request-too-large exception.
     *
     * @param message message
     */
    public RequestTooLargeException(String message) {
        super("REQUEST_TOO_LARGE", HttpStatus.PAYLOAD_TOO_LARGE, message);
    }
}
