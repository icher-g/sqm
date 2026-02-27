package io.sqm.control;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.validate.schema.SchemaValidationSettings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiSqlDecisionServiceFlowTest {

    private static final CatalogSchema SCHEMA = CatalogSchema.of(
        CatalogTable.of(
            "public",
            "users",
            CatalogColumn.of("id", CatalogType.LONG),
            CatalogColumn.of("name", CatalogType.STRING),
            CatalogColumn.of("active", CatalogType.BOOLEAN),
            CatalogColumn.of("tier", CatalogType.STRING)
        ),
        CatalogTable.of(
            "public",
            "orders",
            CatalogColumn.of("id", CatalogType.LONG),
            CatalogColumn.of("user_id", CatalogType.LONG),
            CatalogColumn.of("status", CatalogType.STRING),
            CatalogColumn.of("amount", CatalogType.DECIMAL)
        ),
        CatalogTable.of(
            "public",
            "payments",
            CatalogColumn.of("id", CatalogType.LONG),
            CatalogColumn.of("order_id", CatalogType.LONG),
            CatalogColumn.of("state", CatalogType.STRING)
        )
    );

    private static final String COMPLEX_SQL = """
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

    @Test
    void validation_only_flow_allows_complex_ai_style_sql() {
        var decisionService = SqlDecisionService.create(
            SqlDecisionServiceConfig.builder(SCHEMA)
                .validationSettings(SchemaValidationSettings.defaults())
                .buildValidationConfig()
        );

        var decision = decisionService.analyze(COMPLEX_SQL, ExecutionContext.of("postgresql", ExecutionMode.ANALYZE));

        assertEquals(DecisionKind.ALLOW, decision.kind());
        assertEquals(ReasonCode.NONE, decision.reasonCode());
        assertEquals(0, decision.sqlParams().size());
    }

    @Test
    void full_flow_rewrites_to_bind_sql_for_execute_intent() {
        var decisionService = SqlDecisionService.create(
            SqlDecisionServiceConfig.builder(SCHEMA)
                .validationSettings(SchemaValidationSettings.defaults())
                .builtInRewriteSettings(
                    BuiltInRewriteSettings.builder()
                        .qualificationFailureMode(QualificationFailureMode.SKIP)
                        .build()
                )
                .rewriteRules(BuiltInRewriteRule.LIMIT_INJECTION)
                .buildValidationAndRewriteConfig()
        );

        var context = ExecutionContext.of(
            "postgresql",
            "agent",
            "tenant-a",
            ExecutionMode.EXECUTE,
            ParameterizationMode.BIND
        );

        var decision = decisionService.enforce(COMPLEX_SQL, context);

        assertEquals(DecisionKind.REWRITE, decision.kind());
        assertEquals(ReasonCode.REWRITE_LIMIT, decision.reasonCode());
        assertTrue(decision.rewrittenSql().contains("?"));
        assertTrue(decision.rewrittenSql().toLowerCase().contains("limit"));
        assertTrue(decision.sqlParams().size() >= 6);
    }

    @Test
    void execute_intent_denies_ddl_sql() {
        var decisionService = SqlDecisionService.create(
            SqlDecisionServiceConfig.builder(SCHEMA)
                .validationSettings(SchemaValidationSettings.defaults())
                .buildValidationConfig()
        );

        var decision = decisionService.enforce("drop table users", ExecutionContext.of("postgresql", ExecutionMode.EXECUTE));

        assertEquals(DecisionKind.DENY, decision.kind());
        assertTrue(
            decision.reasonCode() == ReasonCode.DENY_DDL
                || decision.reasonCode() == ReasonCode.DENY_VALIDATION
                || decision.reasonCode() == ReasonCode.DENY_PIPELINE_ERROR
        );
    }
}

