package io.sqm.control;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.control.audit.InMemoryAuditEventPublisher;
import io.sqm.validate.schema.SchemaValidationLimits;
import io.sqm.validate.schema.SchemaValidationSettings;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("SameParameterValue")
class DefaultSqlMiddlewareTest {

    private static final CatalogSchema SCHEMA = CatalogSchema.of(
        CatalogTable.of("public", "users",
            CatalogColumn.of("id", CatalogType.LONG),
            CatalogColumn.of("name", CatalogType.STRING)
        )
    );

    private static SqlMiddleware create(CatalogSchema schema) {
        return SqlMiddleware.create(SqlMiddlewareConfig.forValidation(schema));
    }

    private static SqlMiddleware create(String dialect, CatalogSchema schema) {
        return SqlMiddleware.create(SqlMiddlewareConfig.forValidation(dialect, schema));
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

    private static SqlMiddleware create(
        CatalogSchema schema,
        SchemaValidationSettings settings,
        RuntimeGuardrails guardrails,
        AuditEventPublisher auditPublisher,
        SqlDecisionExplainer explainer,
        SqlQueryParser queryParser
    ) {
        return SqlMiddleware.create(
            SqlMiddlewareConfig.forValidation(null, schema, settings)
                .withGuardrails(guardrails)
                .withAuditPublisher(auditPublisher)
                .withExplainer(explainer)
                .withQueryParser(queryParser)
        );
    }

    private static SqlMiddleware create(
        SqlDecisionEngine engine,
        SqlDecisionExplainer explainer,
        AuditEventPublisher auditPublisher,
        RuntimeGuardrails guardrails,
        SqlQueryParser queryParser
    ) {
        return SqlMiddleware.create(
            SqlMiddlewareConfig.forEngine(engine)
                .withExplainer(explainer)
                .withAuditPublisher(auditPublisher)
                .withGuardrails(guardrails)
                .withQueryParser(queryParser)
        );
    }

    @Test
    void default_factory_creates_working_middleware() {
        var middleware = create(SCHEMA);
        var context = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);

        var result = middleware.analyze("select 1", context);
        assertEquals(DecisionKind.ALLOW, result.kind());
    }

    @Test
    void factory_applies_validation_settings() {
        var settings = SchemaValidationSettings.builder()
            .limits(SchemaValidationLimits.builder().maxSelectColumns(1).build())
            .build();
        var middleware = create(SCHEMA, settings);

        var result = middleware.analyze("select 1, 2", ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));
        assertEquals(DecisionKind.DENY, result.kind());
        assertEquals(ReasonCode.DENY_MAX_SELECT_COLUMNS, result.reasonCode());
    }

    @Test
    void factory_applies_runtime_guardrails() {
        var middleware = create(
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
        var middleware = create(
            SCHEMA,
            SchemaValidationSettings.defaults(),
            RuntimeGuardrails.disabled(),
            audit,
            SqlDecisionExplainer.basic(),
            SqlQueryParser.standard()
        );

        middleware.analyze("select 1", ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));
        assertEquals(1, audit.events().size());
        assertEquals(List.of(), audit.events().getFirst().appliedRules());
    }

    @Test
    void dialect_schema_factory_uses_dialect_aware_validation_wiring() {
        var middleware = create("postgres", SCHEMA);

        var result = middleware.analyze("select 1", ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));
        assertEquals(DecisionKind.ALLOW, result.kind());
    }

    @Test
    void dialect_schema_factory_validates_required_arguments() {
        assertThrows(NullPointerException.class, () -> create("postgresql", null));
        assertThrows(NullPointerException.class, () -> SqlMiddlewareConfig.forValidation("postgresql", SCHEMA, null));
        assertThrows(IllegalArgumentException.class, () -> SqlMiddlewareConfig.forValidation("mysql", SCHEMA));
    }

    @Test
    void dialect_schema_factory_defaults_to_ansi_when_missing() {
        var middleware = create(null, SCHEMA);

        var result = middleware.analyze("select 1", ExecutionContext.of("ansi", ExecutionMode.ANALYZE));
        assertEquals(DecisionKind.ALLOW, result.kind());
    }

    @Test
    void schema_factory_validates_required_arguments() {
        assertThrows(NullPointerException.class, () -> SqlMiddlewareConfig.forValidation(null));
        assertThrows(NullPointerException.class, () -> SqlMiddlewareConfig.forValidation(null, SCHEMA, null));
        assertThrows(NullPointerException.class,
            () -> SqlMiddlewareConfig.forValidation(SCHEMA).withGuardrails(null));
    }

    @Test
    void explicit_factory_validates_required_arguments() {
        var engine = (SqlDecisionEngine) (query, context) -> DecisionResult.allow();
        var explainer = SqlDecisionExplainer.basic();
        var audit = AuditEventPublisher.noop();
        var guardrails = RuntimeGuardrails.disabled();
        var parser = SqlQueryParser.standard();

        assertThrows(NullPointerException.class, () -> create(null, explainer, audit, guardrails, parser));
        assertThrows(NullPointerException.class, () -> create(engine, null, audit, guardrails, parser));
        assertThrows(NullPointerException.class, () -> create(engine, explainer, null, guardrails, parser));
        assertThrows(NullPointerException.class, () -> create(engine, explainer, audit, null, parser));
        assertThrows(NullPointerException.class, () -> create(engine, explainer, audit, guardrails, null));
    }
}
