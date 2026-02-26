package io.sqm.middleware.api;

/**
 * Request payload for middleware enforce operation.
 *
 * @param sql     SQL text to enforce
 * @param context execution context
 */
public record EnforceRequest(String sql, ExecutionContextDto context) {
}
