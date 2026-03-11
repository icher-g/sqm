package io.sqm.control;

import io.sqm.control.audit.*;
import io.sqm.control.config.*;
import io.sqm.control.decision.*;
import io.sqm.control.execution.*;
import io.sqm.control.pipeline.*;
import io.sqm.control.rewrite.*;
import io.sqm.control.service.*;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.control.audit.InMemoryAuditEventPublisher;
import io.sqm.validate.schema.SchemaValidationLimits;
import io.sqm.validate.schema.SchemaValidationSettings;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        SqlStatementParser statementParser
    ) {
        return SqlDecisionService.create(
            SqlDecisionServiceConfig.builder(schema)
                .validationSettings(settings)
                .guardrails(guardrails)
                .auditPublisher(auditPublisher)
                .explainer(explainer)
                .statementParser(statementParser)
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
            SqlStatementParser.standard()
        );

        decisionService.analyze("select 1", ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));
        assertEquals(1, audit.events().size());
        assertEquals(List.of(), audit.events().getFirst().appliedRules());
    }

    @Test
    void audit_event_preserves_tenant_from_execution_context() {
        var audit = InMemoryAuditEventPublisher.create();
        var decisionService = create(
            SCHEMA,
            SchemaValidationSettings.defaults(),
            RuntimeGuardrails.disabled(),
            audit,
            SqlDecisionExplainer.basic(),
            SqlStatementParser.standard()
        );

        decisionService.analyze(
            "select 1",
            ExecutionContext.of("postgresql", "alice", "tenant_a", ExecutionMode.ANALYZE)
        );

        assertEquals(1, audit.events().size());
        assertEquals("tenant_a", audit.events().getFirst().context().tenant());
        assertEquals("alice", audit.events().getFirst().context().principal());
    }

    @Test
    void explain_flow_passes_tenant_to_explainer_context() {
        var seenTenant = new AtomicReference<String>();
        var explainer = (SqlDecisionExplainer) (query, context, decision) -> {
            seenTenant.set(context.tenant());
            return "ok";
        };
        var decisionService = create(
            SCHEMA,
            SchemaValidationSettings.defaults(),
            RuntimeGuardrails.disabled(),
            AuditEventPublisher.noop(),
            explainer,
            SqlStatementParser.standard()
        );

        var explanation = decisionService.explainDecision(
            "select 1",
            ExecutionContext.of("postgresql", "alice", "tenant_a", ExecutionMode.EXECUTE)
        );

        assertEquals("tenant_a", seenTenant.get());
        assertEquals("ok", explanation.explanation());
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

    @Test
    void default_factory_supports_mysql_select_flow() {
        var decisionService = create(SCHEMA);

        var result = decisionService.analyze("select 1", ExecutionContext.of("mysql", ExecutionMode.ANALYZE));

        assertEquals(DecisionKind.ALLOW, result.kind());
    }

    @Test
    void default_factory_supports_dml_flow_without_query_guardrails() {
        var decisionService = create(SCHEMA);

        var result = decisionService.analyze(
            "update users set name = 'alice' where id = 1",
            ExecutionContext.of("mysql", ExecutionMode.ANALYZE)
        );

        assertEquals(DecisionKind.ALLOW, result.kind());
    }

    @Test
    void explainDecision_falls_back_when_explainer_returns_blank() {
        var decisionService = create(
            SCHEMA,
            SchemaValidationSettings.defaults(),
            RuntimeGuardrails.disabled(),
            AuditEventPublisher.noop(),
            (statement, context, decision) -> "   ",
            SqlStatementParser.standard()
        );

        var explanation = decisionService.explainDecision("select 1", ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));

        assertTrue(explanation.explanation().contains("Decision=ALLOW"));
    }

    @Test
    void enforce_rewrites_to_explain_in_dry_run_mode() {
        var decisionService = create(
            SCHEMA,
            SchemaValidationSettings.defaults(),
            new RuntimeGuardrails(null, null, null, true)
        );

        var result = decisionService.enforce("select 1 limit 1", ExecutionContext.of("postgresql", ExecutionMode.EXECUTE));

        assertEquals(DecisionKind.REWRITE, result.kind());
        assertEquals(ReasonCode.REWRITE_EXPLAIN_DRY_RUN, result.reasonCode());
        assertTrue(result.rewrittenSql().startsWith("EXPLAIN "));
    }

    @Test
    void analyze_rejects_blank_sql() {
        var decisionService = create(SCHEMA);

        assertThrows(IllegalArgumentException.class, () -> decisionService.analyze("   ", ExecutionContext.of("postgresql", ExecutionMode.ANALYZE)));
    }
}



