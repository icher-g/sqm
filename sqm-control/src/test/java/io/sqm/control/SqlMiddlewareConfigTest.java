package io.sqm.control;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.control.audit.InMemoryAuditEventPublisher;
import io.sqm.validate.schema.SchemaValidationLimits;
import io.sqm.validate.schema.SchemaValidationSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlMiddlewareConfigTest {
    private static final CatalogSchema SCHEMA = CatalogSchema.of(
        CatalogTable.of("public", "users",
            CatalogColumn.of("id", CatalogType.LONG),
            CatalogColumn.of("name", CatalogType.STRING)
        )
    );

    @Test
    void for_validation_builds_working_middleware_and_supports_customization() {
        var audit = InMemoryAuditEventPublisher.create();
        var guardrails = new RuntimeGuardrails(null, null, 100, false);
        var settings = SchemaValidationSettings.builder()
            .limits(SchemaValidationLimits.builder().maxSelectColumns(1).build())
            .build();

        var middleware = SqlMiddleware.create(
            SqlMiddlewareConfig.forValidation("postgresql", SCHEMA, settings)
                .withAuditPublisher(audit)
                .withGuardrails(guardrails)
        );

        var result = middleware.enforce("select 1, 2", ExecutionContext.of("postgresql", ExecutionMode.EXECUTE));
        assertEquals(DecisionKind.DENY, result.kind());
        assertEquals(ReasonCode.DENY_MAX_SELECT_COLUMNS, result.reasonCode());
        assertEquals(1, audit.events().size());
    }

    @Test
    void for_validation_and_rewrite_builds_rewrite_enabled_middleware() {
        var middleware = SqlMiddleware.create(
            SqlMiddlewareConfig.forValidationAndRewrite(
                "postgresql",
                SCHEMA,
                SchemaValidationSettings.defaults(),
                BuiltInRewriteSettings.defaults(),
                BuiltInRewriteRule.SCHEMA_QUALIFICATION,
                BuiltInRewriteRule.COLUMN_QUALIFICATION,
                BuiltInRewriteRule.LIMIT_INJECTION
            )
        );

        var result = middleware.analyze("select id from users u", ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));
        assertEquals(DecisionKind.REWRITE, result.kind());
        assertEquals(ReasonCode.REWRITE_LIMIT, result.reasonCode());
        assertNotNull(result.rewrittenSql());
        String rendered = result.rewrittenSql().toLowerCase();
        assertTrue(rendered.contains("public.users"));
        assertTrue(rendered.contains("u.id"));
        assertTrue(rendered.contains("limit"));
    }

    @Test
    void for_engine_and_with_methods_validate_nulls() {
        var engine = (SqlDecisionEngine) (query, context) -> DecisionResult.allow();
        var context = SqlMiddlewareConfig.forEngine(engine);

        assertThrows(NullPointerException.class, () -> SqlMiddleware.create(null));
        assertThrows(NullPointerException.class, () -> SqlMiddlewareConfig.forEngine(null));
        assertThrows(NullPointerException.class, () -> context.withAuditPublisher(null));
        assertThrows(NullPointerException.class, () -> context.withExplainer(null));
        assertThrows(NullPointerException.class, () -> context.withGuardrails(null));
        assertThrows(NullPointerException.class, () -> context.withQueryParser(null));
    }
}
