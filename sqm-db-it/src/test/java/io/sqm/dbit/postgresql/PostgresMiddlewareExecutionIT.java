package io.sqm.dbit.postgresql;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.control.config.SqlDecisionServiceConfig;
import io.sqm.control.decision.DecisionKind;
import io.sqm.control.decision.ReasonCode;
import io.sqm.control.execution.ExecutionContext;
import io.sqm.control.execution.ExecutionMode;
import io.sqm.control.execution.ParameterizationMode;
import io.sqm.control.rewrite.BuiltInRewriteRule;
import io.sqm.control.rewrite.BuiltInRewriteSettings;
import io.sqm.control.rewrite.TenantRewriteTablePolicy;
import io.sqm.control.service.SqlDecisionService;
import io.sqm.validate.schema.SchemaValidationSettings;
import io.sqm.validate.schema.SchemaValidationSettingsLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers(disabledWithoutDocker = true)
class PostgresMiddlewareExecutionIT extends PostgresExecutionHarness {
    private static final CatalogSchema SCHEMA = CatalogSchema.of(
        CatalogTable.of(
            "public",
            "users",
            CatalogColumn.of("id", CatalogType.LONG),
            CatalogColumn.of("name", CatalogType.STRING),
            CatalogColumn.of("active", CatalogType.BOOLEAN),
            CatalogColumn.of("tier", CatalogType.STRING),
            CatalogColumn.of("tenant_id", CatalogType.STRING)
        ),
        CatalogTable.of(
            "public",
            "orders",
            CatalogColumn.of("id", CatalogType.LONG),
            CatalogColumn.of("user_id", CatalogType.LONG),
            CatalogColumn.of("status", CatalogType.STRING),
            CatalogColumn.of("amount", CatalogType.DECIMAL),
            CatalogColumn.of("tenant_id", CatalogType.STRING)
        ),
        CatalogTable.of(
            "public",
            "payments",
            CatalogColumn.of("id", CatalogType.LONG),
            CatalogColumn.of("order_id", CatalogType.LONG),
            CatalogColumn.of("state", CatalogType.STRING),
            CatalogColumn.of("tenant_id", CatalogType.STRING)
        )
    );

    @BeforeEach
    void setUpSchema() throws Exception {
        resetMiddlewareSchema();
    }

    @Test
    void validation_only_flow_allows_complex_sql_then_query_executes_on_postgres() throws Exception {
        var decisionService = SqlDecisionService.create(
            SqlDecisionServiceConfig.builder(SCHEMA)
                .validationSettings(SchemaValidationSettings.defaults())
                .buildValidationConfig()
        );

        String sql = """
            with settled_paid as (
                select o.user_id, o.amount
                from orders o
                join payments p on p.order_id = o.id
                where o.status = 'PAID' and p.state = 'SETTLED'
            )
            select u.id, u.name, sum(sp.amount) as total_amount
            from users u
            join settled_paid sp on sp.user_id = u.id
            where u.active = true and u.tier in ('gold', 'silver')
            group by u.id, u.name
            having sum(sp.amount) >= 50
            order by total_amount desc, u.id asc
            """;

        var decision = decisionService.analyze(sql, ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));

        assertEquals(DecisionKind.ALLOW, decision.kind());
        assertEquals(ReasonCode.NONE, decision.reasonCode());

