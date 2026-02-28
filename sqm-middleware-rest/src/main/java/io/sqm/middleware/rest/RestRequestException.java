package io.sqm.middleware.rest;

import org.springframework.http.HttpStatus;

/**
 * Base class for stable REST request failures.
 */
public abstract class RestRequestException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    /**
     * Creates exception with stable code and HTTP status.
     *
     * @param code stable error code
     * @param status HTTP status
     * @param message error message
     */
    protected RestRequestException(String code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    /**
     * Returns stable error code.
     *
     * @return stable error code
     */
    public String code() {
        return code;
    }

    /**
     * Returns HTTP status.
     *
     * @return HTTP status
     */
    public HttpStatus status() {
        return status;
    }
}
