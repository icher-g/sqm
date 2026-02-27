package io.sqm.it;

import io.sqm.catalog.model.CatalogColumn;
import io.sqm.catalog.model.CatalogSchema;
import io.sqm.catalog.model.CatalogTable;
import io.sqm.catalog.model.CatalogType;
import io.sqm.control.*;
import io.sqm.validate.schema.SchemaValidationSettings;
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
                "tier text not null)");

            statement.execute("create table orders (" +
                "id bigint primary key," +
                "user_id bigint not null references users(id)," +
                "status text not null," +
                "amount numeric(10,2) not null)");

            statement.execute("create table payments (" +
                "id bigint primary key," +
                "order_id bigint not null references orders(id)," +
                "state text not null)");

            statement.execute("insert into users(id, name, active, tier) values " +
                "(1, 'Alice', true, 'gold')," +
                "(2, 'Bob', true, 'silver')," +
                "(3, 'Carol', false, 'gold')");

            statement.execute("insert into orders(id, user_id, status, amount) values " +
                "(100, 1, 'PAID', 120.00)," +
                "(101, 1, 'PAID', 35.50)," +
                "(102, 2, 'PAID', 60.00)," +
                "(103, 2, 'NEW', 15.00)," +
                "(104, 3, 'PAID', 210.00)");

            statement.execute("insert into payments(id, order_id, state) values " +
                "(1000, 100, 'SETTLED')," +
                "(1001, 101, 'SETTLED')," +
                "(1002, 102, 'SETTLED')," +
                "(1003, 103, 'PENDING')," +
                "(1004, 104, 'SETTLED')");
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

