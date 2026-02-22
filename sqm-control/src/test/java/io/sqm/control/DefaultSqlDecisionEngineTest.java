package io.sqm.control;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.core.Expression;
import io.sqm.core.Query;
import io.sqm.validate.api.ValidationProblem;
import io.sqm.validate.api.ValidationResult;
import io.sqm.validate.schema.SchemaValidationSettings;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultSqlDecisionEngineTest {
    private static final CatalogSchema SCHEMA = CatalogSchema.of(
        CatalogTable.of("public", "users", CatalogColumn.of("id", CatalogType.LONG))
    );

    @Test
    void allows_when_validation_passes() {
        var engine = SqlDecisionEngine.validationOnly(query -> new ValidationResult(List.of()));
        var result = engine.evaluate(Query.select(Expression.literal(1)).build(), ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));

        assertEquals(DecisionKind.ALLOW, result.kind());
        assertEquals(ReasonCode.NONE, result.reasonCode());
    }

    @Test
    void maps_policy_errors_to_reason_codes() {
        var engine = SqlDecisionEngine.validationOnly(query -> new ValidationResult(List.of(
            new ValidationProblem(ValidationProblem.Code.POLICY_TABLE_DENIED, "table denied")
        )));

        var result = engine.evaluate(Query.select(Expression.literal(1)).build(), ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));
        assertEquals(DecisionKind.DENY, result.kind());
        assertEquals(ReasonCode.DENY_TABLE, result.reasonCode());
    }

    @Test
    void maps_generic_validation_to_deny_validation() {
        var engine = SqlDecisionEngine.validationOnly(query -> new ValidationResult(List.of(
            new ValidationProblem(ValidationProblem.Code.TYPE_MISMATCH, "type mismatch")
        )));

        var result = engine.evaluate(Query.select(Expression.literal(1)).build(), ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));
        assertEquals(DecisionKind.DENY, result.kind());
        assertEquals(ReasonCode.DENY_VALIDATION, result.reasonCode());
    }

    @Test
    void validates_input_arguments() {
        var engine = SqlDecisionEngine.validationOnly(query -> new ValidationResult(List.of()));

        assertThrows(NullPointerException.class, () -> engine.evaluate(null, ExecutionContext.of("postgresql", ExecutionMode.ANALYZE)));
        assertThrows(NullPointerException.class, () -> engine.evaluate(Query.select(Expression.literal(1)).build(), null));
    }

    @Test
    void maps_dialect_validation_to_unsupported_dialect_reason() {
        var engine = SqlDecisionEngine.validationOnly(query -> new ValidationResult(List.of(
            new ValidationProblem(ValidationProblem.Code.DIALECT_CLAUSE_INVALID, "unsupported for dialect")
        )));

        var result = engine.evaluate(Query.select(Expression.literal(1)).build(), ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));
        assertEquals(DecisionKind.DENY, result.kind());
        assertEquals(ReasonCode.DENY_UNSUPPORTED_DIALECT_FEATURE, result.reasonCode());
    }

    @Test
    void returns_rewrite_when_rewriter_changes_query_and_rewritten_query_validates() {
        var validatorCalls = new AtomicInteger();
        var engine = SqlDecisionEngine.fullFlow(
            query -> {
                validatorCalls.incrementAndGet();
                return new ValidationResult(List.of());
            },
            (query, context) -> QueryRewriteResult.rewritten(
                Query.select(Expression.literal(2)).build(),
                "limit-injection",
                ReasonCode.REWRITE_LIMIT
            ),
            (query, context) -> "select 2 limit 100"
        );

        var result = engine.evaluate(Query.select(Expression.literal(1)).build(), ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));

        assertEquals(DecisionKind.REWRITE, result.kind());
        assertEquals(ReasonCode.REWRITE_LIMIT, result.reasonCode());
        assertEquals("select 2 limit 100", result.rewrittenSql());
        assertTrue(result.message().contains("limit-injection"));
        assertEquals(2, validatorCalls.get());
    }

    @Test
    void denies_when_rewritten_query_fails_validation() {
        var validatorCalls = new AtomicInteger();
        var engine = SqlDecisionEngine.fullFlow(
            query -> {
                int call = validatorCalls.incrementAndGet();
                if (call == 1) {
                    return new ValidationResult(List.of());
                }
                return new ValidationResult(List.of(
                    new ValidationProblem(ValidationProblem.Code.POLICY_MAX_JOINS_EXCEEDED, "too many joins")
                ));
            },
            (query, context) -> QueryRewriteResult.rewritten(
                Query.select(Expression.literal(2)).build(),
                "qualification",
                ReasonCode.REWRITE_QUALIFICATION
            ),
            (query, context) -> "select 2"
        );

        var result = engine.evaluate(Query.select(Expression.literal(1)).build(), ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));

        assertEquals(DecisionKind.DENY, result.kind());
        assertEquals(ReasonCode.DENY_MAX_JOINS, result.reasonCode());
        assertEquals(2, validatorCalls.get());
    }

    @Test
    void validates_pipeline_factory_arguments() {
        var validator = (io.sqm.validate.api.QueryValidator) query -> new ValidationResult(List.of());
        var rewriter = SqlQueryRewriter.noop();
        var renderer = (SqlQueryRenderer) (query, context) -> "select 1";

        assertThrows(NullPointerException.class, () -> SqlDecisionEngine.fullFlow(null, rewriter, renderer));
        assertThrows(NullPointerException.class, () -> SqlDecisionEngine.fullFlow(validator, null, renderer));
        assertThrows(NullPointerException.class, () -> SqlDecisionEngine.fullFlow(validator, rewriter, null));
    }

    @Test
    void dialect_factories_validate_supported_dialect_names() {
        var validator = (io.sqm.validate.api.QueryValidator) query -> new ValidationResult(List.of());

        assertThrows(IllegalArgumentException.class,
            () -> SqlDecisionEngine.validationAndRewrite("mysql", validator, SqlQueryRewriter.noop()));
    }

    @Test
    void dialect_factories_default_to_ansi_when_dialect_missing() {
        var engine1 = SqlDecisionEngine.validationOnly(null, SCHEMA, SchemaValidationSettings.defaults());
        var engine2 = SqlDecisionEngine.validationOnly(" ", SCHEMA, SchemaValidationSettings.defaults());
        var engine3 = SqlDecisionEngine.validationAndRewrite(
            null,
            query -> new ValidationResult(List.of()),
            SqlQueryRewriter.noop()
        );

        assertEquals(DecisionKind.ALLOW,
            engine1.evaluate(Query.select(Expression.literal(1)).build(), ExecutionContext.of("ansi", ExecutionMode.ANALYZE)).kind());
        assertEquals(DecisionKind.ALLOW,
            engine2.evaluate(Query.select(Expression.literal(1)).build(), ExecutionContext.of("ansi", ExecutionMode.ANALYZE)).kind());
        assertEquals(DecisionKind.ALLOW,
            engine3.evaluate(Query.select(Expression.literal(1)).build(), ExecutionContext.of("ansi", ExecutionMode.ANALYZE)).kind());
    }

    @Test
    void dialect_rewrite_factory_uses_selected_renderer() {
        var engine = SqlDecisionEngine.validationAndRewrite(
            "postgres",
            query -> new ValidationResult(List.of()),
            (query, context) -> QueryRewriteResult.rewritten(
                Query.select(Expression.literal(1)).build(),
                "qualification",
                ReasonCode.REWRITE_QUALIFICATION
            )
        );

        var result = engine.evaluate(Query.select(Expression.literal(1)).build(), ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));
        assertEquals(DecisionKind.REWRITE, result.kind());
        assertEquals(ReasonCode.REWRITE_QUALIFICATION, result.reasonCode());
        assertTrue(result.rewrittenSql().toLowerCase().contains("select"));
    }

    @Test
    void built_in_limit_injection_rewrite_renders_sql() {
        var engine = SqlDecisionEngine.validationAndRewrite(
            "postgresql",
            query -> new ValidationResult(List.of()),
            BuiltInRewriteRule.LIMIT_INJECTION
        );

        var result = engine.evaluate(Query.select(Expression.literal(1)).build(), ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));
        assertEquals(DecisionKind.REWRITE, result.kind());
        assertEquals(ReasonCode.REWRITE_LIMIT, result.reasonCode());
        assertTrue(result.rewrittenSql().toLowerCase().contains("limit 1000"));
    }

    @Test
    void schema_dialect_factory_builds_validation_engine() {
        var engine = SqlDecisionEngine.validationOnly(
            "postgresql",
            SCHEMA,
            SchemaValidationSettings.defaults()
        );

        var result = engine.evaluate(Query.select(Expression.literal(1)).build(), ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));
        assertEquals(DecisionKind.ALLOW, result.kind());
    }

    @Test
    void schema_built_in_qualification_rewrite_is_wired_in_engine_factory() {
        var engine = SqlDecisionEngine.validationAndRewrite(
            "postgresql",
            SCHEMA,
            SchemaValidationSettings.defaults(),
            BuiltInRewriteRule.SCHEMA_QUALIFICATION
        );
        var query = SqlQueryParser.standard().parse("select id from users", ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));

        var result = engine.evaluate(query, ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));

        assertEquals(DecisionKind.REWRITE, result.kind());
        assertEquals(ReasonCode.REWRITE_QUALIFICATION, result.reasonCode());
        assertTrue(result.rewrittenSql().toLowerCase().contains("from public.users"));
    }

    @Test
    void rewrite_deny_exception_is_mapped_to_deny_decision() {
        var engine = SqlDecisionEngine.fullFlow(
            query -> new ValidationResult(List.of()),
            (query, context) -> {
                throw new RewriteDenyException(ReasonCode.DENY_MAX_ROWS, "limit too high");
            },
            SqlQueryRenderer.ansi()
        );

        var result = engine.evaluate(Query.select(Expression.literal(1)).build(), ExecutionContext.of("ansi", ExecutionMode.ANALYZE));

        assertEquals(DecisionKind.DENY, result.kind());
        assertEquals(ReasonCode.DENY_MAX_ROWS, result.reasonCode());
    }

    @Test
    void null_renderer_output_fails_fast_on_rewrite_path() {
        var engine = SqlDecisionEngine.fullFlow(
            query -> new ValidationResult(List.of()),
            (query, context) -> QueryRewriteResult.rewritten(
                Query.select(Expression.literal(2)).build(),
                "r",
                ReasonCode.REWRITE_LIMIT
            ),
            (query, context) -> null
        );

        assertThrows(NullPointerException.class,
            () -> engine.evaluate(Query.select(Expression.literal(1)).build(), ExecutionContext.of("ansi", ExecutionMode.ANALYZE)));
    }
}
