package io.sqm.middleware.rest;

import org.springframework.http.HttpStatus;

/**
 * Exception for unauthorized REST requests.
 */
public final class UnauthorizedRequestException extends RestRequestException {

    /**
     * Creates unauthorized exception.
     *
     * @param message error message
     */
    public UnauthorizedRequestException(String message) {
        super("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, message);
    }
}
