package io.sqm.control;

import io.sqm.core.Query;

/**
 * Parses SQL text into SQM {@link Query} models.
 */
@FunctionalInterface
public interface SqlQueryParser {
    /**
     * Parses SQL text into a query model for the provided execution context.
     *
     * @param sql input SQL
     * @param context execution context
     * @return parsed query model
     */
    Query parse(String sql, ExecutionContext context);
}

