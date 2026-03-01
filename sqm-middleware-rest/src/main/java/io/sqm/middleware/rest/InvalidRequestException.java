package io.sqm.middleware.rest;

import org.springframework.http.HttpStatus;

/**
 * Exception for malformed or incomplete REST request payloads.
 */
public final class InvalidRequestException extends RestRequestException {

    /**
     * Creates invalid-request exception.
     *
     * @param message error message
     */
    public InvalidRequestException(String message) {
        super("INVALID_REQUEST", HttpStatus.BAD_REQUEST, message);
    }
}
