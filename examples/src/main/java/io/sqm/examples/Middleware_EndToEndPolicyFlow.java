package io.sqm.examples;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.control.AuditEventPublisher;
import io.sqm.control.BuiltInRewriteRule;
import io.sqm.control.BuiltInRewriteSettings;
import io.sqm.control.DecisionResult;
import io.sqm.control.ExecutionContext;
import io.sqm.control.ExecutionMode;
import io.sqm.control.ParameterizationMode;
import io.sqm.control.QueryRewriteResult;
import io.sqm.control.QueryRewriteRule;
import io.sqm.control.RuntimeGuardrails;
import io.sqm.control.SqlDecisionExplainer;
import io.sqm.control.SqlDecisionService;
import io.sqm.control.SqlDecisionServiceConfig;
import io.sqm.control.TenantRewriteTablePolicy;
import io.sqm.validate.schema.SchemaValidationSettings;
import io.sqm.validate.schema.SchemaValidationSettingsLoader;

import java.util.List;

/**
 * Demonstrates end-to-end middleware usage with validation-only and full rewrite flows.
 */
public final class Middleware_EndToEndPolicyFlow {

    private Middleware_EndToEndPolicyFlow() {
    }

    /**
     * Runs middleware examples for validation-only, full rewrite flow, and extension points.
     *
     * @param args application arguments
     */
    public static void main(String[] args) {
        var schema = CatalogSchema.of(
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

        String aiSql = """
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

        runValidationOnlyFlow(schema, aiSql);
        runFullFlowWithRewrite(schema, aiSql);
        runFlowWithTenantPoliciesFromConfig(schema);
        runFlowWithCustomExtensions(schema, aiSql);
    }

    private static void runValidationOnlyFlow(CatalogSchema schema, String sql) {
        SqlDecisionService decisionService = SqlDecisionService.create(
            SqlDecisionServiceConfig.builder(schema)
                .validationSettings(SchemaValidationSettings.defaults())
                .buildValidationConfig()
        );

        DecisionResult decision = decisionService.analyze(sql, ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));

        print("Validation-only analyze", decision);
    }

    private static void runFullFlowWithRewrite(CatalogSchema schema, String sql) {
        SqlDecisionService decisionService = SqlDecisionService.create(
            SqlDecisionServiceConfig.builder(schema)
                .validationSettings(SchemaValidationSettings.defaults())
                .builtInRewriteSettings(
                    BuiltInRewriteSettings.builder()
                        .defaultLimitInjectionValue(1000)
                        .tenantTablePolicy("public.users", TenantRewriteTablePolicy.required("tenant_id"))
                        .tenantTablePolicy("public.orders", TenantRewriteTablePolicy.required("tenant_id"))
                        .build()
                )
                .rewriteRules(
                    BuiltInRewriteRule.LIMIT_INJECTION,
                    BuiltInRewriteRule.TENANT_PREDICATE,
                    BuiltInRewriteRule.CANONICALIZATION
                )
                .guardrails(new RuntimeGuardrails(10_000, 1_000L, null, false))
                .buildValidationAndRewriteConfig()
        );

        DecisionResult decision = decisionService.enforce(
            sql,
            ExecutionContext.of(
                "postgresql",
                "ai-agent",
                "tenant-a",
                ExecutionMode.EXECUTE,
                ParameterizationMode.BIND
            )
        );

        print("Full flow enforce (rewrite + bind)", decision);
    }

    private static void runFlowWithCustomExtensions(CatalogSchema schema, String sql) {
        QueryRewriteRule addTenantGuard = (query, context) -> {
            if (context.tenant() == null || context.tenant().isBlank()) {
                return QueryRewriteResult.unchanged(query);
            }
            return QueryRewriteResult.rewritten(query, "tenant-guard", io.sqm.control.ReasonCode.REWRITE_CANONICALIZATION);
        };

        SqlDecisionService decisionService = SqlDecisionService.create(
            SqlDecisionServiceConfig.builder(schema)
                .validationSettings(SchemaValidationSettings.defaults())
                .queryRewriter(io.sqm.control.SqlQueryRewriter.chain(addTenantGuard))
                .queryRenderer(io.sqm.control.SqlQueryRenderer.standard())
                .auditPublisher(AuditEventPublisher.noop())
                .explainer(SqlDecisionExplainer.basic())
                .buildValidationAndRewriteConfig()
        );

        DecisionResult decision = decisionService.analyze(
            sql,
            ExecutionContext.of("postgresql", "custom-user", "tenant-x", ExecutionMode.ANALYZE, ParameterizationMode.OFF)
        );

        print("Custom extension points", decision);
    }

    private static void runFlowWithTenantPoliciesFromConfig(CatalogSchema schema) {
        var yaml = """
            tenantRequirementMode: REQUIRED
            accessPolicy:
              tenants:
                - name: tenant-a
                  deniedTables:
                    - payments
                - name: tenant-b
                  deniedTables:
                    - users
            """;

        var settings = SchemaValidationSettingsLoader.fromYaml(yaml);
        SqlDecisionService decisionService = SqlDecisionService.create(
            SqlDecisionServiceConfig.builder(schema)
                .validationSettings(settings)
                .buildValidationConfig()
        );

        var sql = "select id from payments";

        var deniedForTenantA = decisionService.analyze(
            sql,
            ExecutionContext.of("postgresql", "agent", "tenant-a", ExecutionMode.ANALYZE, ParameterizationMode.OFF)
        );
        print("Tenant policy from config (tenant-a denied)", deniedForTenantA);

        var allowedForTenantB = decisionService.analyze(
            sql,
            ExecutionContext.of("postgresql", "agent", "tenant-b", ExecutionMode.ANALYZE, ParameterizationMode.OFF)
        );
        print("Tenant policy from config (tenant-b allowed)", allowedForTenantB);
    }

    private static void print(String label, DecisionResult decision) {
        System.out.println("=== " + label + " ===");
        System.out.println("kind        : " + decision.kind());
        System.out.println("reasonCode  : " + decision.reasonCode());
        System.out.println("message     : " + decision.message());
        System.out.println("fingerprint : " + decision.fingerprint());
        System.out.println("rewrittenSql: " + decision.rewrittenSql());
        System.out.println("sqlParams   : " + formatParams(decision.sqlParams()));
        System.out.println();
    }

    private static String formatParams(List<Object> params) {
        return params == null ? "[]" : params.toString();
    }
}

