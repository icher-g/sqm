package io.sqm.control;

import io.sqm.catalog.model.CatalogSchema;
import io.sqm.control.impl.DefaultSqlMiddleware;
import io.sqm.validate.schema.SchemaValidationSettings;

import java.util.Objects;

/**
 * Framework entry points for middleware SQL analysis and enforcement flows.
 */
public interface SqlMiddleware {
    /**
     * Creates middleware using the provided decision engine and the default explainer.
     *
     * @param engine decision engine
     * @return middleware instance
     */
    static SqlMiddleware of(SqlDecisionEngine engine) {
        return of(
            engine,
            SqlDecisionExplainer.basic(),
            AuditEventPublisher.noop(),
            RuntimeGuardrails.disabled(),
            SqlQueryParser.standard()
        );
    }

    /**
     * Creates middleware using the provided decision engine and explanation strategy.
     *
     * @param engine    decision engine
     * @param explainer explanation strategy
     * @return middleware instance
     */
    static SqlMiddleware of(SqlDecisionEngine engine, SqlDecisionExplainer explainer) {
        return of(
            engine,
            explainer,
            AuditEventPublisher.noop(),
            RuntimeGuardrails.disabled(),
            SqlQueryParser.standard()
        );
    }

    /**
     * Creates middleware using the provided decision engine and audit publisher.
     *
     * @param engine         decision engine
     * @param auditPublisher audit event publisher
     * @return middleware instance
     */
    static SqlMiddleware of(SqlDecisionEngine engine, AuditEventPublisher auditPublisher) {
        return of(
            engine,
            SqlDecisionExplainer.basic(),
            auditPublisher,
            RuntimeGuardrails.disabled(),
            SqlQueryParser.standard()
        );
    }

    /**
     * Creates middleware using the provided decision engine and runtime guardrails.
     *
     * @param engine     decision engine
     * @param guardrails runtime guardrails
     * @return middleware instance
     */
    static SqlMiddleware of(SqlDecisionEngine engine, RuntimeGuardrails guardrails) {
        return of(
            engine,
            SqlDecisionExplainer.basic(),
            AuditEventPublisher.noop(),
            guardrails,
            SqlQueryParser.standard()
        );
    }

    /**
     * Creates middleware using the provided decision engine, explanation strategy, and audit publisher.
     *
     * @param engine         decision engine
     * @param explainer      explanation strategy
     * @param auditPublisher audit event publisher
     * @return middleware instance
     */
    static SqlMiddleware of(
        SqlDecisionEngine engine,
        SqlDecisionExplainer explainer,
        AuditEventPublisher auditPublisher
    ) {
        return of(engine, explainer, auditPublisher, RuntimeGuardrails.disabled(), SqlQueryParser.standard());
    }

    /**
     * Creates middleware using the provided decision engine, explanation strategy, audit publisher, and runtime guardrails.
     *
     * @param engine         decision engine
     * @param explainer      explanation strategy
     * @param auditPublisher audit event publisher
     * @param guardrails     runtime guardrails
     * @return middleware instance
     */
    static SqlMiddleware of(
        SqlDecisionEngine engine,
        SqlDecisionExplainer explainer,
        AuditEventPublisher auditPublisher,
        RuntimeGuardrails guardrails
    ) {
        return of(engine, explainer, auditPublisher, guardrails, SqlQueryParser.standard());
    }

    /**
     * Creates middleware using explicit parser wiring.
     *
     * @param engine         decision engine
     * @param explainer      explanation strategy
     * @param auditPublisher audit event publisher
     * @param guardrails     runtime guardrails
     * @param queryParser    SQL-to-query parser
     * @return middleware instance
     */
    static SqlMiddleware of(
        SqlDecisionEngine engine,
        SqlDecisionExplainer explainer,
        AuditEventPublisher auditPublisher,
        RuntimeGuardrails guardrails,
        SqlQueryParser queryParser
    ) {
        Objects.requireNonNull(engine, "engine must not be null");
        Objects.requireNonNull(explainer, "explainer must not be null");
        Objects.requireNonNull(auditPublisher, "auditPublisher must not be null");
        Objects.requireNonNull(guardrails, "guardrails must not be null");
        Objects.requireNonNull(queryParser, "queryParser must not be null");
        return new DefaultSqlMiddleware(engine, explainer, auditPublisher, guardrails, queryParser);
    }

    /**
     * Creates middleware from catalog schema using ANSI dialect by default, default validation settings,
     * and no-op observability/guardrails.
     *
     * @param schema catalog schema
     * @return middleware instance
     */
    static SqlMiddleware of(CatalogSchema schema) {
        return of(null, schema);
    }

    /**
     * Creates middleware from catalog schema with explicit validation settings using ANSI dialect by default.
     *
     * @param schema catalog schema
     * @param settings schema validation settings
     * @return middleware instance
     */
    static SqlMiddleware of(CatalogSchema schema, SchemaValidationSettings settings) {
        return of(null, schema, settings);
    }

