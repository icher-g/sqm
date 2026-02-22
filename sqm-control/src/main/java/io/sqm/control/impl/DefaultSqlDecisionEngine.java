package io.sqm.control.impl;

import io.sqm.catalog.model.CatalogSchema;
import io.sqm.control.BuiltInRewriteSettings;
import io.sqm.control.DecisionResult;
import io.sqm.control.ExecutionContext;
import io.sqm.control.ReasonCode;
import io.sqm.control.RewriteDenyException;
import io.sqm.control.SqlDecisionEngine;
import io.sqm.control.SqlQueryRenderer;
import io.sqm.control.SqlQueryRewriter;
import io.sqm.control.BuiltInRewriteRule;
import io.sqm.core.Query;
import io.sqm.validate.api.QueryValidator;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.postgresql.PostgresValidationDialect;
import io.sqm.validate.schema.SchemaQueryValidator;
import io.sqm.validate.schema.SchemaValidationSettings;
import io.sqm.validate.schema.dialect.SchemaValidationDialect;

import java.util.Locale;
import java.util.Objects;

/**
 * Default decision engine that delegates semantic checks to {@link QueryValidator}.
 */
public final class DefaultSqlDecisionEngine implements SqlDecisionEngine {
    private final QueryValidator queryValidator;
    private final SqlQueryRewriter queryRewriter;
    private final SqlQueryRenderer queryRenderer;

    private DefaultSqlDecisionEngine(
        QueryValidator queryValidator,
        SqlQueryRewriter queryRewriter,
        SqlQueryRenderer queryRenderer
    ) {
        this.queryValidator = queryValidator;
        this.queryRewriter = queryRewriter;
        this.queryRenderer = queryRenderer;
    }

    /**
     * Creates a validation-only decision engine.
     *
     * @param queryValidator semantic validator
     * @return engine instance
     */
    public static DefaultSqlDecisionEngine validationOnly(QueryValidator queryValidator) {
        Objects.requireNonNull(queryValidator, "queryValidator must not be null");
        return new DefaultSqlDecisionEngine(
            queryValidator,
            SqlQueryRewriter.noop(),
            (query, context) -> {
                throw new IllegalStateException("queryRenderer must be configured when rewrite rules are enabled");
            }
        );
    }

    /**
     * Creates a decision engine with explicit validate/rewrite/render pipeline wiring.
     *
     * @param queryValidator semantic validator
     * @param queryRewriter  query-model rewrite pipeline
     * @param queryRenderer  renderer used when a rewrite decision must emit SQL
     * @return engine instance
     */
    public static DefaultSqlDecisionEngine fullFlow(
        QueryValidator queryValidator,
        SqlQueryRewriter queryRewriter,
        SqlQueryRenderer queryRenderer
    ) {
        Objects.requireNonNull(queryValidator, "queryValidator must not be null");
        Objects.requireNonNull(queryRewriter, "queryRewriter must not be null");
        Objects.requireNonNull(queryRenderer, "queryRenderer must not be null");
        return new DefaultSqlDecisionEngine(queryValidator, queryRewriter, queryRenderer);
    }

    /**
     * Creates a validation-only decision engine for a catalog schema and selected dialect.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param schema catalog schema
     * @return engine instance
     */
    public static DefaultSqlDecisionEngine validationOnly(String dialect, CatalogSchema schema) {
        return validationOnly(dialect, schema, SchemaValidationSettings.defaults());
    }

    /**
     * Creates a validation-only decision engine for a catalog schema and selected dialect.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param schema catalog schema
     * @param settings schema validation settings
     * @return engine instance
     */
    public static DefaultSqlDecisionEngine validationOnly(
        String dialect,
        CatalogSchema schema,
        SchemaValidationSettings settings
    ) {
        return validationOnly(schemaValidatorForDialect(dialect, schema, settings));
    }

