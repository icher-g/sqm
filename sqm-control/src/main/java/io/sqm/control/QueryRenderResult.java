package io.sqm.control;

import java.util.List;

/**
 * Represents a result of SQL renderer.
 *
 * @param sql    a rendered SQL
 * @param params a list of params if the {@link ExecutionContext#parameterizationMode()} returns {@link ParameterizationMode#BIND}.
 */
public record QueryRenderResult(String sql, List<Object> params) {

    /**
     * Creates a new instance from the provided SQL.
     *
     * @param sql a SQL.
     * @return a new instance.
     */
    public static QueryRenderResult of(String sql) {
        return new QueryRenderResult(sql, List.of());
    }

    /**
     * Creates a new instance from the provided SQL and list of parameters.
     *
     * @param sql    a SQL
     * @param params a list of parameters used in the SQL.
     * @return a new instance.
     */
    public static QueryRenderResult of(String sql, List<Object> params) {
        return new QueryRenderResult(sql, params);
    }
}
