package io.sqm.control;

import io.sqm.catalog.model.CatalogSchema;
import io.sqm.validate.schema.SchemaQueryValidator;
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
            DefaultSqlQueryParser.standard()
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
            DefaultSqlQueryParser.standard()
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
            DefaultSqlQueryParser.standard()
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
            DefaultSqlQueryParser.standard()
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
        return of(engine, explainer, auditPublisher, RuntimeGuardrails.disabled(), DefaultSqlQueryParser.standard());
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
        return of(engine, explainer, auditPublisher, guardrails, DefaultSqlQueryParser.standard());
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
     * Creates middleware from catalog schema using default validation settings and no-op observability/guardrails.
     *
     * @param schema catalog schema
     * @return middleware instance
     */
    static SqlMiddleware of(CatalogSchema schema) {
        return of(
            schema,
            SchemaValidationSettings.defaults(),
            RuntimeGuardrails.disabled(),
            AuditEventPublisher.noop(),
            SqlDecisionExplainer.basic(),
            DefaultSqlQueryParser.standard()
        );
    }

    /**
     * Creates middleware from catalog schema with explicit validation settings.
     *
     * @param schema catalog schema
     * @param settings schema validation settings
     * @return middleware instance
     */
    static SqlMiddleware of(CatalogSchema schema, SchemaValidationSettings settings) {
        return of(
            schema,
            settings,
            RuntimeGuardrails.disabled(),
            AuditEventPublisher.noop(),
            SqlDecisionExplainer.basic(),
            DefaultSqlQueryParser.standard()
        );
    }

    /**
     * Creates middleware from catalog schema with explicit validation settings and runtime guardrails.
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
        return of(
            schema,
            settings,
            guardrails,
            AuditEventPublisher.noop(),
            SqlDecisionExplainer.basic(),
            DefaultSqlQueryParser.standard()
        );
    }

    /**
     * Creates middleware from catalog schema with full customization.
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
        Objects.requireNonNull(schema, "schema must not be null");
        Objects.requireNonNull(settings, "settings must not be null");
        Objects.requireNonNull(guardrails, "guardrails must not be null");
        Objects.requireNonNull(auditPublisher, "auditPublisher must not be null");
        Objects.requireNonNull(explainer, "explainer must not be null");
        Objects.requireNonNull(queryParser, "queryParser must not be null");

        var queryValidator = SchemaQueryValidator.of(schema, settings);
        var engine = DefaultSqlDecisionEngine.of(queryValidator);
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
     * @param sql     input SQL
     * @param context execution context
     * @return decision result
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
