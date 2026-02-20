package io.sqm.core.control;

import java.io.Serializable;
import java.util.Objects;

/**
 * Execution context for SQL middleware processing.
 *
 * @param dialect   dialect identifier (for example, {@code postgresql})
 * @param principal principal identifier initiating the request, may be {@code null}
 * @param tenant    tenant identifier for multitenant policies, may be {@code null}
 * @param mode      processing mode
 */
public record ExecutionContext(
    String dialect,
    String principal,
    String tenant,
    ExecutionMode mode
) implements Serializable {

    /**
     * Creates a context for anonymous execution.
     *
     * @param dialect dialect identifier
     * @param mode    processing mode
     * @return new execution context
     */
    public static ExecutionContext of(String dialect, ExecutionMode mode) {
        return new ExecutionContext(dialect, null, null, mode);
    }

    /**
     * Creates a context with all supported attributes.
     *
     * @param dialect   dialect identifier
     * @param principal principal identifier
     * @param tenant    tenant identifier
     * @param mode      processing mode
     * @return new execution context
     */
    public static ExecutionContext of(String dialect, String principal, String tenant, ExecutionMode mode) {
        return new ExecutionContext(dialect, principal, tenant, mode);
    }

    /**
     * Validates required fields.
     *
     * @param dialect   dialect identifier
     * @param principal principal identifier
     * @param tenant    tenant identifier
     * @param mode      processing mode
     */
    public ExecutionContext {
        if (dialect == null || dialect.isBlank()) {
            throw new IllegalArgumentException("dialect must not be blank");
        }
        Objects.requireNonNull(mode, "mode must not be null");
    }
}
