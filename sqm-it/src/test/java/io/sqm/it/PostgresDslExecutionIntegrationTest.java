package io.sqm.it;

import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class PostgresDslExecutionIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private final RenderContext renderContext = RenderContext.of(new PostgresDialect());

    @BeforeEach
    void setUpSchema() throws Exception {
        try (var connection = DriverManager.getConnection(
            POSTGRES.getJdbcUrl(),
            POSTGRES.getUsername(),
            POSTGRES.getPassword()
        )) {
            try (var statement = connection.createStatement()) {
                statement.execute("drop table if exists orders");
                statement.execute("drop table if exists users");
                statement.execute("drop table if exists events");

                statement.execute("create table users (" +
                    "id bigint primary key," +
                    "name text not null," +
                    "active boolean not null)");
                statement.execute("create table orders (" +
                    "id bigint primary key," +
                    "user_id bigint not null references users(id)," +
                    "status text not null," +
                    "amount numeric(10,2) not null)");
                statement.execute("create table events (" +
                    "user_id bigint not null," +
                    "version int not null," +
                    "payload text," +
                    "primary key (user_id, version))");

                statement.execute("insert into users(id, name, active) values " +
                    "(1, 'Alice', true), (2, 'Bob', false), (3, 'Carol', true)");
                statement.execute("insert into orders(id, user_id, status, amount) values " +
                    "(10, 1, 'PAID', 12.50), (11, 1, 'NEW', 7.00), (12, 3, 'PAID', 5.25)");
                statement.execute("insert into events(user_id, version, payload) values " +
                    "(1, 1, 'a'), (1, 2, 'b'), (2, 1, 'c'), (2, 3, 'd')");
            }
        }
    }

    @Test
    void dslQuery_rendersAndExecutesJoinAggregateAgainstPostgres() throws Exception {
        var query = select(
            col("u", "name"),
            func("count", starArg()).as("cnt"),
            func("sum", arg(col("o", "amount"))).as("total")
        )
            .from(tbl("users").as("u"))
            .join(inner(tbl("orders").as("o")).on(col("u", "id").eq(col("o", "user_id"))))
            .where(col("u", "active").eq(true).and(col("o", "status").eq("PAID")))
            .groupBy(group("u", "name"))
            .orderBy(order(col("u", "name")).asc())
            .build();

        var sql = renderContext.render(query).sql();
        assertTrue(sql.contains("JOIN"));
        assertTrue(sql.contains("GROUP BY"));

        List<String> rows = new ArrayList<>();
        try (var connection = DriverManager.getConnection(
            POSTGRES.getJdbcUrl(),
            POSTGRES.getUsername(),
            POSTGRES.getPassword()
        )) {
            try (var statement = connection.createStatement();
                 var resultSet = statement.executeQuery(sql)) {
                while (resultSet.next()) {
                    rows.add(resultSet.getString(1) + "|" + resultSet.getLong(2) + "|" + resultSet.getBigDecimal(3));
                }
            }
        }

        assertEquals(List.of("Alice|1|12.50", "Carol|1|5.25"), rows);
    }

    @Test
    void dslDistinctOn_rendersAndExecutesAgainstPostgres() throws Exception {
        var query = select(col("e", "user_id"), col("e", "version"))
            .from(tbl("events").as("e"))
            .distinct(distinctOn(col("e", "user_id")))
            .orderBy(
                order(col("e", "user_id")).asc(),
                order(col("e", "version")).desc()
            )
            .build();

        var sql = renderContext.render(query).sql();
        assertTrue(sql.contains("DISTINCT ON"));

        List<String> rows = new ArrayList<>();
        try (var connection = DriverManager.getConnection(
            POSTGRES.getJdbcUrl(),
            POSTGRES.getUsername(),
            POSTGRES.getPassword()
        )) {
            try (var statement = connection.createStatement();
                 var resultSet = statement.executeQuery(sql)) {
                while (resultSet.next()) {
                    rows.add(resultSet.getLong(1) + "|" + resultSet.getInt(2));
                }
            }
        }

        assertEquals(List.of("1|2", "2|3"), rows);
    }
}