    /**
     * Creates a decision engine with validation and rewrite enabled using a renderer selected by dialect.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier (for example, {@code ansi}, {@code postgresql}, or {@code postgres})
     * @param queryValidator semantic validator
     * @param queryRewriter query-model rewrite pipeline
     * @return engine instance
     */
    public static DefaultSqlDecisionEngine validationAndRewrite(
        String dialect,
        QueryValidator queryValidator,
        SqlQueryRewriter queryRewriter
    ) {
        return fullFlow(queryValidator, queryRewriter, SqlQueryRenderer.forDialect(dialect));
    }

    /**
     * Creates a decision engine with validation and all currently available built-in rewrites.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param queryValidator semantic validator
     * @return engine instance
     */
    public static DefaultSqlDecisionEngine validationAndRewrite(String dialect, QueryValidator queryValidator) {
        return validationAndRewrite(dialect, queryValidator, SqlQueryRewriter.allBuiltIn());
    }

    /**
     * Creates a decision engine with validation and all currently available built-in rewrites using explicit settings.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param queryValidator semantic validator
     * @param rewriteSettings built-in rewrite settings
     * @return engine instance
     */
    public static DefaultSqlDecisionEngine validationAndRewrite(
        String dialect,
        QueryValidator queryValidator,
        BuiltInRewriteSettings rewriteSettings
    ) {
        return validationAndRewrite(dialect, queryValidator, SqlQueryRewriter.allBuiltIn(rewriteSettings));
    }

    /**
     * Creates a decision engine with validation and the selected built-in rewrites.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param queryValidator semantic validator
     * @param rewrites built-in rewrites to enable
     * @return engine instance
     */
    public static DefaultSqlDecisionEngine validationAndRewrite(
        String dialect,
        QueryValidator queryValidator,
        BuiltInRewriteRule... rewrites
    ) {
        return validationAndRewrite(dialect, queryValidator, SqlQueryRewriter.builtIn(rewrites));
    }

    /**
     * Creates a decision engine with validation and selected built-in rewrites using explicit settings.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param queryValidator semantic validator
     * @param rewriteSettings built-in rewrite settings
     * @param rewrites built-in rewrites to enable
     * @return engine instance
     */
    public static DefaultSqlDecisionEngine validationAndRewrite(
        String dialect,
        QueryValidator queryValidator,
        BuiltInRewriteSettings rewriteSettings,
        BuiltInRewriteRule... rewrites
    ) {
        return validationAndRewrite(dialect, queryValidator, SqlQueryRewriter.builtIn(rewriteSettings, rewrites));
    }

    /**
     * Creates a decision engine with validation and rewrite for a catalog schema and selected dialect.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param schema catalog schema
     * @param settings schema validation settings
     * @param queryRewriter query-model rewrite pipeline
     * @return engine instance
     */
    public static DefaultSqlDecisionEngine validationAndRewrite(
        String dialect,
        CatalogSchema schema,
        SchemaValidationSettings settings,
        SqlQueryRewriter queryRewriter
    ) {
        return validationAndRewrite(dialect, schemaValidatorForDialect(dialect, schema, settings), queryRewriter);
    }

    /**
     * Creates a decision engine with validation and all currently available built-in rewrites for a catalog schema.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param schema catalog schema
     * @param settings schema validation settings
     * @return engine instance
     */
    public static DefaultSqlDecisionEngine validationAndRewrite(
        String dialect,
        CatalogSchema schema,
        SchemaValidationSettings settings
    ) {
        return validationAndRewrite(dialect, schema, settings, SqlQueryRewriter.allBuiltIn(schema));
    }

    /**
     * Creates a decision engine with validation and all currently available schema-aware built-in rewrites using explicit settings.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param schema catalog schema
     * @param settings schema validation settings
     * @param rewriteSettings built-in rewrite settings
     * @return engine instance
     */
    public static DefaultSqlDecisionEngine validationAndRewrite(
        String dialect,
        CatalogSchema schema,
        SchemaValidationSettings settings,
        BuiltInRewriteSettings rewriteSettings
    ) {
        return validationAndRewrite(dialect, schema, settings, SqlQueryRewriter.allBuiltIn(schema, rewriteSettings));
    }

