package io.sqm.dbit.oracle;

import io.sqm.core.Statement;
import io.sqm.dbit.support.DialectExecutionHarness;
import io.sqm.render.oracle.spi.OracleDialect;
import io.sqm.render.spi.RenderContext;
import oracle.jdbc.OraclePreparedStatement;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Duration;

abstract class OracleExecutionHarness extends DialectExecutionHarness {
    private static final String DATABASE_PASSWORD = "SqmOracle1!";

    @Container
    protected static final GenericContainer<?> ORACLE = new GenericContainer<>(
        DockerImageName.parse("gvenzl/oracle-free:23-slim-faststart")
    )
        .withEnv("ORACLE_PASSWORD", DATABASE_PASSWORD)
        .withEnv("APP_USER", "sqm")
        .withEnv("APP_USER_PASSWORD", DATABASE_PASSWORD)
        .withExposedPorts(1521)
        .withLogConsumer(containerLogConsumer("Oracle"))
        .waitingFor(Wait.forLogMessage(".*DATABASE IS READY TO USE!.*\\s", 1))
        .withStartupTimeout(Duration.ofMinutes(5));

    private final RenderContext renderContext = RenderContext.of(new OracleDialect());

    @Override
    protected Connection openConnection() throws Exception {
        return java.sql.DriverManager.getConnection(
            "jdbc:oracle:thin:@//" + ORACLE.getHost() + ':' + ORACLE.getMappedPort(1521) + "/FREEPDB1",
            "sqm",
            DATABASE_PASSWORD
        );
    }

    @Override
    protected String render(Statement statement) {
        return renderContext.render(statement).sql();
    }

    /**
     * Executes Oracle DML rendered with a single {@code RETURNING ... INTO :1} target.
     *
     * @param sql rendered Oracle DML statement
     * @return the returned numeric value
     * @throws Exception when the statement cannot execute
     */
    protected long executeReturningInto(String sql) throws Exception {
        try (var connection = openConnection();
             var statement = connection.prepareStatement(sql).unwrap(OraclePreparedStatement.class)) {
            statement.registerReturnParameter(1, Types.NUMERIC);
            statement.executeUpdate();
            try (var resultSet = statement.getReturnResultSet()) {
                if (!resultSet.next()) {
                    throw new IllegalStateException("Oracle RETURNING INTO produced no value");
                }
                return resultSet.getLong(1);
            }
        }
    }

    /**
     * Returns the current database timestamp for a flashback query that must be later than fixture DDL.
     *
     * @return current Oracle database timestamp
     * @throws Exception when the timestamp cannot be read
     */
    protected Timestamp currentDatabaseTimestamp() throws Exception {
        try (var connection = openConnection();
             var statement = connection.createStatement();
             var resultSet = statement.executeQuery("select systimestamp from dual")) {
            if (!resultSet.next()) {
                throw new IllegalStateException("Oracle did not return SYSTIMESTAMP");
            }
            return resultSet.getTimestamp(1);
        }
    }

    protected void resetDslSchema() throws Exception {
        dropSequenceIfExists("users_seq");
        dropTableIfExists("events");
        dropTableIfExists("sales_wide");
        dropTableIfExists("sales");
        dropTableIfExists("categories");
        dropTableIfExists("orders");
        dropTableIfExists("src_users");
        dropTableIfExists("users");
        executeStatements(
            "create table users (id number(19) primary key, name varchar2(100) not null, active number(1) not null)",
            "create table src_users (id number(19) primary key, name varchar2(100) not null, active number(1) not null)",
            "create table orders (id number(19) primary key, user_id number(19) not null)",
            "create table categories (id number(19) primary key, parent_id number(19), name varchar2(100) not null)",
            "create table sales (id number(19) not null, quarter varchar2(2) not null, amount number(19) not null) "
                + "partition by list (quarter) (partition sales_q1 values ('Q1'), partition sales_q2 values ('Q2'))",
            "create table sales_wide (id number(19) primary key, q1 number(19) not null, q2 number(19) not null)",
            "create table events (id number(19) primary key, created_at timestamp with time zone not null)",
            "insert into users(id, name, active) values (1, 'Alice', 1)",
            "insert into users(id, name, active) values (2, 'Bob', 0)",
            "insert into src_users(id, name, active) values (1, 'Alicia', 1)",
            "insert into src_users(id, name, active) values (3, 'Carol', 1)",
            "insert into orders(id, user_id) values (10, 1)",
            "insert into orders(id, user_id) values (11, 1)",
            "insert into orders(id, user_id) values (12, 2)",
            "insert into categories(id, parent_id, name) values (1, null, 'Root')",
            "insert into categories(id, parent_id, name) values (2, 1, 'Alpha')",
            "insert into categories(id, parent_id, name) values (3, 1, 'Beta')",
            "insert into sales(id, quarter, amount) values (1, 'Q1', 10)",
            "insert into sales(id, quarter, amount) values (2, 'Q1', 20)",
            "insert into sales(id, quarter, amount) values (3, 'Q2', 30)",
            "insert into sales_wide(id, q1, q2) values (1, 10, 20)",
            "insert into events(id, created_at) values (1, systimestamp)",
            "create sequence users_seq start with 100 increment by 1"
        );
    }

    private void dropTableIfExists(String tableName) throws Exception {
        try {
            executeStatements("drop table " + tableName + " purge");
        }
        catch (SQLException exception) {
            if (exception.getErrorCode() != 942) {
                throw exception;
            }
        }
    }

    private void dropSequenceIfExists(String sequenceName) throws Exception {
        try {
            executeStatements("drop sequence " + sequenceName);
        }
        catch (SQLException exception) {
            if (exception.getErrorCode() != 2289) {
                throw exception;
            }
        }
    }
}
