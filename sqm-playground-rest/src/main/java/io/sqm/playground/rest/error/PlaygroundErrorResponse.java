package io.sqm.playground.rest.error;

/**
 * Stable REST error response for playground transport failures.
 *
 * @param code stable error code
 * @param message human-readable message
 * @param path request path
 */
public record PlaygroundErrorResponse(String code, String message, String path) {
}
