package io.sqm.playground.rest.error;

import org.springframework.http.HttpStatus;

import java.util.Objects;

/**
 * Base exception for stable playground request failures.
 */
public abstract class PlaygroundRequestException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    /**
     * Creates a stable playground request exception.
     *
     * @param code stable error code
     * @param status HTTP status
     * @param message message
     */
    protected PlaygroundRequestException(String code, HttpStatus status, String message) {
        super(message);
        this.code = Objects.requireNonNull(code, "code must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
    }

    /**
     * Returns stable error code.
     *
     * @return error code
     */
    public String code() {
        return code;
    }

    /**
     * Returns HTTP status.
     *
     * @return status
     */
    public HttpStatus status() {
        return status;
    }
}
