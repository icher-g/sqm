package io.sqm.middleware.api;

/**
 * Request payload for middleware analyze operation.
 *
 * @param sql     SQL text to analyze
 * @param context execution context
 */
public record AnalyzeRequest(String sql, ExecutionContextDto context) {
}
