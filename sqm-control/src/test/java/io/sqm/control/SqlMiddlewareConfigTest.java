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
            SqlMiddlewareConfig.builder(SCHEMA)
                .validationSettings(settings)
                .auditPublisher(audit)
                .guardrails(guardrails)
                .buildValidationConfig()
        );

        var result = middleware.enforce("select 1, 2", ExecutionContext.of("postgresql", ExecutionMode.EXECUTE));
        assertEquals(DecisionKind.DENY, result.kind());
        assertEquals(ReasonCode.DENY_MAX_SELECT_COLUMNS, result.reasonCode());
        assertEquals(1, audit.events().size());
    }

    @Test
    void for_validation_and_rewrite_builds_rewrite_enabled_middleware() {
        var middleware = SqlMiddleware.create(
            SqlMiddlewareConfig.builder(SCHEMA)
                .validationSettings(SchemaValidationSettings.defaults())
                .builtInRewriteSettings(BuiltInRewriteSettings.defaults())
                .rewriteRules(
                    BuiltInRewriteRule.SCHEMA_QUALIFICATION,
                    BuiltInRewriteRule.COLUMN_QUALIFICATION,
                    BuiltInRewriteRule.LIMIT_INJECTION
                ).buildValidationAndRewriteConfig()
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
    void builder_defaults_and_required_schema_are_enforced() {
        assertThrows(NullPointerException.class, () -> SqlMiddlewareConfig.builder(null));

        var config = SqlMiddlewareConfig.builder(SCHEMA).buildValidationConfig();

        assertNotNull(config.engine());
        assertNotNull(config.explainer());
        assertNotNull(config.auditPublisher());
        assertNotNull(config.guardrails());
        assertNotNull(config.queryParser());
    }
}
