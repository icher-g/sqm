package io.sqm.control;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.validate.schema.SchemaValidationLimits;
import io.sqm.validate.schema.SchemaValidationSettings;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultSqlMiddlewareTest {

    private static final CatalogSchema SCHEMA = CatalogSchema.of(
        CatalogTable.of("public", "users",
            CatalogColumn.of("id", CatalogType.LONG),
            CatalogColumn.of("name", CatalogType.STRING)
        )
    );

    @Test
    void default_factory_creates_working_middleware() {
        var middleware = SqlMiddleware.of(SCHEMA);
        var context = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);

        var result = middleware.analyze("select 1", context);
        assertEquals(DecisionKind.ALLOW, result.kind());
    }

    @Test
    void factory_applies_validation_settings() {
        var settings = SchemaValidationSettings.builder()
            .limits(SchemaValidationLimits.builder().maxSelectColumns(1).build())
            .build();
        var middleware = SqlMiddleware.of(SCHEMA, settings);

        var result = middleware.analyze("select 1, 2", ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));
        assertEquals(DecisionKind.DENY, result.kind());
        assertEquals(ReasonCode.DENY_MAX_SELECT_COLUMNS, result.reasonCode());
    }

    @Test
    void factory_applies_runtime_guardrails() {
        var middleware = SqlMiddleware.of(
            SCHEMA,
            SchemaValidationSettings.defaults(),
            new RuntimeGuardrails(null, null, 100, false)
        );

        var result = middleware.enforce("select 1", ExecutionContext.of("postgresql", ExecutionMode.EXECUTE));
        assertEquals(DecisionKind.DENY, result.kind());
        assertEquals(ReasonCode.DENY_MAX_ROWS, result.reasonCode());
    }

    @Test
    void factory_allows_custom_audit_wiring() {
        var audit = InMemoryAuditEventPublisher.create();
        var middleware = SqlMiddleware.of(
            SCHEMA,
            SchemaValidationSettings.defaults(),
            RuntimeGuardrails.disabled(),
            audit,
            SqlDecisionExplainer.basic(),
            DefaultSqlQueryParser.standard()
        );

        middleware.analyze("select 1", ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));
        assertEquals(1, audit.events().size());
        assertEquals(List.of(), audit.events().getFirst().appliedRules());
    }

    @Test
    void schema_factory_validates_required_arguments() {
        assertThrows(NullPointerException.class, () -> SqlMiddleware.of((io.sqm.catalog.model.CatalogSchema) null));
        assertThrows(NullPointerException.class, () -> SqlMiddleware.of(SCHEMA, null));
        assertThrows(NullPointerException.class, () -> SqlMiddleware.of(SCHEMA, SchemaValidationSettings.defaults(), null));
    }

    @Test
    void explicit_factory_validates_required_arguments() {
        var engine = (SqlDecisionEngine) (query, context) -> DecisionResult.allow();
        var explainer = SqlDecisionExplainer.basic();
        var audit = AuditEventPublisher.noop();
        var guardrails = RuntimeGuardrails.disabled();
        var parser = DefaultSqlQueryParser.standard();

        assertThrows(NullPointerException.class, () -> SqlMiddleware.of(null, explainer, audit, guardrails, parser));
        assertThrows(NullPointerException.class, () -> SqlMiddleware.of(engine, null, audit, guardrails, parser));
        assertThrows(NullPointerException.class, () -> SqlMiddleware.of(engine, explainer, null, guardrails, parser));
        assertThrows(NullPointerException.class, () -> SqlMiddleware.of(engine, explainer, audit, null, parser));
        assertThrows(NullPointerException.class, () -> SqlMiddleware.of(engine, explainer, audit, guardrails, null));
    }
}
