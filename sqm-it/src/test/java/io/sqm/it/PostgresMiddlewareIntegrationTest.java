package io.sqm.it;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.control.*;
import io.sqm.validate.schema.SchemaValidationSettings;
import io.sqm.validate.schema.SchemaValidationSettingsLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class PostgresMiddlewareIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

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
        try (var connection = openConnection(); var statement = connection.createStatement()) {
            statement.execute("drop table if exists payments");
            statement.execute("drop table if exists orders");
            statement.execute("drop table if exists users");

            statement.execute("create table users (" +
                "id bigint primary key," +
                "name text not null," +
                "active boolean not null," +
                "tier text not null," +
                "tenant_id text not null)");

            statement.execute("create table orders (" +
                "id bigint primary key," +
                "user_id bigint not null references users(id)," +
                "status text not null," +
                "amount numeric(10,2) not null," +
                "tenant_id text not null)");

            statement.execute("create table payments (" +
                "id bigint primary key," +
                "order_id bigint not null references orders(id)," +
                "state text not null," +
                "tenant_id text not null)");

            statement.execute("insert into users(id, name, active, tier, tenant_id) values " +
                "(1, 'Alice', true, 'gold', 'tenant-a')," +
                "(2, 'Bob', true, 'silver', 'tenant-a')," +
                "(3, 'Carol', false, 'gold', 'tenant-b')");

            statement.execute("insert into orders(id, user_id, status, amount, tenant_id) values " +
                "(100, 1, 'PAID', 120.00, 'tenant-a')," +
                "(101, 1, 'PAID', 35.50, 'tenant-a')," +
                "(102, 2, 'PAID', 60.00, 'tenant-a')," +
                "(103, 2, 'NEW', 15.00, 'tenant-a')," +
                "(104, 3, 'PAID', 210.00, 'tenant-b')");

            statement.execute("insert into payments(id, order_id, state, tenant_id) values " +
                "(1000, 100, 'SETTLED', 'tenant-a')," +
                "(1001, 101, 'SETTLED', 'tenant-a')," +
                "(1002, 102, 'SETTLED', 'tenant-a')," +
                "(1003, 103, 'PENDING', 'tenant-a')," +
                "(1004, 104, 'SETTLED', 'tenant-b')");
        }
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

        List<String> rows = executeQuery(sql, List.of());
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

        List<String> rows = executeQuery(decision.rewrittenSql(), decision.sqlParams());
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

        List<String> rows = executeQuery("select count(*) from users", List.of());
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

        List<String> rows = executeQuery("select id from payments order by id", List.of());
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

        assertEquals(DecisionKind.REWRITE, tenantADecision.kind());
        assertEquals(ReasonCode.REWRITE_TENANT_PREDICATE, tenantADecision.reasonCode());
        assertTrue(tenantADecision.rewrittenSql().contains("?"));
        assertEquals(List.of("tenant-a"), tenantADecision.sqlParams());
        assertEquals(List.of("1|Alice", "2|Bob"), executeQuery(tenantADecision.rewrittenSql(), tenantADecision.sqlParams()));

        var tenantBDecision = decisionService.enforce(
            sql,
            ExecutionContext.of("postgresql", "agent", "tenant-b", ExecutionMode.EXECUTE, ParameterizationMode.BIND)
        );
        assertEquals(List.of("tenant-b"), tenantBDecision.sqlParams());
        assertEquals(List.of("3|Carol"), executeQuery(tenantBDecision.rewrittenSql(), tenantBDecision.sqlParams()));
    }

    private Connection openConnection() throws Exception {
        return DriverManager.getConnection(
            POSTGRES.getJdbcUrl(),
            POSTGRES.getUsername(),
            POSTGRES.getPassword()
        );
    }

    private List<String> executeQuery(String sql, List<Object> params) throws Exception {
        List<String> rows = new ArrayList<>();
        try (var connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, params);
            try (ResultSet resultSet = statement.executeQuery()) {
                int columns = resultSet.getMetaData().getColumnCount();
                while (resultSet.next()) {
                    List<String> values = new ArrayList<>();
                    for (int index = 1; index <= columns; index++) {
                        Object value = resultSet.getObject(index);
                        values.add(stringify(value));
                    }
                    rows.add(String.join("|", values));
                }
            }
        }
        return rows;
    }

    private void bind(PreparedStatement statement, List<Object> params) throws Exception {
        for (int index = 0; index < params.size(); index++) {
            statement.setObject(index + 1, params.get(index));
        }
    }

    private String stringify(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal.setScale(2, RoundingMode.CEILING).toPlainString();
        }
        return String.valueOf(value);
    }
}

