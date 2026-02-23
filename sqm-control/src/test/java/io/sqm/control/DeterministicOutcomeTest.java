package io.sqm.control;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.validate.schema.SchemaValidationLimits;
import io.sqm.validate.schema.SchemaValidationSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeterministicOutcomeTest {

    private static final CatalogSchema SCHEMA = CatalogSchema.of(
        CatalogTable.of("public", "users",
            CatalogColumn.of("id", CatalogType.LONG),
            CatalogColumn.of("name", CatalogType.STRING)
        )
    );

    private static SqlMiddleware create(CatalogSchema schema) {
        return SqlMiddleware.create(SqlMiddlewareConfig.forValidation(schema));
    }

    private static SqlMiddleware create(CatalogSchema schema, SchemaValidationSettings settings) {
        return SqlMiddleware.create(SqlMiddlewareConfig.forValidation(null, schema, settings));
    }

    private static SqlMiddleware create(
        CatalogSchema schema,
        SchemaValidationSettings settings,
        RuntimeGuardrails guardrails
    ) {
        return SqlMiddleware.create(SqlMiddlewareConfig.forValidation(null, schema, settings).withGuardrails(guardrails));
    }

    @Test
    void same_input_yields_same_validation_decision() {
        var settings = SchemaValidationSettings.builder()
            .limits(SchemaValidationLimits.builder().maxSelectColumns(1).build())
            .build();
        var middleware = create(SCHEMA, settings);
        var context = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);

        var first = middleware.analyze("select 1, 2", context);
        var second = middleware.analyze("select 1, 2", context);

        assertEquals(DecisionKind.DENY, first.kind());
        assertEquals(ReasonCode.DENY_MAX_SELECT_COLUMNS, first.reasonCode());
        assertEquals(first.kind(), second.kind());
        assertEquals(first.reasonCode(), second.reasonCode());
        assertEquals(first.message(), second.message());
    }

    @Test
    void same_input_yields_same_parse_failure_decision() {
        var middleware = create(SCHEMA);
        var context = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);

        var first = middleware.analyze("select from", context);
        var second = middleware.analyze("select from", context);

        assertEquals(DecisionKind.DENY, first.kind());
        assertEquals(ReasonCode.DENY_PIPELINE_ERROR, first.reasonCode());
        assertEquals(first.kind(), second.kind());
        assertEquals(first.reasonCode(), second.reasonCode());
    }

    @Test
    void same_input_yields_same_guardrail_decision() {
        var middleware = create(
            SCHEMA,
            SchemaValidationSettings.defaults(),
            new RuntimeGuardrails(null, null, 100, false)
        );
        var context = ExecutionContext.of("postgresql", ExecutionMode.EXECUTE);

        var first = middleware.enforce("select * from users limit 500", context);
        var second = middleware.enforce("select * from users limit 500", context);

        assertEquals(DecisionKind.DENY, first.kind());
        assertEquals(ReasonCode.DENY_MAX_ROWS, first.reasonCode());
        assertEquals(first.kind(), second.kind());
        assertEquals(first.reasonCode(), second.reasonCode());
        assertEquals(first.message(), second.message());
    }

    @Test
    void same_input_yields_same_rewrite_decision() {
        var middleware = create(
            SCHEMA,
            SchemaValidationSettings.defaults(),
            new RuntimeGuardrails(null, null, null, true)
        );
        var context = ExecutionContext.of("postgresql", ExecutionMode.EXECUTE);

        var first = middleware.enforce("select * from users limit 10", context);
        var second = middleware.enforce("select * from users limit 10", context);

        assertEquals(DecisionKind.REWRITE, first.kind());
        assertEquals(ReasonCode.REWRITE_EXPLAIN_DRY_RUN, first.reasonCode());
        assertEquals(first.kind(), second.kind());
        assertEquals(first.reasonCode(), second.reasonCode());
        assertEquals(first.rewrittenSql(), second.rewrittenSql());
    }
}