    /**
     * Creates a decision engine with validation and selected built-in rewrites for a catalog schema.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param schema catalog schema
     * @param settings schema validation settings
     * @param rewrites built-in rewrites to enable
     * @return engine instance
     */
    public static DefaultSqlDecisionEngine validationAndRewrite(
        String dialect,
        CatalogSchema schema,
        SchemaValidationSettings settings,
        BuiltInRewriteRule... rewrites
    ) {
        return validationAndRewrite(dialect, schema, settings, SqlQueryRewriter.builtIn(schema, rewrites));
    }

    /**
     * Creates a decision engine with validation and selected schema-aware built-in rewrites using explicit settings.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param schema catalog schema
     * @param settings schema validation settings
     * @param rewriteSettings built-in rewrite settings
     * @param rewrites built-in rewrites to enable
     * @return engine instance
     */
    public static DefaultSqlDecisionEngine validationAndRewrite(
        String dialect,
        CatalogSchema schema,
        SchemaValidationSettings settings,
        BuiltInRewriteSettings rewriteSettings,
        BuiltInRewriteRule... rewrites
    ) {
        return validationAndRewrite(dialect, schema, settings, SqlQueryRewriter.builtIn(schema, rewriteSettings, rewrites));
    }

    /**
     * Creates a decision engine with full flow enabled and a renderer selected by dialect.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier (for example, {@code ansi}, {@code postgresql}, or {@code postgres})
     * @param queryValidator semantic validator
     * @param queryRewriter query-model rewrite pipeline
     * @return engine instance
     */
    public static DefaultSqlDecisionEngine fullFlow(
        String dialect,
        QueryValidator queryValidator,
        SqlQueryRewriter queryRewriter
    ) {
        return fullFlow(queryValidator, queryRewriter, SqlQueryRenderer.forDialect(dialect));
    }

    /**
     * Creates a decision engine with full flow enabled and all currently available built-in rewrites.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param queryValidator semantic validator
     * @return engine instance
     */
    public static DefaultSqlDecisionEngine fullFlow(String dialect, QueryValidator queryValidator) {
        return fullFlow(dialect, queryValidator, SqlQueryRewriter.allBuiltIn());
    }

    /**
     * Creates a decision engine with full flow enabled and all currently available built-in rewrites using explicit settings.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param queryValidator semantic validator
     * @param rewriteSettings built-in rewrite settings
     * @return engine instance
     */
    public static DefaultSqlDecisionEngine fullFlow(
        String dialect,
        QueryValidator queryValidator,
        BuiltInRewriteSettings rewriteSettings
    ) {
        return fullFlow(dialect, queryValidator, SqlQueryRewriter.allBuiltIn(rewriteSettings));
    }

    /**
     * Creates a decision engine with full flow enabled and selected built-in rewrites.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param queryValidator semantic validator
     * @param rewrites built-in rewrites to enable
     * @return engine instance
     */
    public static DefaultSqlDecisionEngine fullFlow(
        String dialect,
        QueryValidator queryValidator,
        BuiltInRewriteRule... rewrites
    ) {
        return fullFlow(dialect, queryValidator, SqlQueryRewriter.builtIn(rewrites));
    }

    /**
     * Creates a decision engine with full flow enabled and selected built-in rewrites using explicit settings.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param queryValidator semantic validator
     * @param rewriteSettings built-in rewrite settings
     * @param rewrites built-in rewrites to enable
     * @return engine instance
     */
    public static DefaultSqlDecisionEngine fullFlow(
        String dialect,
        QueryValidator queryValidator,
        BuiltInRewriteSettings rewriteSettings,
        BuiltInRewriteRule... rewrites
    ) {
        return fullFlow(dialect, queryValidator, SqlQueryRewriter.builtIn(rewriteSettings, rewrites));
    }

