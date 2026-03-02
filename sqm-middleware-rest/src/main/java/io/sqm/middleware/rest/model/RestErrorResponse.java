package io.sqm.middleware.rest.model;

/**
 * Stable error response payload returned by REST host endpoints.
 *
 * @param code stable error code
 * @param message error message
 * @param path request path
 */
public record RestErrorResponse(String code, String message, String path) {
}


