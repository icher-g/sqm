package io.sqm.control.execution;

import io.sqm.core.dialect.SqlDialectId;

import java.io.Serializable;
import java.util.Objects;

/**
 * Execution context for SQL middleware processing.
 *
 * @param dialectId            dialect identifier (for example, {@code postgresql})
 * @param principal            principal identifier initiating the request, may be {@code null}
 * @param tenant               tenant identifier for multitenant policies, may be {@code null}
 * @param mode                 processing mode
 * @param parameterizationMode describes how literals need to be treated.
 */
public record ExecutionContext(
    SqlDialectId dialectId,
    String principal,
    String tenant,
    ExecutionMode mode,
    ParameterizationMode parameterizationMode
) implements Serializable {

    /**
     * Validates required fields.
     *
     * @param dialectId dialect identifier
     * @param principal principal identifier
     * @param tenant    tenant identifier
     * @param mode      processing mode
     */
    public ExecutionContext {
        Objects.requireNonNull(dialectId, "dialectId must not be null");
        Objects.requireNonNull(mode, "mode must not be null");
    }

    /**
     * Creates a context for anonymous execution.
     *
     * @param dialect dialect identifier
     * @param mode    processing mode
     * @return new execution context
     */
    public static ExecutionContext of(String dialect, ExecutionMode mode) {
        return of(SqlDialectId.of(dialect), mode);
    }

    /**
     * Creates a context for anonymous execution.
     *
     * @param dialect dialect identifier
     * @param mode    processing mode
     * @return new execution context
     */
    public static ExecutionContext of(SqlDialectId dialect, ExecutionMode mode) {
        return new ExecutionContext(dialect, null, null, mode, ParameterizationMode.OFF);
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
        return of(SqlDialectId.of(dialect), principal, tenant, mode);
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
    public static ExecutionContext of(SqlDialectId dialect, String principal, String tenant, ExecutionMode mode) {
        return new ExecutionContext(dialect, principal, tenant, mode, ParameterizationMode.OFF);
    }

    /**
     * Creates a context with all supported attributes.
     *
     * @param dialect              dialect identifier
     * @param principal            principal identifier
     * @param tenant               tenant identifier
     * @param mode                 processing mode
     * @param parameterizationMode describes how literals need to be treated.
     * @return new execution context
     */
    public static ExecutionContext of(String dialect, String principal, String tenant, ExecutionMode mode, ParameterizationMode parameterizationMode) {
        return of(SqlDialectId.of(dialect), principal, tenant, mode, parameterizationMode);
    }

    /**
     * Creates a context with all supported attributes.
     *
     * @param dialect              dialect identifier
     * @param principal            principal identifier
     * @param tenant               tenant identifier
     * @param mode                 processing mode
     * @param parameterizationMode describes how literals need to be treated.
     * @return new execution context
     */
    public static ExecutionContext of(
        SqlDialectId dialect,
        String principal,
        String tenant,
        ExecutionMode mode,
        ParameterizationMode parameterizationMode
    ) {
        return new ExecutionContext(dialect, principal, tenant, mode, parameterizationMode);
    }

    /**
     * Returns the normalized dialect identifier as a string for compatibility with existing callers.
     *
     * @return dialect identifier string
     */
    public String dialect() {
        return dialectId.value();
    }

    /**
     * Creates a new context with the provided tenant while preserving other attributes.
     *
     * @param newTenant new tenant
     * @return new execution context
     */
    public ExecutionContext withTenant(String newTenant) {
        return new ExecutionContext(dialectId, principal, newTenant, mode, parameterizationMode);
    }

    /**
     * Creates a new context with the provided execution mode while preserving other attributes.
     *
     * @param newMode new processing mode
     * @return new execution context
     */
    public ExecutionContext withExecutionMode(ExecutionMode newMode) {
        return new ExecutionContext(dialectId, principal, tenant, newMode, parameterizationMode);
    }

    /**
     * Creates a new context with the provided parameterization mode while preserving other attributes.
     *
     * @param newMode new parameterization mode
     * @return new execution context
     */
    public ExecutionContext withParameterizationMode(ParameterizationMode newMode) {
        return new ExecutionContext(dialectId, principal, tenant, mode, newMode);
    }
}



