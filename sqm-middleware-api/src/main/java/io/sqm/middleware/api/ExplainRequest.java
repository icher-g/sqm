package io.sqm.middleware.api;

/**
 * Request payload for middleware decision explanation.
 *
 * @param sql     SQL text to explain
 * @param context execution context
 */
public record ExplainRequest(String sql, ExecutionContextDto context) {
}