        List<String> rows = queryRows(sql, List.of());
        assertEquals(List.of("1|Alice|155.50", "2|Bob|60.00"), rows);
    }

    @Test
    void full_flow_rewrites_with_bind_params_and_rewritten_query_executes_on_postgres() throws Exception {
        var decisionService = SqlDecisionService.create(
            SqlDecisionServiceConfig.builder(SCHEMA)
                .validationSettings(SchemaValidationSettings.defaults())
                .builtInRewriteSettings(BuiltInRewriteSettings.defaults())
                .rewriteRules(BuiltInRewriteRule.LIMIT_INJECTION)
                .buildValidationAndRewriteConfig()
        );

        String sql = """
            with settled_paid as (
                select o.user_id, o.amount
                from orders o
                join payments p on p.order_id = o.id
                where o.status = 'PAID' and p.state = 'SETTLED'
            )
            select u.id, u.name, sum(sp.amount) as total_amount
            from users u
            join settled_paid sp on sp.user_id = u.id
            where u.active = true and u.tier in ('gold', 'silver')
            group by u.id, u.name
            having sum(sp.amount) >= 50
            order by total_amount desc, u.id asc
            """;

        var context = ExecutionContext.of(
            "postgresql",
            "agent",
            "tenant-a",
            ExecutionMode.EXECUTE,
            ParameterizationMode.BIND
        );

        var decision = decisionService.enforce(sql, context);

        assertEquals(DecisionKind.REWRITE, decision.kind());
        assertEquals(ReasonCode.REWRITE_LIMIT, decision.reasonCode());
        assertTrue(decision.rewrittenSql().contains("?"));
        assertTrue(decision.rewrittenSql().toLowerCase().contains("limit"));
        assertTrue(decision.sqlParams().size() >= 6);

        List<String> rows = queryRows(decision.rewrittenSql(), decision.sqlParams());
        assertEquals(List.of("1|Alice|155.50", "2|Bob|60.00"), rows);
    }

    @Test
    void execute_intent_flow_denies_ddl_and_prevents_execution() throws Exception {
        var decisionService = SqlDecisionService.create(
            SqlDecisionServiceConfig.builder(SCHEMA)
                .validationSettings(SchemaValidationSettings.defaults())
                .buildValidationConfig()
        );

        String ddl = "drop table users";

        var decision = decisionService.enforce(ddl, ExecutionContext.of("postgresql", ExecutionMode.EXECUTE));

        assertEquals(DecisionKind.DENY, decision.kind());
        assertTrue(
            decision.reasonCode() == ReasonCode.DENY_DDL ||
                decision.reasonCode() == ReasonCode.DENY_VALIDATION ||
                decision.reasonCode() == ReasonCode.DENY_PIPELINE_ERROR
        );

        List<String> rows = queryRows("select count(*) from users", List.of());
        assertEquals(List.of("3"), rows);
    }

    @Test
    void tenant_specific_access_policy_denies_and_allows_by_tenant() throws Exception {
        var yaml = """
            accessPolicy:
              tenants:
                - name: tenant-a
                  deniedTables:
                    - payments
            """;

        var settings = SchemaValidationSettingsLoader.fromYaml(yaml);
        var decisionService = SqlDecisionService.create(
            SqlDecisionServiceConfig.builder(SCHEMA)
                .validationSettings(settings)
                .buildValidationConfig()
        );

        var deniedForTenantA = decisionService.analyze(
            "select id from payments",
            ExecutionContext.of("postgresql", "agent", "tenant-a", ExecutionMode.ANALYZE, ParameterizationMode.OFF)
        );
        assertEquals(DecisionKind.DENY, deniedForTenantA.kind());
        assertEquals(ReasonCode.DENY_TABLE, deniedForTenantA.reasonCode());

        var allowedForTenantB = decisionService.analyze(
            "select id from payments",
            ExecutionContext.of("postgresql", "agent", "tenant-b", ExecutionMode.ANALYZE, ParameterizationMode.OFF)
        );
        assertNotEquals(DecisionKind.DENY, allowedForTenantB.kind());

        List<String> rows = queryRows("select id from payments order by id", List.of());
        assertEquals(List.of("1000", "1001", "1002", "1003", "1004"), rows);
    }

    @Test
    void tenant_predicate_rewrite_bind_mode_enforces_tenant_isolation_on_execution() throws Exception {
        var decisionService = SqlDecisionService.create(
            SqlDecisionServiceConfig.builder(SCHEMA)
                .validationSettings(SchemaValidationSettings.defaults())
                .builtInRewriteSettings(
                    BuiltInRewriteSettings.builder()
                        .tenantTablePolicy("public.users", TenantRewriteTablePolicy.required("tenant_id"))
                        .build()
                )
                .rewriteRules(BuiltInRewriteRule.TENANT_PREDICATE)
                .buildValidationAndRewriteConfig()
        );

        String sql = "select u.id, u.name from users u order by u.id";
        var tenantAContext = ExecutionContext.of(
            "postgresql",
            "agent",
            "tenant-a",
            ExecutionMode.EXECUTE,
            ParameterizationMode.BIND
        );

        var tenantADecision = decisionService.enforce(sql, tenantAContext);

        assertNotEquals(
            DecisionKind.DENY,
            tenantADecision.kind(),
            () -> "tenant-a denied: reason=" + tenantADecision.reasonCode() + " message=" + tenantADecision.message()
        );
        assertNotNull(tenantADecision.rewrittenSql());
        assertTrue(tenantADecision.rewrittenSql().toLowerCase().contains("tenant_id"));
        assertTrue(
            tenantADecision.sqlParams().stream().anyMatch("tenant-a"::equals),
            "expected tenant-a bind param, got: " + tenantADecision.sqlParams()
        );
        assertEquals(List.of("1|Alice", "2|Bob"), queryRows(tenantADecision.rewrittenSql(), tenantADecision.sqlParams()));

        var tenantBDecision = decisionService.enforce(
            sql,
            ExecutionContext.of("postgresql", "agent", "tenant-b", ExecutionMode.EXECUTE, ParameterizationMode.BIND)
        );
        assertNotEquals(
            DecisionKind.DENY,
            tenantBDecision.kind(),
            () -> "tenant-b denied: reason=" + tenantBDecision.reasonCode() + " message=" + tenantBDecision.message()
        );
        assertNotNull(tenantBDecision.rewrittenSql());
        assertTrue(tenantBDecision.rewrittenSql().toLowerCase().contains("tenant_id"));
        assertTrue(
            tenantBDecision.sqlParams().stream().anyMatch("tenant-b"::equals),
            "expected tenant-b bind param, got: " + tenantBDecision.sqlParams()
        );
        assertEquals(List.of("3|Carol"), queryRows(tenantBDecision.rewrittenSql(), tenantBDecision.sqlParams()));
    }

}
