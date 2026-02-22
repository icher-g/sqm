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

class SqlDecisionEngineFactoryCoverageTest {
    private static final CatalogSchema SCHEMA = CatalogSchema.of(
        CatalogTable.of("public", "users", CatalogColumn.of("id", CatalogType.LONG))
    );

    private static final Query SIMPLE_QUERY = Query.select(Expression.literal(1));
    private static final ExecutionContext ANSI_ANALYZE = ExecutionContext.of("ansi", ExecutionMode.ANALYZE);

    @Test
    void delegates_cover_schema_and_dialect_overloads() {
        var validator = (io.sqm.validate.api.QueryValidator) query -> new ValidationResult(java.util.List.of());
        var noopRewriter = SqlQueryRewriter.noop();
        var customRenderer = SqlQueryRenderer.ansi();

        assertEquals(DecisionKind.ALLOW,
            SqlDecisionEngine.validationOnly("ansi", SCHEMA).evaluate(SIMPLE_QUERY, ANSI_ANALYZE).kind());
        assertEquals(DecisionKind.ALLOW,
            SqlDecisionEngine.validationAndRewrite("ansi", validator).evaluate(SIMPLE_QUERY, ANSI_ANALYZE).kind());
        assertEquals(DecisionKind.ALLOW,
            SqlDecisionEngine.validationAndRewrite("ansi", SCHEMA, SchemaValidationSettings.defaults())
                .evaluate(SIMPLE_QUERY, ANSI_ANALYZE).kind());
        assertEquals(DecisionKind.ALLOW,
            SqlDecisionEngine.fullFlow("ansi", validator).evaluate(SIMPLE_QUERY, ANSI_ANALYZE).kind());
        assertEquals(DecisionKind.ALLOW,
            SqlDecisionEngine.fullFlow("ansi", SCHEMA, SchemaValidationSettings.defaults())
                .evaluate(SIMPLE_QUERY, ANSI_ANALYZE).kind());

        assertEquals(DecisionKind.ALLOW,
            SqlDecisionEngine.fullFlow("ansi", validator, noopRewriter, customRenderer)
                .evaluate(SIMPLE_QUERY, ANSI_ANALYZE).kind());
        assertEquals(DecisionKind.ALLOW,
            SqlDecisionEngine.fullFlow("ansi", SCHEMA, SchemaValidationSettings.defaults(), noopRewriter, customRenderer)
                .evaluate(SIMPLE_QUERY, ANSI_ANALYZE).kind());
    }

    @Test
    void built_in_rewrite_overloads_fail_fast_when_rules_are_unavailable() {
        var validator = (io.sqm.validate.api.QueryValidator) query -> new ValidationResult(java.util.List.of());

        assertThrows(IllegalArgumentException.class,
            () -> SqlDecisionEngine.validationAndRewrite("ansi", validator, BuiltInRewriteRule.LIMIT_INJECTION));
        assertThrows(IllegalArgumentException.class,
            () -> SqlDecisionEngine.validationAndRewrite(
                "ansi", SCHEMA, SchemaValidationSettings.defaults(), BuiltInRewriteRule.LIMIT_INJECTION));
        assertThrows(IllegalArgumentException.class,
            () -> SqlDecisionEngine.fullFlow("ansi", validator, BuiltInRewriteRule.LIMIT_INJECTION));
        assertThrows(IllegalArgumentException.class,
            () -> SqlDecisionEngine.fullFlow(
                "ansi", SCHEMA, SchemaValidationSettings.defaults(), BuiltInRewriteRule.LIMIT_INJECTION));
    }

    @Test
    void custom_renderer_full_flow_overload_validates_dialect_name() {
        var validator = (io.sqm.validate.api.QueryValidator) query -> new ValidationResult(java.util.List.of());

        assertThrows(IllegalArgumentException.class,
            () -> SqlDecisionEngine.fullFlow("mysql", validator, SqlQueryRewriter.noop(), SqlQueryRenderer.ansi()));
    }
}
