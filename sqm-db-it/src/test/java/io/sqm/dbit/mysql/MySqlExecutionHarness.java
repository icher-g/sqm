package io.sqm.dbit.mysql;

import io.sqm.core.Statement;
import io.sqm.dbit.support.DialectExecutionHarness;
import io.sqm.render.mysql.spi.MySqlDialect;
import io.sqm.render.spi.RenderContext;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;

import java.sql.Connection;
import java.sql.DriverManager;

abstract class MySqlExecutionHarness extends DialectExecutionHarness {
    @Container
    protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
        .withDatabaseName("sqm")
        .withUsername("sqm")
        .withPassword("sqm");

    private final RenderContext renderContext = RenderContext.of(new MySqlDialect());

    @Override
    protected Connection openConnection() throws Exception {
        return DriverManager.getConnection(
            MYSQL.getJdbcUrl(),
            MYSQL.getUsername(),
            MYSQL.getPassword()
        );
    }

    @Override
    protected String render(Statement statement) {
        return renderContext.render(statement).sql();
    }

    protected void resetDslSchema() throws Exception {
        executeStatements(
            "drop table if exists orders",
            "drop table if exists users",
            "create table users (" +
                "id bigint primary key," +
                "name varchar(100) not null," +
                "active boolean not null," +
                "payload json not null," +
                "created_at datetime not null," +
                "key idx_users_name (name))",
            "create table orders (" +
                "id bigint primary key," +
                "user_id bigint not null," +
                "status varchar(20) not null," +
                "amount decimal(10,2) not null," +
                "key idx_orders_user (user_id)," +
                "key idx_orders_status (status)," +
                "constraint fk_orders_user foreign key (user_id) references users(id))",
            "insert into users(id, name, active, payload, created_at) values " +
                "(1, 'Alice', true, json_object('user', json_object('id', 1)), '2024-01-01 10:15:00')," +
                "(2, 'Bob', false, json_object('user', json_object('id', 2)), '2024-01-02 09:00:00')," +
                "(3, 'Carol', true, json_object('user', json_object('id', 3)), '2024-01-03 08:30:00')",
            "insert into orders(id, user_id, status, amount) values " +
                "(10, 1, 'closed', 12.50)," +
                "(11, 1, 'new', 7.00)," +
                "(12, 3, 'closed', 5.25)," +
                "(13, 2, 'closed', 20.00)"
        );
    }
}
