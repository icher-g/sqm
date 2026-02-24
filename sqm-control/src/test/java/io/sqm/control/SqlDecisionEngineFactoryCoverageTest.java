package io.sqm.control;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.core.Expression;
import io.sqm.core.Query;
import io.sqm.validate.api.ValidationResult;
import io.sqm.validate.schema.SchemaValidationSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlDecisionEngineFactoryCoverageTest {
    // Touch file to force test recompilation after Query.select(...) return-type refactor.
    private static final CatalogSchema SCHEMA = CatalogSchema.of(
        CatalogTable.of("public", "users", CatalogColumn.of("id", CatalogType.LONG))
    );

    private static final ExecutionContext ANSI_ANALYZE = ExecutionContext.of("ansi", ExecutionMode.ANALYZE);

    private static Query query() {
        return Query.select(Expression.literal(1)).build();
    }

    @Test
    void delegates_cover_schema_and_dialect_overloads() {
        var validator = (io.sqm.validate.api.QueryValidator) query -> new ValidationResult(java.util.List.of());
        var noopRewriter = SqlQueryRewriter.noop();
        var customRenderer = SqlQueryRenderer.ansi();

        assertEquals(DecisionKind.ALLOW,
            SqlDecisionEngine.validationOnly("ansi", SCHEMA).evaluate(query(), ANSI_ANALYZE).kind());
        assertEquals(DecisionKind.REWRITE,
            SqlDecisionEngine.validationAndRewrite("ansi", validator).evaluate(query(), ANSI_ANALYZE).kind());
        assertEquals(DecisionKind.REWRITE,
            SqlDecisionEngine.validationAndRewrite("ansi", SCHEMA, SchemaValidationSettings.defaults())
                .evaluate(query(), ANSI_ANALYZE).kind());
        assertEquals(DecisionKind.REWRITE,
            SqlDecisionEngine.fullFlow("ansi", validator).evaluate(query(), ANSI_ANALYZE).kind());
        assertEquals(DecisionKind.REWRITE,
            SqlDecisionEngine.fullFlow("ansi", SCHEMA, SchemaValidationSettings.defaults())
                .evaluate(query(), ANSI_ANALYZE).kind());

        assertEquals(DecisionKind.ALLOW,
            SqlDecisionEngine.fullFlow("ansi", validator, noopRewriter, customRenderer)
                .evaluate(query(), ANSI_ANALYZE).kind());
        assertEquals(DecisionKind.ALLOW,
            SqlDecisionEngine.fullFlow("ansi", SCHEMA, SchemaValidationSettings.defaults(), noopRewriter, customRenderer)
                .evaluate(query(), ANSI_ANALYZE).kind());
    }

    @Test
    void built_in_rewrite_overloads_support_limit_and_identifier_normalization_and_reject_unavailable_rules() {
        var validator = (io.sqm.validate.api.QueryValidator) query -> new ValidationResult(java.util.List.of());

        assertEquals(DecisionKind.REWRITE,
            SqlDecisionEngine.validationAndRewrite("postgresql", validator, BuiltInRewriteRule.LIMIT_INJECTION)
                .evaluate(query(), ExecutionContext.of("postgresql", ExecutionMode.ANALYZE)).kind());
        assertEquals(DecisionKind.REWRITE,
            SqlDecisionEngine.validationAndRewrite(
                "postgresql", SCHEMA, SchemaValidationSettings.defaults(), BuiltInRewriteRule.LIMIT_INJECTION)
                .evaluate(query(), ExecutionContext.of("postgresql", ExecutionMode.ANALYZE)).kind());
        assertEquals(DecisionKind.REWRITE,
            SqlDecisionEngine.fullFlow("postgresql", validator, BuiltInRewriteRule.LIMIT_INJECTION)
                .evaluate(query(), ExecutionContext.of("postgresql", ExecutionMode.ANALYZE)).kind());
        assertEquals(DecisionKind.REWRITE,
            SqlDecisionEngine.fullFlow(
                "postgresql", SCHEMA, SchemaValidationSettings.defaults(), BuiltInRewriteRule.LIMIT_INJECTION)
                .evaluate(query(), ExecutionContext.of("postgresql", ExecutionMode.ANALYZE)).kind());

        var normalizeQuery = SqlQueryParser.standard().parse(
            "select U.ID from Public.Users as U",
            ExecutionContext.of("postgresql", ExecutionMode.ANALYZE)
        );
        var normalizeResult = SqlDecisionEngine.validationAndRewrite(
            "postgresql",
            validator,
            BuiltInRewriteRule.IDENTIFIER_NORMALIZATION
        ).evaluate(normalizeQuery, ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));
        assertEquals(DecisionKind.REWRITE, normalizeResult.kind());
        assertEquals(ReasonCode.REWRITE_IDENTIFIER_NORMALIZATION, normalizeResult.reasonCode());

        assertThrows(IllegalArgumentException.class,
            () -> SqlDecisionEngine.validationAndRewrite("ansi", validator, BuiltInRewriteRule.LITERAL_PARAMETERIZATION));
    }

    @Test
    void built_in_rewrite_settings_overloads_apply_configured_limit_value() {
        var validator = (io.sqm.validate.api.QueryValidator) query -> new ValidationResult(java.util.List.of());
        var settings = new BuiltInRewriteSettings(37);
        var context = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);

        var nonSchema = SqlDecisionEngine.validationAndRewrite(
            "postgresql",
            validator,
            settings,
            BuiltInRewriteRule.LIMIT_INJECTION
        ).evaluate(query(), context);

        var schema = SqlDecisionEngine.fullFlow(
            "postgresql",
            SCHEMA,
            SchemaValidationSettings.defaults(),
            settings,
            BuiltInRewriteRule.LIMIT_INJECTION
        ).evaluate(query(), context);

        assertEquals(DecisionKind.REWRITE, nonSchema.kind());
        assertTrue(nonSchema.rewrittenSql().toLowerCase().contains("limit 37"));
        assertEquals(DecisionKind.REWRITE, schema.kind());
        assertTrue(schema.rewrittenSql().toLowerCase().contains("limit 37"));
    }

    @Test
    void limit_excess_policy_modes_produce_deny_or_clamp_rewrite() {
        var validator = (io.sqm.validate.api.QueryValidator) query -> new ValidationResult(java.util.List.of());
        var context = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);
        var query = SqlQueryParser.standard().parse("select 1 limit 99", context);

        var denyResult = SqlDecisionEngine.validationAndRewrite(
            "postgresql",
            validator,
            new BuiltInRewriteSettings(10, 10, BuiltInRewriteSettings.LimitExcessMode.DENY),
            BuiltInRewriteRule.LIMIT_INJECTION
        ).evaluate(SqlQueryParser.standard().parse("select 1 limit 99", context), context);

        var clampResult = SqlDecisionEngine.validationAndRewrite(
            "postgresql",
            validator,
            new BuiltInRewriteSettings(1000, 10, BuiltInRewriteSettings.LimitExcessMode.CLAMP),
            BuiltInRewriteRule.LIMIT_INJECTION
        ).evaluate(query, context);

        assertEquals(DecisionKind.DENY, denyResult.kind());
        assertEquals(ReasonCode.DENY_MAX_ROWS, denyResult.reasonCode());
        assertEquals(DecisionKind.REWRITE, clampResult.kind());
        assertEquals(ReasonCode.REWRITE_LIMIT, clampResult.reasonCode());
        assertTrue(clampResult.rewrittenSql().toLowerCase().contains("limit 10"));
    }

    @Test
    void canonicalization_built_in_overload_rewrites_arithmetic_expression() {
        var validator = (io.sqm.validate.api.QueryValidator) query -> new ValidationResult(java.util.List.of());
        var context = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);
        var query = SqlQueryParser.standard().parse("select 1 + 0", context);

        var result = SqlDecisionEngine.validationAndRewrite(
            "postgresql",
            validator,
            BuiltInRewriteRule.CANONICALIZATION
        ).evaluate(query, context);

        assertEquals(DecisionKind.REWRITE, result.kind());
        assertEquals(ReasonCode.REWRITE_CANONICALIZATION, result.reasonCode());
        assertTrue(result.rewrittenSql().toLowerCase().contains("select 1"));
    }

    @Test
    void custom_renderer_full_flow_overload_validates_dialect_name() {
        var validator = (io.sqm.validate.api.QueryValidator) query -> new ValidationResult(java.util.List.of());

        assertThrows(IllegalArgumentException.class,
            () -> SqlDecisionEngine.fullFlow("mysql", validator, SqlQueryRewriter.noop(), SqlQueryRenderer.ansi()));
    }

    @Test
    void additional_factory_overloads_delegate_successfully() {
        var validator = (io.sqm.validate.api.QueryValidator) query -> new ValidationResult(java.util.List.of());
        var settings = SchemaValidationSettings.defaults();
        var rewriteSettings = new BuiltInRewriteSettings(15);
        var context = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);

        assertEquals(DecisionKind.REWRITE,
            SqlDecisionEngine.validationAndRewrite("postgresql", validator, rewriteSettings).evaluate(query(), context).kind());
        assertEquals(DecisionKind.REWRITE,
            SqlDecisionEngine.fullFlow("postgresql", validator, rewriteSettings).evaluate(query(), context).kind());
        assertEquals(DecisionKind.REWRITE,
            SqlDecisionEngine.fullFlow("postgresql", validator, rewriteSettings, BuiltInRewriteRule.LIMIT_INJECTION)
                .evaluate(query(), context).kind());

        assertEquals(DecisionKind.REWRITE,
            SqlDecisionEngine.validationAndRewrite("postgresql", SCHEMA, settings).evaluate(query(), context).kind());
        assertEquals(DecisionKind.REWRITE,
            SqlDecisionEngine.validationAndRewrite("postgresql", SCHEMA, settings, rewriteSettings).evaluate(query(), context).kind());
        assertEquals(DecisionKind.REWRITE,
            SqlDecisionEngine.validationAndRewrite("postgresql", SCHEMA, settings, rewriteSettings, BuiltInRewriteRule.LIMIT_INJECTION)
                .evaluate(query(), context).kind());

        assertEquals(DecisionKind.REWRITE,
            SqlDecisionEngine.fullFlow("postgresql", SCHEMA, settings).evaluate(query(), context).kind());
        assertEquals(DecisionKind.REWRITE,
            SqlDecisionEngine.fullFlow("postgresql", SCHEMA, settings, rewriteSettings).evaluate(query(), context).kind());
        assertEquals(DecisionKind.REWRITE,
            SqlDecisionEngine.fullFlow("postgresql", SCHEMA, settings, rewriteSettings, BuiltInRewriteRule.LIMIT_INJECTION)
                .evaluate(query(), context).kind());

        assertEquals(DecisionKind.ALLOW,
            SqlDecisionEngine.validationOnly("postgresql", SCHEMA).evaluate(query(), context).kind());
        assertEquals(DecisionKind.ALLOW,
            SqlDecisionEngine.validationAndRewrite("postgresql", SCHEMA, settings, SqlQueryRewriter.noop()).evaluate(query(), context).kind());
        assertEquals(DecisionKind.ALLOW,
            SqlDecisionEngine.fullFlow("postgresql", SCHEMA, settings, SqlQueryRewriter.noop()).evaluate(query(), context).kind());
    }
}
