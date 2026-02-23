package io.sqm.control;

import io.sqm.catalog.model.CatalogSchema;
import io.sqm.validate.schema.SchemaValidationSettings;

import java.util.Objects;

/**
 * Immutable configuration object for assembling {@link SqlMiddleware} instances.
 *
 * <p>This type provides named factory methods for common middleware flows and fluent
 * customization methods for optional concerns (audit, explain, parser, guardrails),
 * avoiding large overload sets on {@link SqlMiddleware}.</p>
 */
public final class SqlMiddlewareConfig {
    private final SqlDecisionEngine engine;
    private final SqlDecisionExplainer explainer;
    private final AuditEventPublisher auditPublisher;
    private final RuntimeGuardrails guardrails;
    private final SqlQueryParser queryParser;

    private SqlMiddlewareConfig(
        SqlDecisionEngine engine,
        SqlDecisionExplainer explainer,
        AuditEventPublisher auditPublisher,
        RuntimeGuardrails guardrails,
        SqlQueryParser queryParser
    ) {
        this.engine = Objects.requireNonNull(engine, "engine must not be null");
        this.explainer = Objects.requireNonNull(explainer, "explainer must not be null");
        this.auditPublisher = Objects.requireNonNull(auditPublisher, "auditPublisher must not be null");
        this.guardrails = Objects.requireNonNull(guardrails, "guardrails must not be null");
        this.queryParser = Objects.requireNonNull(queryParser, "queryParser must not be null");
    }

    /**
     * Creates a middleware context for a pre-built decision engine.
     *
     * @param engine decision engine
     * @return middleware context with default explainer/audit/guardrails/parser
     */
    public static SqlMiddlewareConfig forEngine(SqlDecisionEngine engine) {
        return new SqlMiddlewareConfig(
            engine,
            SqlDecisionExplainer.basic(),
            AuditEventPublisher.noop(),
            RuntimeGuardrails.disabled(),
            SqlQueryParser.standard()
        );
    }

    /**
     * Creates a validation-only middleware context for a catalog schema using ANSI by default.
     *
     * @param schema catalog schema
     * @return middleware context
     */
    public static SqlMiddlewareConfig forValidation(CatalogSchema schema) {
        return forValidation(null, schema, SchemaValidationSettings.defaults());
    }

    /**
     * Creates a validation-only middleware context for a catalog schema and dialect.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param schema catalog schema
     * @return middleware context
     */
    public static SqlMiddlewareConfig forValidation(String dialect, CatalogSchema schema) {
        return forValidation(dialect, schema, SchemaValidationSettings.defaults());
    }

    /**
     * Creates a validation-only middleware context for a catalog schema and dialect.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param schema catalog schema
     * @param validationSettings schema validation settings
     * @return middleware context
     */
    public static SqlMiddlewareConfig forValidation(
        String dialect,
        CatalogSchema schema,
        SchemaValidationSettings validationSettings
    ) {
        return forEngine(SqlDecisionEngine.validationOnly(dialect, schema, validationSettings));
    }

    /**
     * Creates a validation+rewrite middleware context using schema-aware built-in rewrites.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param schema catalog schema
     * @param validationSettings schema validation settings
     * @return middleware context
     */
    public static SqlMiddlewareConfig forValidationAndRewrite(
        String dialect,
        CatalogSchema schema,
        SchemaValidationSettings validationSettings
    ) {
        return forEngine(SqlDecisionEngine.validationAndRewrite(dialect, schema, validationSettings));
    }

    /**
     * Creates a validation+rewrite middleware context using selected schema-aware built-in rewrites.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param schema catalog schema
     * @param validationSettings schema validation settings
     * @param rewriteSettings built-in rewrite settings
     * @param rewrites built-in rewrites to enable
     * @return middleware context
     */
    public static SqlMiddlewareConfig forValidationAndRewrite(
        String dialect,
        CatalogSchema schema,
        SchemaValidationSettings validationSettings,
        BuiltInRewriteSettings rewriteSettings,
        BuiltInRewriteRule... rewrites
    ) {
        return forEngine(SqlDecisionEngine.validationAndRewrite(
            dialect,
            schema,
            validationSettings,
            rewriteSettings,
            rewrites
        ));
    }

    /**
     * Returns a copy with custom decision explanation strategy.
     *
     * @param explainer explanation strategy
     * @return updated middleware context
     */
    public SqlMiddlewareConfig withExplainer(SqlDecisionExplainer explainer) {
        return new SqlMiddlewareConfig(engine, explainer, auditPublisher, guardrails, queryParser);
    }

    /**
     * Returns a copy with custom audit publisher.
     *
     * @param auditPublisher audit publisher
     * @return updated middleware context
     */
    public SqlMiddlewareConfig withAuditPublisher(AuditEventPublisher auditPublisher) {
        return new SqlMiddlewareConfig(engine, explainer, auditPublisher, guardrails, queryParser);
    }

    /**
     * Returns a copy with custom runtime guardrails.
     *
     * @param guardrails runtime guardrails
     * @return updated middleware context
     */
    public SqlMiddlewareConfig withGuardrails(RuntimeGuardrails guardrails) {
        return new SqlMiddlewareConfig(engine, explainer, auditPublisher, guardrails, queryParser);
    }

    /**
     * Returns a copy with custom SQL parser.
     *
     * @param queryParser SQL parser
     * @return updated middleware context
     */
    public SqlMiddlewareConfig withQueryParser(SqlQueryParser queryParser) {
        return new SqlMiddlewareConfig(engine, explainer, auditPublisher, guardrails, queryParser);
    }

    /**
     * Returns the configured decision engine.
     *
     * @return decision engine
     */
    public SqlDecisionEngine engine() {
        return engine;
    }

    /**
     * Returns the configured decision explainer.
     *
     * @return decision explainer
     */
    public SqlDecisionExplainer explainer() {
        return explainer;
    }

    /**
     * Returns the configured audit publisher.
     *
     * @return audit publisher
     */
    public AuditEventPublisher auditPublisher() {
        return auditPublisher;
    }

    /**
     * Returns runtime guardrail settings.
     *
     * @return runtime guardrails
     */
    public RuntimeGuardrails guardrails() {
        return guardrails;
    }

    /**
     * Returns the SQL parser.
     *
     * @return SQL parser
     */
    public SqlQueryParser queryParser() {
        return queryParser;
    }
}
