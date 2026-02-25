package io.sqm.control;

import io.sqm.catalog.model.CatalogSchema;
import io.sqm.control.impl.SqlMiddlewareConfigBuilderImpl;

import java.util.Objects;

/**
 * Immutable configuration object for assembling {@link SqlMiddleware} instances.
 *
 * <p>This type provides named factory methods for common middleware flows and fluent
 * customization methods for optional concerns (audit, explain, parser, guardrails),
 * avoiding large overload sets on {@link SqlMiddleware}.</p>
 */
public record SqlMiddlewareConfig(
    SqlDecisionEngine engine,
    SqlDecisionExplainer explainer,
    AuditEventPublisher auditPublisher,
    RuntimeGuardrails guardrails,
    SqlQueryParser queryParser) {

    /**
     * Validates required configuration fields.
     *
     * @param engine         decision engine
     * @param explainer      explanation strategy
     * @param auditPublisher audit publisher
     * @param guardrails     runtime guardrails
     * @param queryParser    SQL parser
     */
    public SqlMiddlewareConfig {
        Objects.requireNonNull(engine, "engine must not be null");
        Objects.requireNonNull(queryParser, "queryParser must not be null");
    }

    /**
     * Creates a builder pre-bound to the provided catalog schema.
     *
     * @param schema catalog schema used by validation and schema-aware rewrites
     * @return middleware config builder
     */
    public static SqlMiddlewareConfigBuilder builder(CatalogSchema schema) {
        return new SqlMiddlewareConfigBuilderImpl(schema);
    }
}
