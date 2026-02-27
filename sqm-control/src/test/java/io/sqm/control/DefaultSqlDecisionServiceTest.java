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
class DefaultSqlDecisionServiceTest {

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
        return SqlDecisionService.create(SqlDecisionServiceConfig.builder(schema).validationSettings(settings).guardrails(guardrails).buildValidationConfig());
    }

    private static SqlDecisionService create(
        CatalogSchema schema,
        SchemaValidationSettings settings,
        RuntimeGuardrails guardrails,
        AuditEventPublisher auditPublisher,
        SqlDecisionExplainer explainer,
        SqlQueryParser queryParser
    ) {
        return SqlDecisionService.create(
            SqlDecisionServiceConfig.builder(schema)
                .validationSettings(settings)
                .guardrails(guardrails)
                .auditPublisher(auditPublisher)
                .explainer(explainer)
                .queryParser(queryParser)
                .buildValidationConfig()
        );
    }

    @Test
    void default_factory_creates_working_middleware() {
        var decisionService = create(SCHEMA);
        var context = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);

        var result = decisionService.analyze("select 1", context);
        assertEquals(DecisionKind.ALLOW, result.kind());
    }

    @Test
    void factory_applies_validation_settings() {
        var settings = SchemaValidationSettings.builder()
            .limits(SchemaValidationLimits.builder().maxSelectColumns(1).build())
            .build();
        var decisionService = create(SCHEMA, settings);

        var result = decisionService.analyze("select 1, 2", ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));
        assertEquals(DecisionKind.DENY, result.kind());
        assertEquals(ReasonCode.DENY_MAX_SELECT_COLUMNS, result.reasonCode());
    }

    @Test
    void factory_applies_runtime_guardrails() {
        var decisionService = create(
            SCHEMA,
            SchemaValidationSettings.defaults(),
            new RuntimeGuardrails(null, null, 100, false)
        );

        var result = decisionService.enforce("select 1", ExecutionContext.of("postgresql", ExecutionMode.EXECUTE));
        assertEquals(DecisionKind.DENY, result.kind());
        assertEquals(ReasonCode.DENY_MAX_ROWS, result.reasonCode());
    }

    @Test
    void factory_allows_custom_audit_wiring() {
        var audit = InMemoryAuditEventPublisher.create();
        var decisionService = create(
            SCHEMA,
            SchemaValidationSettings.defaults(),
            RuntimeGuardrails.disabled(),
            audit,
            SqlDecisionExplainer.basic(),
            SqlQueryParser.standard()
        );

        decisionService.analyze("select 1", ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));
        assertEquals(1, audit.events().size());
        assertEquals(List.of(), audit.events().getFirst().appliedRules());
    }

    @Test
    void dialect_schema_factory_uses_dialect_aware_validation_wiring() {
        var decisionService = create(SCHEMA);

        var result = decisionService.analyze("select 1", ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));
        assertEquals(DecisionKind.ALLOW, result.kind());
    }

    @Test
    void dialect_schema_factory_validates_required_arguments() {
        assertThrows(NullPointerException.class, () -> create(null));
    }

    @Test
    void dialect_schema_factory_defaults_to_ansi_when_missing() {
        var decisionService = create(SCHEMA);

        var result = decisionService.analyze("select 1", ExecutionContext.of("ansi", ExecutionMode.ANALYZE));
        assertEquals(DecisionKind.ALLOW, result.kind());
    }
}

