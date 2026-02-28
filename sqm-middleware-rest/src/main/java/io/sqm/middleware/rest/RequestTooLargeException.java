package io.sqm.middleware.rest;

import org.springframework.http.HttpStatus;

/**
 * Exception for oversized HTTP requests.
 */
public final class RequestTooLargeException extends RestRequestException {

    /**
     * Creates request-too-large exception.
     *
     * @param message error message
     */
    public RequestTooLargeException(String message) {
        super("REQUEST_TOO_LARGE", HttpStatus.PAYLOAD_TOO_LARGE, message);
    }
}