    /**
     * Creates a decision engine with full flow enabled while allowing a custom renderer override.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier used to validate supported dialect names
     * @param queryValidator semantic validator
     * @param queryRewriter query-model rewrite pipeline
     * @param queryRenderer renderer used for rewrite output SQL
     * @return engine instance
     */
    public static DefaultSqlDecisionEngine fullFlow(
        String dialect,
        QueryValidator queryValidator,
        SqlQueryRewriter queryRewriter,
        SqlQueryRenderer queryRenderer
    ) {
        SqlQueryRenderer.forDialect(dialect);
        return fullFlow(queryValidator, queryRewriter, queryRenderer);
    }

    /**
     * Creates a full-flow decision engine for a catalog schema and selected dialect.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param schema catalog schema
     * @param settings schema validation settings
     * @param queryRewriter query-model rewrite pipeline
     * @return engine instance
     */
    public static DefaultSqlDecisionEngine fullFlow(
        String dialect,
        CatalogSchema schema,
        SchemaValidationSettings settings,
        SqlQueryRewriter queryRewriter
    ) {
        return fullFlow(dialect, schemaValidatorForDialect(dialect, schema, settings), queryRewriter);
    }

    /**
     * Creates a full-flow decision engine for a catalog schema and all currently available built-in rewrites.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param schema catalog schema
     * @param settings schema validation settings
     * @return engine instance
     */
    public static DefaultSqlDecisionEngine fullFlow(
        String dialect,
        CatalogSchema schema,
        SchemaValidationSettings settings
    ) {
        return fullFlow(dialect, schema, settings, SqlQueryRewriter.allBuiltIn(schema));
    }

    /**
     * Creates a full-flow decision engine for a catalog schema and all currently available schema-aware built-in rewrites using explicit settings.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param schema catalog schema
     * @param settings schema validation settings
     * @param rewriteSettings built-in rewrite settings
     * @return engine instance
     */
    public static DefaultSqlDecisionEngine fullFlow(
        String dialect,
        CatalogSchema schema,
        SchemaValidationSettings settings,
        BuiltInRewriteSettings rewriteSettings
    ) {
        return fullFlow(dialect, schema, settings, SqlQueryRewriter.allBuiltIn(schema, rewriteSettings));
    }

    /**
     * Creates a full-flow decision engine for a catalog schema and selected built-in rewrites.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param schema catalog schema
     * @param settings schema validation settings
     * @param rewrites built-in rewrites to enable
     * @return engine instance
     */
    public static DefaultSqlDecisionEngine fullFlow(
        String dialect,
        CatalogSchema schema,
        SchemaValidationSettings settings,
        BuiltInRewriteRule... rewrites
    ) {
        return fullFlow(dialect, schema, settings, SqlQueryRewriter.builtIn(schema, rewrites));
    }

    /**
     * Creates a full-flow decision engine for a catalog schema and selected schema-aware built-in rewrites using explicit settings.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param schema catalog schema
     * @param settings schema validation settings
     * @param rewriteSettings built-in rewrite settings
     * @param rewrites built-in rewrites to enable
     * @return engine instance
     */
    public static DefaultSqlDecisionEngine fullFlow(
        String dialect,
        CatalogSchema schema,
        SchemaValidationSettings settings,
        BuiltInRewriteSettings rewriteSettings,
        BuiltInRewriteRule... rewrites
    ) {
        return fullFlow(dialect, schema, settings, SqlQueryRewriter.builtIn(schema, rewriteSettings, rewrites));
    }

    /**
     * Creates a full-flow decision engine for a catalog schema and selected dialect using a custom renderer.
     *
     * <p>If {@code dialect} is {@code null} or blank, ANSI is used by default.</p>
     *
     * @param dialect dialect identifier
     * @param schema catalog schema
     * @param settings schema validation settings
     * @param queryRewriter query-model rewrite pipeline
     * @param queryRenderer renderer used for rewrite output SQL
     * @return engine instance
     */
    public static DefaultSqlDecisionEngine fullFlow(
        String dialect,
        CatalogSchema schema,
        SchemaValidationSettings settings,
        SqlQueryRewriter queryRewriter,
        SqlQueryRenderer queryRenderer
    ) {
        return fullFlow(dialect, schemaValidatorForDialect(dialect, schema, settings), queryRewriter, queryRenderer);
    }

