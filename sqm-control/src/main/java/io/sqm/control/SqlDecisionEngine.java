package io.sqm.control;

import io.sqm.catalog.model.CatalogSchema;
import io.sqm.core.Query;
import io.sqm.control.impl.DefaultSqlDecisionEngine;
import io.sqm.validate.api.QueryValidator;
import io.sqm.validate.schema.SchemaValidationSettings;

/**
 * Functional contract for query-model decision evaluation.
 */
@FunctionalInterface
public interface SqlDecisionEngine {
    /**
     * Creates a validation-only decision engine.
     *
     * @param queryValidator semantic validator
     * @return decision engine
     */
    static SqlDecisionEngine validationOnly(QueryValidator queryValidator) {
        return DefaultSqlDecisionEngine.validationOnly(queryValidator);
    }

    /**
     * Creates a validation-only decision engine for a catalog schema.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param schema catalog schema
     * @return decision engine
     */
    static SqlDecisionEngine validationOnly(String dialect, CatalogSchema schema) {
        return DefaultSqlDecisionEngine.validationOnly(dialect, schema);
    }

    /**
     * Creates a validation-only decision engine for a catalog schema.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param schema catalog schema
     * @param settings schema validation settings
     * @return decision engine
     */
    static SqlDecisionEngine validationOnly(
        String dialect,
        CatalogSchema schema,
        SchemaValidationSettings settings
    ) {
        return DefaultSqlDecisionEngine.validationOnly(dialect, schema, settings);
    }

    /**
     * Creates a full-flow decision engine with explicit pipeline wiring.
     *
     * @param queryValidator semantic validator
     * @param queryRewriter query rewrite pipeline
     * @param queryRenderer query renderer
     * @return decision engine
     */
    static SqlDecisionEngine fullFlow(
        QueryValidator queryValidator,
        SqlQueryRewriter queryRewriter,
        SqlQueryRenderer queryRenderer
    ) {
        return DefaultSqlDecisionEngine.fullFlow(queryValidator, queryRewriter, queryRenderer);
    }

    /**
     * Creates a validation+rewrite engine with explicit rewriter and dialect-selected renderer.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param queryValidator semantic validator
     * @param queryRewriter query rewrite pipeline
     * @return decision engine
     */
    static SqlDecisionEngine validationAndRewrite(
        String dialect,
        QueryValidator queryValidator,
        SqlQueryRewriter queryRewriter
    ) {
        return DefaultSqlDecisionEngine.validationAndRewrite(dialect, queryValidator, queryRewriter);
    }

    /**
     * Creates a validation+rewrite engine with all currently available built-in rewrites.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param queryValidator semantic validator
     * @return decision engine
     */
    static SqlDecisionEngine validationAndRewrite(String dialect, QueryValidator queryValidator) {
        return DefaultSqlDecisionEngine.validationAndRewrite(dialect, queryValidator);
    }

    /**
     * Creates a validation+rewrite engine with selected built-in rewrites.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param queryValidator semantic validator
     * @param rewrites built-in rewrites to enable
     * @return decision engine
     */
    static SqlDecisionEngine validationAndRewrite(
        String dialect,
        QueryValidator queryValidator,
        BuiltInRewriteRule... rewrites
    ) {
        return DefaultSqlDecisionEngine.validationAndRewrite(dialect, queryValidator, rewrites);
    }

    /**
     * Creates a validation+rewrite engine for a catalog schema.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param schema catalog schema
     * @param settings schema validation settings
     * @param queryRewriter query rewrite pipeline
     * @return decision engine
     */
    static SqlDecisionEngine validationAndRewrite(
        String dialect,
        CatalogSchema schema,
        SchemaValidationSettings settings,
        SqlQueryRewriter queryRewriter
    ) {
        return DefaultSqlDecisionEngine.validationAndRewrite(dialect, schema, settings, queryRewriter);
    }

    /**
     * Creates a validation+rewrite engine for a catalog schema using all available built-in rewrites.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param schema catalog schema
     * @param settings schema validation settings
     * @return decision engine
     */
    static SqlDecisionEngine validationAndRewrite(
        String dialect,
        CatalogSchema schema,
        SchemaValidationSettings settings
    ) {
        return DefaultSqlDecisionEngine.validationAndRewrite(dialect, schema, settings);
    }