    /**
     * Creates middleware from catalog schema with explicit validation settings and runtime guardrails
     * using ANSI dialect by default.
     *
     * @param schema catalog schema
     * @param settings schema validation settings
     * @param guardrails runtime guardrails
     * @return middleware instance
     */
    static SqlMiddleware of(
        CatalogSchema schema,
        SchemaValidationSettings settings,
        RuntimeGuardrails guardrails
    ) {
        return of(null, schema, settings, guardrails);
    }

    /**
     * Creates middleware from catalog schema with full customization using ANSI dialect by default.
     *
     * @param schema catalog schema
     * @param settings schema validation settings
     * @param guardrails runtime guardrails
     * @param auditPublisher audit event publisher
     * @param explainer decision explainer
     * @param queryParser SQL parser
     * @return middleware instance
     */
    static SqlMiddleware of(
        CatalogSchema schema,
        SchemaValidationSettings settings,
        RuntimeGuardrails guardrails,
        AuditEventPublisher auditPublisher,
        SqlDecisionExplainer explainer,
        SqlQueryParser queryParser
    ) {
        return of(null, schema, settings, guardrails, auditPublisher, explainer, queryParser);
    }

    /**
     * Creates middleware from catalog schema for a fixed target dialect using dialect-aware validation.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier (for example, {@code ansi}, {@code postgresql}, {@code postgres})
     * @param schema catalog schema
     * @return middleware instance
     */
    static SqlMiddleware of(String dialect, CatalogSchema schema) {
        return of(
            dialect,
            schema,
            SchemaValidationSettings.defaults(),
            RuntimeGuardrails.disabled(),
            AuditEventPublisher.noop(),
            SqlDecisionExplainer.basic(),
            SqlQueryParser.standard()
        );
    }

    /**
     * Creates middleware from catalog schema for a fixed target dialect with explicit validation settings.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param schema catalog schema
     * @param settings schema validation settings
     * @return middleware instance
     */
    static SqlMiddleware of(String dialect, CatalogSchema schema, SchemaValidationSettings settings) {
        return of(
            dialect,
            schema,
            settings,
            RuntimeGuardrails.disabled(),
            AuditEventPublisher.noop(),
            SqlDecisionExplainer.basic(),
            SqlQueryParser.standard()
        );
    }

    /**
     * Creates middleware from catalog schema for a fixed target dialect with explicit validation settings and guardrails.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param schema catalog schema
     * @param settings schema validation settings
     * @param guardrails runtime guardrails
     * @return middleware instance
     */
    static SqlMiddleware of(
        String dialect,
        CatalogSchema schema,
        SchemaValidationSettings settings,
        RuntimeGuardrails guardrails
    ) {
        return of(
            dialect,
            schema,
            settings,
            guardrails,
            AuditEventPublisher.noop(),
            SqlDecisionExplainer.basic(),
            SqlQueryParser.standard()
        );
    }

    /**
     * Creates middleware from catalog schema for a fixed target dialect with full customization.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param schema catalog schema
     * @param settings schema validation settings
     * @param guardrails runtime guardrails
     * @param auditPublisher audit event publisher
     * @param explainer decision explainer
     * @param queryParser SQL parser
     * @return middleware instance
     */
    static SqlMiddleware of(
        String dialect,
        CatalogSchema schema,
        SchemaValidationSettings settings,
        RuntimeGuardrails guardrails,
        AuditEventPublisher auditPublisher,
        SqlDecisionExplainer explainer,
        SqlQueryParser queryParser
    ) {
        Objects.requireNonNull(schema, "schema must not be null");
        Objects.requireNonNull(settings, "settings must not be null");
        Objects.requireNonNull(guardrails, "guardrails must not be null");
        Objects.requireNonNull(auditPublisher, "auditPublisher must not be null");
        Objects.requireNonNull(explainer, "explainer must not be null");
        Objects.requireNonNull(queryParser, "queryParser must not be null");

        var engine = SqlDecisionEngine.validationOnly(dialect, schema, settings);
        return of(engine, explainer, auditPublisher, guardrails, queryParser);
    }

    /**
     * Evaluates SQL in analyze mode.
     *
     * @param sql     input SQL
     * @param context execution context
     * @return decision result
     */
    DecisionResult analyze(String sql, ExecutionContext context);

    /**
     * Evaluates SQL in enforce (execute-intent) mode.
     *
     * <p>This method does not execute SQL. It returns a decision and may rewrite the SQL
     * (for example, {@code EXPLAIN} dry-run when enabled by {@link RuntimeGuardrails}).</p>
     *
     * @param sql     input SQL
     * @param context execution context
     * @return decision result for execute-intent flow
     */
    DecisionResult enforce(String sql, ExecutionContext context);

    /**
     * Evaluates SQL in analyze mode and returns decision with explanation.
     *
     * @param sql     input SQL
     * @param context execution context
     * @return decision with explanation
     */
    DecisionExplanation explainDecision(String sql, ExecutionContext context);
}