    /**
     * Evaluates a query model and maps validation outcomes to middleware decisions.
     *
     * @param query query model
     * @param context execution context
     * @return decision result
     */
    @Override
    public DecisionResult evaluate(Query query, ExecutionContext context) {
        Objects.requireNonNull(query, "query must not be null");
        Objects.requireNonNull(context, "context must not be null");

        var validation = queryValidator.validate(query);
        if (!validation.ok()) {
            var first = validation.problems().getFirst();
            return DecisionResult.deny(mapReason(first.code()), first.message());
        }

        final io.sqm.control.QueryRewriteResult rewrite;
        try {
            rewrite = queryRewriter.rewrite(query, context);
        } catch (RewriteDenyException ex) {
            return DecisionResult.deny(ex.reasonCode(), ex.getMessage());
        }
        if (!rewrite.rewritten()) {
            return DecisionResult.allow();
        }

        var rewrittenValidation = queryValidator.validate(rewrite.query());
        if (!rewrittenValidation.ok()) {
            var first = rewrittenValidation.problems().getFirst();
            return DecisionResult.deny(mapReason(first.code()), first.message());
        }

        String rewrittenSql = Objects.requireNonNull(
            queryRenderer.render(rewrite.query(), context),
            "queryRenderer must not return null"
        );
        String message = "Rewritten by policy rules: " + String.join(", ", rewrite.appliedRuleIds());
        return DecisionResult.rewrite(rewrite.primaryReasonCode(), message, rewrittenSql);
    }

    private static ReasonCode mapReason(ValidationProblem.Code code) {
        return switch (code) {
            case DDL_NOT_ALLOWED -> ReasonCode.DENY_DDL;
            case DML_NOT_ALLOWED -> ReasonCode.DENY_DML;
            case POLICY_TABLE_DENIED -> ReasonCode.DENY_TABLE;
            case POLICY_COLUMN_DENIED -> ReasonCode.DENY_COLUMN;
            case POLICY_FUNCTION_NOT_ALLOWED -> ReasonCode.DENY_FUNCTION;
            case POLICY_MAX_JOINS_EXCEEDED -> ReasonCode.DENY_MAX_JOINS;
            case POLICY_MAX_SELECT_COLUMNS_EXCEEDED -> ReasonCode.DENY_MAX_SELECT_COLUMNS;
            case DIALECT_FEATURE_UNSUPPORTED, DIALECT_CLAUSE_INVALID -> ReasonCode.DENY_UNSUPPORTED_DIALECT_FEATURE;
            default -> ReasonCode.DENY_VALIDATION;
        };
    }

    private static String normalizeDialect(String dialect) {
        if (dialect == null || dialect.isBlank()) {
            return "ansi";
        }
        return dialect.trim().toLowerCase(Locale.ROOT);
    }

    private static QueryValidator schemaValidatorForDialect(
        String dialect,
        CatalogSchema schema,
        SchemaValidationSettings settings
    ) {
        Objects.requireNonNull(schema, "schema must not be null");
        Objects.requireNonNull(settings, "settings must not be null");
        String normalized = normalizeDialect(dialect);
        return switch (normalized) {
            case "ansi" -> SchemaQueryValidator.of(schema, settings);
            case "postgresql", "postgres" -> SchemaQueryValidator.of(schema, mergeDialectSettings(settings, PostgresValidationDialect.of()));
            default -> throw new IllegalArgumentException("unsupported dialect: " + dialect);
        };
    }

    private static SchemaValidationSettings mergeDialectSettings(
        SchemaValidationSettings base,
        SchemaValidationDialect dialect
    ) {
        return SchemaValidationSettings.builder()
            .functionCatalog(dialect.functionCatalog())
            .accessPolicy(base.accessPolicy())
            .principal(base.principal())
            .limits(base.limits())
            .addRules(dialect.additionalRules())
            .addRules(base.additionalRules())
            .build();
    }
}

