package io.sqm.dbit.postgresql;

import io.sqm.core.Statement;
import io.sqm.dbit.support.DialectExecutionHarness;
import io.sqm.render.postgresql.spi.PostgresDialect;
import io.sqm.render.spi.RenderContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.sql.Connection;
import java.sql.DriverManager;

abstract class PostgresExecutionHarness extends DialectExecutionHarness {
    @Container
    protected static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private final RenderContext renderContext = RenderContext.of(new PostgresDialect());

    @Override
    protected Connection openConnection() throws Exception {
        return DriverManager.getConnection(
            POSTGRES.getJdbcUrl(),
            POSTGRES.getUsername(),
            POSTGRES.getPassword()
        );
    }

    @Override
    protected String render(Statement statement) {
        return renderContext.render(statement).sql();
    }

    protected void resetDslSchema() throws Exception {
        executeStatements(
            "drop table if exists source_users",
            "drop table if exists orders",
            "drop table if exists users",
            "drop table if exists events",
            "create table users (" +
                "id bigint primary key," +
                "name text not null," +
                "active boolean not null)",
            "create table source_users (" +
                "id bigint primary key," +
                "name text not null," +
                "active boolean not null)",
            "create table orders (" +
                "id bigint primary key," +
                "user_id bigint not null references users(id)," +
                "status text not null," +
                "amount numeric(10,2) not null)",
            "create table events (" +
                "user_id bigint not null," +
                "version int not null," +
                "payload text," +
                "primary key (user_id, version))",
            "insert into users(id, name, active) values " +
                "(1, 'Alice', true), (2, 'Bob', false), (3, 'Carol', true)",
            "insert into source_users(id, name, active) values " +
                "(1, 'Alicia', true), (2, 'Bob', false), (4, 'Dave', true), (5, 'Eve', true)",
            "insert into orders(id, user_id, status, amount) values " +
                "(10, 1, 'PAID', 12.50), (11, 1, 'NEW', 7.00), (12, 3, 'PAID', 5.25)",
            "insert into events(user_id, version, payload) values " +
                "(1, 1, 'a'), (1, 2, 'b'), (2, 1, 'c'), (2, 3, 'd')"
        );
    }

    protected void resetMiddlewareSchema() throws Exception {
        executeStatements(
            "drop table if exists payments",
            "drop table if exists orders",
            "drop table if exists users",
            "create table users (" +
                "id bigint primary key," +
                "name text not null," +
                "active boolean not null," +
                "tier text not null," +
                "tenant_id text not null)",
            "create table orders (" +
                "id bigint primary key," +
                "user_id bigint not null references users(id)," +
                "status text not null," +
                "amount numeric(10,2) not null," +
                "tenant_id text not null)",
            "create table payments (" +
                "id bigint primary key," +
                "order_id bigint not null references orders(id)," +
                "state text not null," +
                "tenant_id text not null)",
            "insert into users(id, name, active, tier, tenant_id) values " +
                "(1, 'Alice', true, 'gold', 'tenant-a')," +
                "(2, 'Bob', true, 'silver', 'tenant-a')," +
                "(3, 'Carol', false, 'gold', 'tenant-b')",
            "insert into orders(id, user_id, status, amount, tenant_id) values " +
                "(100, 1, 'PAID', 120.00, 'tenant-a')," +
                "(101, 1, 'PAID', 35.50, 'tenant-a')," +
                "(102, 2, 'PAID', 60.00, 'tenant-a')," +
                "(103, 2, 'NEW', 15.00, 'tenant-a')," +
                "(104, 3, 'PAID', 210.00, 'tenant-b')",
            "insert into payments(id, order_id, state, tenant_id) values " +
                "(1000, 100, 'SETTLED', 'tenant-a')," +
                "(1001, 101, 'SETTLED', 'tenant-a')," +
                "(1002, 102, 'SETTLED', 'tenant-a')," +
                "(1003, 103, 'PENDING', 'tenant-a')," +
                "(1004, 104, 'SETTLED', 'tenant-b')"
        );
    }
}
