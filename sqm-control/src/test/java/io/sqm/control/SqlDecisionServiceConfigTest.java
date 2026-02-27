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

class SqlDecisionServiceConfigTest {
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

        var decisionService = SqlDecisionService.create(
            SqlDecisionServiceConfig.builder(SCHEMA)
                .validationSettings(settings)
                .auditPublisher(audit)
                .guardrails(guardrails)
                .buildValidationConfig()
        );

        var result = decisionService.enforce("select 1, 2", ExecutionContext.of("postgresql", ExecutionMode.EXECUTE));
        assertEquals(DecisionKind.DENY, result.kind());
        assertEquals(ReasonCode.DENY_MAX_SELECT_COLUMNS, result.reasonCode());
        assertEquals(1, audit.events().size());
    }

    @Test
    void for_validation_and_rewrite_builds_rewrite_enabled_middleware() {
        var decisionService = SqlDecisionService.create(
            SqlDecisionServiceConfig.builder(SCHEMA)
                .validationSettings(SchemaValidationSettings.defaults())
                .builtInRewriteSettings(BuiltInRewriteSettings.defaults())
                .rewriteRules(
                    BuiltInRewriteRule.SCHEMA_QUALIFICATION,
                    BuiltInRewriteRule.COLUMN_QUALIFICATION,
                    BuiltInRewriteRule.LIMIT_INJECTION
                ).buildValidationAndRewriteConfig()
        );

        var result = decisionService.analyze("select id from users u", ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));
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
        assertThrows(NullPointerException.class, () -> SqlDecisionServiceConfig.builder(null));

        var config = SqlDecisionServiceConfig.builder(SCHEMA).buildValidationConfig();

        assertNotNull(config.engine());
        assertNotNull(config.explainer());
        assertNotNull(config.auditPublisher());
        assertNotNull(config.guardrails());
        assertNotNull(config.queryParser());
    }

    @Test
    void validation_and_rewrite_defaults_are_safe_when_rewrite_config_is_omitted() {
        var decisionService = SqlDecisionService.create(
            SqlDecisionServiceConfig.builder(SCHEMA)
                .validationSettings(SchemaValidationSettings.defaults())
                .buildValidationAndRewriteConfig()
        );

        var result = decisionService.analyze("select id from users", ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));

        assertEquals(DecisionKind.REWRITE, result.kind());
        assertEquals(ReasonCode.REWRITE_LIMIT, result.reasonCode());
        assertNotNull(result.rewrittenSql());
        assertTrue(result.rewrittenSql().toLowerCase().contains("limit"));
    }

    @Test
    void validation_and_rewrite_with_explicit_empty_rules_uses_built_in_rewrites() {
        var decisionService = SqlDecisionService.create(
            SqlDecisionServiceConfig.builder(SCHEMA)
                .validationSettings(SchemaValidationSettings.defaults())
                .rewriteRules()
                .buildValidationAndRewriteConfig()
        );

        var result = decisionService.analyze("select id from users", ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));

        assertEquals(DecisionKind.REWRITE, result.kind());
        assertEquals(ReasonCode.REWRITE_LIMIT, result.reasonCode());
        assertNotNull(result.rewrittenSql());
        assertTrue(result.rewrittenSql().toLowerCase().contains("limit"));
    }

    @Test
    void builder_loads_validation_settings_from_json_config() {
        var json = """
            {
              "accessPolicy": {
                "deniedTables": ["users"]
              }
            }
            """;

        var decisionService = SqlDecisionService.create(
            SqlDecisionServiceConfig.builder(SCHEMA)
                .validationSettingsJson(json)
                .buildValidationConfig()
        );

        var result = decisionService.analyze("select id from users", ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));
        assertEquals(DecisionKind.DENY, result.kind());
        assertEquals(ReasonCode.DENY_TABLE, result.reasonCode());
    }

    @Test
    void builder_loads_validation_settings_from_yaml_config() {
        var yaml = """
            accessPolicy:
              deniedTables:
                - users
            """;

        var decisionService = SqlDecisionService.create(
            SqlDecisionServiceConfig.builder(SCHEMA)
                .validationSettingsYaml(yaml)
                .buildValidationConfig()
        );

        var result = decisionService.analyze("select id from users", ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));
        assertEquals(DecisionKind.DENY, result.kind());
        assertEquals(ReasonCode.DENY_TABLE, result.reasonCode());
    }

    @Test
    void builder_uses_system_property_settings_when_explicit_settings_are_not_provided() {
        var yaml = """
            accessPolicy:
              deniedTables:
                - users
            """;
        var key = ConfigKeys.VALIDATION_SETTINGS_YAML.property();
        var previous = System.getProperty(key);
        try {
            System.setProperty(key, yaml);
            var decisionService = SqlDecisionService.create(
                SqlDecisionServiceConfig.builder(SCHEMA).buildValidationConfig()
            );

            var result = decisionService.analyze("select id from users", ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));
            assertEquals(DecisionKind.DENY, result.kind());
            assertEquals(ReasonCode.DENY_TABLE, result.reasonCode());
        } finally {
            if (previous == null) {
                System.clearProperty(key);
            } else {
                System.setProperty(key, previous);
            }
        }
    }
}

