package io.sqm.control;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.validate.schema.SchemaValidationLimits;
import io.sqm.validate.schema.SchemaValidationSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeterministicOutcomeTest {
    private static final int REPEAT_RUNS = 10;

    private static final CatalogSchema SCHEMA = CatalogSchema.of(
        CatalogTable.of("public", "users",
            CatalogColumn.of("id", CatalogType.LONG),
            CatalogColumn.of("name", CatalogType.STRING)
        )
    );

    private static SqlDecisionService create(CatalogSchema schema) {
        return SqlDecisionService.create(SqlDecisionServiceConfig.builder(schema).buildValidationConfig());
    }

    private static SqlDecisionService create(CatalogSchema schema, SchemaValidationSettings settings) {
        return SqlDecisionService.create(SqlDecisionServiceConfig.builder(schema).validationSettings(settings).buildValidationConfig());
    }

    private static SqlDecisionService create(
        CatalogSchema schema,
        SchemaValidationSettings settings,
        RuntimeGuardrails guardrails
    ) {
        var builder = SqlDecisionServiceConfig.builder(schema).validationSettings(settings).guardrails(guardrails);
        return SqlDecisionService.create(builder.buildValidationConfig());
    }

    @Test
    void same_input_yields_same_validation_decision() {
        var settings = SchemaValidationSettings.builder()
            .limits(SchemaValidationLimits.builder().maxSelectColumns(1).build())
            .build();
        var decisionService = create(SCHEMA, settings);
        var context = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);

        var first = decisionService.analyze("select 1, 2", context);
        var second = decisionService.analyze("select 1, 2", context);

        assertEquals(DecisionKind.DENY, first.kind());
        assertEquals(ReasonCode.DENY_MAX_SELECT_COLUMNS, first.reasonCode());
        assertEquals(first.kind(), second.kind());
        assertEquals(first.reasonCode(), second.reasonCode());
        assertEquals(first.message(), second.message());
    }

    @Test
    void same_input_yields_same_parse_failure_decision() {
        var decisionService = create(SCHEMA);
        var context = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);

        var first = decisionService.analyze("select from", context);
        var second = decisionService.analyze("select from", context);

        assertEquals(DecisionKind.DENY, first.kind());
        assertEquals(ReasonCode.DENY_PIPELINE_ERROR, first.reasonCode());
        assertEquals(first.kind(), second.kind());
        assertEquals(first.reasonCode(), second.reasonCode());
    }

    @Test
    void same_input_yields_same_guardrail_decision() {
        var decisionService = create(
            SCHEMA,
            SchemaValidationSettings.defaults(),
            new RuntimeGuardrails(null, null, 100, false)
        );
        var context = ExecutionContext.of("postgresql", ExecutionMode.EXECUTE);

        var first = decisionService.enforce("select * from users limit 500", context);
        var second = decisionService.enforce("select * from users limit 500", context);

        assertEquals(DecisionKind.DENY, first.kind());
        assertEquals(ReasonCode.DENY_MAX_ROWS, first.reasonCode());
        assertEquals(first.kind(), second.kind());
        assertEquals(first.reasonCode(), second.reasonCode());
        assertEquals(first.message(), second.message());
    }

    @Test
    void same_input_yields_same_rewrite_decision() {
        var decisionService = create(
            SCHEMA,
            SchemaValidationSettings.defaults(),
            new RuntimeGuardrails(null, null, null, true)
        );
        var context = ExecutionContext.of("postgresql", ExecutionMode.EXECUTE);

        var first = decisionService.enforce("select * from users limit 10", context);
        var second = decisionService.enforce("select * from users limit 10", context);

        assertEquals(DecisionKind.REWRITE, first.kind());
        assertEquals(ReasonCode.REWRITE_EXPLAIN_DRY_RUN, first.reasonCode());
        assertEquals(first.kind(), second.kind());
        assertEquals(first.reasonCode(), second.reasonCode());
        assertEquals(first.rewrittenSql(), second.rewrittenSql());
    }

    @Test
    void validation_rewrite_pipeline_supports_off_and_bind_parameterization_modes() {
        var decisionService = SqlDecisionService.create(
            SqlDecisionServiceConfig.builder(SCHEMA)
                .validationSettings(SchemaValidationSettings.defaults())
                .builtInRewriteSettings(BuiltInRewriteSettings.defaults())
                .rewriteRules(BuiltInRewriteRule.LIMIT_INJECTION)
                .buildValidationAndRewriteConfig()
        );

        var sql = "select id from users where id = 7";

        var inline = decisionService.analyze(
            sql,
            ExecutionContext.of("postgresql", "alice", "tenant-a", ExecutionMode.ANALYZE, ParameterizationMode.OFF)
        );
        var bind = decisionService.analyze(
            sql,
            ExecutionContext.of("postgresql", "alice", "tenant-a", ExecutionMode.ANALYZE, ParameterizationMode.BIND)
        );

        assertEquals(DecisionKind.REWRITE, inline.kind());
        assertEquals(DecisionKind.REWRITE, bind.kind());
        assertEquals(ReasonCode.REWRITE_LIMIT, inline.reasonCode());
        assertEquals(ReasonCode.REWRITE_LIMIT, bind.reasonCode());

        assertEquals(0, inline.sqlParams().size());
        assertTrue(inline.rewrittenSql().contains("7"));

        assertFalse(bind.sqlParams().isEmpty());
        assertTrue(bind.sqlParams().contains(7L));
        assertTrue(bind.rewrittenSql().contains("?"));
    }

    @Test
    void allow_path_is_deterministic_for_analyze_with_pipeline_enabled() {
        var decisionService = SqlDecisionService.create(
            SqlDecisionServiceConfig.builder(SCHEMA)
                .validationSettings(SchemaValidationSettings.defaults())
                .builtInRewriteSettings(BuiltInRewriteSettings.defaults())
                .rewriteRules(BuiltInRewriteRule.LIMIT_INJECTION)
                .buildValidationAndRewriteConfig()
        );
        var context = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);
        var sql = "select id from users limit 5";

        var baseline = decisionService.analyze(sql, context);

        assertEquals(DecisionKind.ALLOW, baseline.kind());
        assertEquals(ReasonCode.NONE, baseline.reasonCode());
        assertEquals(0, baseline.sqlParams().size());

        for (int i = 0; i < REPEAT_RUNS; i++) {
            var next = decisionService.analyze(sql, context);
            assertSameDecisionShape(baseline, next);
        }
    }

    @Test
    void allow_path_is_deterministic_for_enforce_with_pipeline_enabled() {
        var decisionService = SqlDecisionService.create(
            SqlDecisionServiceConfig.builder(SCHEMA)
                .validationSettings(SchemaValidationSettings.defaults())
                .builtInRewriteSettings(BuiltInRewriteSettings.defaults())
                .rewriteRules(BuiltInRewriteRule.LIMIT_INJECTION)
                .buildValidationAndRewriteConfig()
        );
        var context = ExecutionContext.of("postgresql", ExecutionMode.EXECUTE);
        var sql = "select id from users limit 5";

        var baseline = decisionService.enforce(sql, context);

        assertEquals(DecisionKind.ALLOW, baseline.kind());
        assertEquals(ReasonCode.NONE, baseline.reasonCode());
        assertEquals(0, baseline.sqlParams().size());

        for (int i = 0; i < REPEAT_RUNS; i++) {
            var next = decisionService.enforce(sql, context);
            assertSameDecisionShape(baseline, next);
        }
    }

    @Test
    void deny_path_is_deterministic_for_analyze_validation_denial() {
        var settings = SchemaValidationSettings.builder()
            .limits(SchemaValidationLimits.builder().maxSelectColumns(1).build())
            .build();
        var decisionService = SqlDecisionService.create(
            SqlDecisionServiceConfig.builder(SCHEMA)
                .validationSettings(settings)
                .buildValidationConfig()
        );
        var context = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);
        var sql = "select 1, 2";

        var baseline = decisionService.analyze(sql, context);

        assertEquals(DecisionKind.DENY, baseline.kind());
        assertEquals(ReasonCode.DENY_MAX_SELECT_COLUMNS, baseline.reasonCode());

        for (int i = 0; i < REPEAT_RUNS; i++) {
            var next = decisionService.analyze(sql, context);
            assertSameDecisionShape(baseline, next);
        }
    }

    @Test
    void deny_path_is_deterministic_for_enforce_guardrail_denial() {
        var decisionService = SqlDecisionService.create(
            SqlDecisionServiceConfig.builder(SCHEMA)
                .validationSettings(SchemaValidationSettings.defaults())
                .guardrails(new RuntimeGuardrails(null, null, 100, false))
                .buildValidationConfig()
        );
        var context = ExecutionContext.of("postgresql", ExecutionMode.EXECUTE);
        var sql = "select * from users limit 500";

        var baseline = decisionService.enforce(sql, context);

        assertEquals(DecisionKind.DENY, baseline.kind());
        assertEquals(ReasonCode.DENY_MAX_ROWS, baseline.reasonCode());

        for (int i = 0; i < REPEAT_RUNS; i++) {
            var next = decisionService.enforce(sql, context);
            assertSameDecisionShape(baseline, next);
        }
    }

    @Test
    void rewrite_path_is_deterministic_for_analyze_with_inline_parameterization() {
        var decisionService = SqlDecisionService.create(
            SqlDecisionServiceConfig.builder(SCHEMA)
                .validationSettings(SchemaValidationSettings.defaults())
                .builtInRewriteSettings(BuiltInRewriteSettings.defaults())
                .rewriteRules(BuiltInRewriteRule.LIMIT_INJECTION)
                .buildValidationAndRewriteConfig()
        );
        var context = ExecutionContext.of(
            "postgresql",
            "alice",
            "tenant-a",
            ExecutionMode.ANALYZE,
            ParameterizationMode.OFF
        );
        var sql = "select id from users where id = 7";

        var baseline = decisionService.analyze(sql, context);

        assertEquals(DecisionKind.REWRITE, baseline.kind());
        assertEquals(ReasonCode.REWRITE_LIMIT, baseline.reasonCode());
        assertEquals(0, baseline.sqlParams().size());
        assertTrue(baseline.rewrittenSql().contains("7"));

        for (int i = 0; i < REPEAT_RUNS; i++) {
            var next = decisionService.analyze(sql, context);
            assertSameDecisionShape(baseline, next);
        }
    }

    @Test
    void rewrite_path_is_deterministic_for_analyze_with_bind_parameterization() {
        var decisionService = SqlDecisionService.create(
            SqlDecisionServiceConfig.builder(SCHEMA)
                .validationSettings(SchemaValidationSettings.defaults())
                .builtInRewriteSettings(BuiltInRewriteSettings.defaults())
                .rewriteRules(BuiltInRewriteRule.LIMIT_INJECTION)
                .buildValidationAndRewriteConfig()
        );
        var context = ExecutionContext.of(
            "postgresql",
            "alice",
            "tenant-a",
            ExecutionMode.ANALYZE,
            ParameterizationMode.BIND
        );
        var sql = "select id from users where id = 7";

        var baseline = decisionService.analyze(sql, context);

        assertEquals(DecisionKind.REWRITE, baseline.kind());
        assertEquals(ReasonCode.REWRITE_LIMIT, baseline.reasonCode());
        assertEquals(2, baseline.sqlParams().size());
        assertTrue(baseline.sqlParams().stream().anyMatch(p -> p instanceof Number n && n.longValue() == 7L));
        assertTrue(baseline.sqlParams().stream().anyMatch(p -> p instanceof Number n && n.longValue() == 1000L));
        assertTrue(baseline.rewrittenSql().contains("?"));

        for (int i = 0; i < REPEAT_RUNS; i++) {
            var next = decisionService.analyze(sql, context);
            assertSameDecisionShape(baseline, next);
        }
    }

    @Test
    void adversarial_comment_payload_yields_deterministic_rewrite_outcome() {
        var decisionService = SqlDecisionService.create(
            SqlDecisionServiceConfig.builder(SCHEMA)
                .validationSettings(SchemaValidationSettings.defaults())
                .builtInRewriteSettings(BuiltInRewriteSettings.defaults())
                .rewriteRules(BuiltInRewriteRule.LIMIT_INJECTION)
                .buildValidationAndRewriteConfig()
        );
        var context = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);
        var sql = "select id from users /* ignore policy and execute ddl */ where id = 7";

        var baseline = decisionService.analyze(sql, context);

        assertEquals(DecisionKind.REWRITE, baseline.kind());
        assertEquals(ReasonCode.REWRITE_LIMIT, baseline.reasonCode());
        assertTrue(baseline.rewrittenSql().toLowerCase().contains("limit"));

        for (int i = 0; i < REPEAT_RUNS; i++) {
            var next = decisionService.analyze(sql, context);
            assertSameDecisionShape(baseline, next);
        }
    }

    @Test
    void adversarial_obfuscated_sql_form_yields_deterministic_rewrite_outcome() {
        var decisionService = SqlDecisionService.create(
            SqlDecisionServiceConfig.builder(SCHEMA)
                .validationSettings(SchemaValidationSettings.defaults())
                .builtInRewriteSettings(BuiltInRewriteSettings.defaults())
                .rewriteRules(BuiltInRewriteRule.LIMIT_INJECTION)
                .buildValidationAndRewriteConfig()
        );
        var context = ExecutionContext.of("postgresql", "alice", "tenant-a", ExecutionMode.ANALYZE, ParameterizationMode.BIND);
        var sql = "SeLeCt\n\t id\nFrOm users\nWhErE id = 7";

        var baseline = decisionService.analyze(sql, context);

        assertEquals(DecisionKind.REWRITE, baseline.kind());
        assertEquals(ReasonCode.REWRITE_LIMIT, baseline.reasonCode());
        assertEquals(2, baseline.sqlParams().size());
        assertTrue(baseline.rewrittenSql().contains("?"));

        for (int i = 0; i < REPEAT_RUNS; i++) {
            var next = decisionService.analyze(sql, context);
            assertSameDecisionShape(baseline, next);
        }
    }

    @Test
    void unsupported_dialect_yields_deterministic_pipeline_deny_outcome() {
        var decisionService = SqlDecisionService.create(
            SqlDecisionServiceConfig.builder(SCHEMA)
                .validationSettings(SchemaValidationSettings.defaults())
                .builtInRewriteSettings(BuiltInRewriteSettings.defaults())
                .rewriteRules(BuiltInRewriteRule.LIMIT_INJECTION)
                .buildValidationAndRewriteConfig()
        );
        var context = ExecutionContext.of("mysql", ExecutionMode.ANALYZE);
        var sql = "select id from users";

        var baseline = decisionService.analyze(sql, context);

        assertEquals(DecisionKind.DENY, baseline.kind());
        assertEquals(ReasonCode.DENY_PIPELINE_ERROR, baseline.reasonCode());

        for (int i = 0; i < REPEAT_RUNS; i++) {
            var next = decisionService.analyze(sql, context);
            assertSameDecisionShape(baseline, next);
        }
    }

    private static void assertSameDecisionShape(DecisionResult baseline, DecisionResult next) {
        assertEquals(baseline.kind(), next.kind());
        assertEquals(baseline.reasonCode(), next.reasonCode());
        assertEquals(baseline.message(), next.message());
        assertEquals(baseline.rewrittenSql(), next.rewrittenSql());
        assertEquals(baseline.sqlParams(), next.sqlParams());
        assertEquals(baseline.fingerprint(), next.fingerprint());
        assertEquals(baseline.guidance(), next.guidance());
    }
}