    /**
     * Creates a validation+rewrite engine for a catalog schema with selected built-in rewrites.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param schema catalog schema
     * @param settings schema validation settings
     * @param rewrites built-in rewrites to enable
     * @return decision engine
     */
    static SqlDecisionEngine validationAndRewrite(
        String dialect,
        CatalogSchema schema,
        SchemaValidationSettings settings,
        BuiltInRewriteRule... rewrites
    ) {
        return DefaultSqlDecisionEngine.validationAndRewrite(dialect, schema, settings, rewrites);
    }

    /**
     * Creates a full-flow engine with explicit rewriter and dialect-selected renderer.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param queryValidator semantic validator
     * @param queryRewriter query rewrite pipeline
     * @return decision engine
     */
    static SqlDecisionEngine fullFlow(
        String dialect,
        QueryValidator queryValidator,
        SqlQueryRewriter queryRewriter
    ) {
        return DefaultSqlDecisionEngine.fullFlow(dialect, queryValidator, queryRewriter);
    }

    /**
     * Creates a full-flow engine with all currently available built-in rewrites.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param queryValidator semantic validator
     * @return decision engine
     */
    static SqlDecisionEngine fullFlow(String dialect, QueryValidator queryValidator) {
        return DefaultSqlDecisionEngine.fullFlow(dialect, queryValidator);
    }

    /**
     * Creates a full-flow engine with selected built-in rewrites.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param queryValidator semantic validator
     * @param rewrites built-in rewrites to enable
     * @return decision engine
     */
    static SqlDecisionEngine fullFlow(
        String dialect,
        QueryValidator queryValidator,
        BuiltInRewriteRule... rewrites
    ) {
        return DefaultSqlDecisionEngine.fullFlow(dialect, queryValidator, rewrites);
    }

    /**
     * Creates a full-flow engine with custom renderer override.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param queryValidator semantic validator
     * @param queryRewriter query rewrite pipeline
     * @param queryRenderer query renderer
     * @return decision engine
     */
    static SqlDecisionEngine fullFlow(
        String dialect,
        QueryValidator queryValidator,
        SqlQueryRewriter queryRewriter,
        SqlQueryRenderer queryRenderer
    ) {
        return DefaultSqlDecisionEngine.fullFlow(dialect, queryValidator, queryRewriter, queryRenderer);
    }

    /**
     * Creates a full-flow engine for a catalog schema with explicit rewrite pipeline.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param schema catalog schema
     * @param settings schema validation settings
     * @param queryRewriter query rewrite pipeline
     * @return decision engine
     */
    static SqlDecisionEngine fullFlow(
        String dialect,
        CatalogSchema schema,
        SchemaValidationSettings settings,
        SqlQueryRewriter queryRewriter
    ) {
        return DefaultSqlDecisionEngine.fullFlow(dialect, schema, settings, queryRewriter);
    }

    /**
     * Creates a full-flow engine for a catalog schema using all available built-in rewrites.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param schema catalog schema
     * @param settings schema validation settings
     * @return decision engine
     */
    static SqlDecisionEngine fullFlow(
        String dialect,
        CatalogSchema schema,
        SchemaValidationSettings settings
    ) {
        return DefaultSqlDecisionEngine.fullFlow(dialect, schema, settings);
    }

    /**
     * Creates a full-flow engine for a catalog schema with selected built-in rewrites.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param schema catalog schema
     * @param settings schema validation settings
     * @param rewrites built-in rewrites to enable
     * @return decision engine
     */
    static SqlDecisionEngine fullFlow(
        String dialect,
        CatalogSchema schema,
        SchemaValidationSettings settings,
        BuiltInRewriteRule... rewrites
    ) {
        return DefaultSqlDecisionEngine.fullFlow(dialect, schema, settings, rewrites);
    }

    /**
     * Creates a full-flow engine for a catalog schema with custom renderer override.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param schema catalog schema
     * @param settings schema validation settings
     * @param queryRewriter query rewrite pipeline
     * @param queryRenderer query renderer
     * @return decision engine
     */
    static SqlDecisionEngine fullFlow(
        String dialect,
        CatalogSchema schema,
        SchemaValidationSettings settings,
        SqlQueryRewriter queryRewriter,
        SqlQueryRenderer queryRenderer
    ) {
        return DefaultSqlDecisionEngine.fullFlow(dialect, schema, settings, queryRewriter, queryRenderer);
    }

    /**
     * Evaluates query model for the provided context and returns a decision.
     *
     * @param query   parsed query model
     * @param context execution context
     * @return decision result
     */
    DecisionResult evaluate(Query query, ExecutionContext context);
}
