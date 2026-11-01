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
    private static boolean flashbackFixtureInitialized;
    private static long flashbackFixtureCommitScn;

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
     * Returns a database timestamp whose mapped SCN includes the committed flashback fixture.
     * Oracle's approximate timestamp mapping is checked explicitly rather than assuming a fixture age.
     *
     * @return Oracle timestamp resolving at or after the fixture commit SCN
     * @throws Exception when reading or waiting for the timestamp mapping fails
     */
    protected Timestamp currentDatabaseTimestamp() throws Exception {
        initializeFlashbackFixture();
        try (var connection = openConnection();
             var clock = connection.createStatement();
             var mapping = connection.prepareStatement("select timestamp_to_scn(?) from dual")) {
            var deadline = System.nanoTime() + Duration.ofSeconds(30).toNanos();
            do {
                Timestamp candidate;
                try (var resultSet = clock.executeQuery("select cast(systimestamp as timestamp) from dual")) {
                    if (!resultSet.next()) throw new IllegalStateException("Oracle did not return SYSTIMESTAMP");
                    candidate = resultSet.getTimestamp(1);
                }
                // Check the exact JDBC value that the flashback query will bind.
                mapping.setTimestamp(1, candidate);
                try (var resultSet = mapping.executeQuery()) {
                    if (!resultSet.next()) throw new IllegalStateException("Oracle did not return TIMESTAMP_TO_SCN");
                    if (resultSet.getLong(1) >= flashbackFixtureCommitScn) return candidate;
                }
                Thread.sleep(100);
            }
            while (System.nanoTime() < deadline);
            throw new SQLException("Oracle timestamp mapping did not reach fixture commit SCN " + flashbackFixtureCommitScn);
        }
    }

    /**
     * Recreates the flashback fixture for immediate-after-DDL regression checks.
     *
     * @throws Exception when fixture setup fails
     */
    protected void resetFlashbackFixture() throws Exception {
        dropTableIfExists("flashback_users");
        flashbackFixtureInitialized = false;
        initializeFlashbackFixture();
    }

    protected void resetDslSchema() throws Exception {
        initializeFlashbackFixture();
        dropSequenceIfExists("users_seq");
        dropTableIfExists("pattern_events");
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
            "create table pattern_events (account_id number(19) not null, event_id number(19) not null, "
                + "amount number(19) not null, kind varchar2(1) not null, primary key (account_id, event_id))",
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
            "insert into pattern_events(account_id, event_id, amount, kind) values (1, 1, 10, 'A')",
            "insert into pattern_events(account_id, event_id, amount, kind) values (1, 2, 12, 'B')",
            "insert into pattern_events(account_id, event_id, amount, kind) values (1, 3, 14, 'B')",
            "insert into pattern_events(account_id, event_id, amount, kind) values (1, 4, 9, 'A')",
            "insert into pattern_events(account_id, event_id, amount, kind) values (1, 5, 11, 'B')",
            "insert into pattern_events(account_id, event_id, amount, kind) values (1, 6, 13, 'B')",
            "insert into pattern_events(account_id, event_id, amount, kind) values (2, 1, 20, 'A')",
            "insert into pattern_events(account_id, event_id, amount, kind) values (2, 2, 22, 'B')",
            "insert into pattern_events(account_id, event_id, amount, kind) values (2, 3, 18, 'A')",
            "insert into pattern_events(account_id, event_id, amount, kind) values (2, 4, 19, 'B')",
            "insert into pattern_events(account_id, event_id, amount, kind) values (2, 5, 21, 'B')",
            "create sequence users_seq start with 100 increment by 1"
        );
    }

    /**
     * Recreates the committed MODEL fixtures without resetting baseline Oracle fixtures.
     *
     * @throws Exception when fixture setup fails
     */
    protected void resetModelSchema() throws Exception {
        for (var table : new String[]{"model_sales", "model_rates", "model_years", "model_targets", "model_output"}) {
            dropTableIfExists(table);
        }
        // Each setup connection uses JDBC autocommit; every test starts from committed rows.
        executeStatements(
            "create table model_sales (region varchar2(1), product varchar2(10), year_key number(4), amount number(19))",
            "create table model_rates (product varchar2(10) primary key, rate number(19))",
            "create table model_years (year_key number(4) primary key)",
            "create table model_targets (product varchar2(10), year_key number(4))",
            "create table model_output (product varchar2(10), year_key number(4), sales number(19))",
            "insert into model_sales values ('N', 'Bike', 2025, 10)",
            "insert into model_sales values ('N', 'Bike', 2026, 20)",
            "insert into model_sales values ('N', 'Car', 2025, 30)",
            "insert into model_sales values ('N', 'Car', 2026, null)",
            "insert into model_sales values ('S', 'Bike', 2025, 100)",
            "insert into model_sales values ('S', 'Bike', 2026, 200)",
            "insert into model_sales values ('S', 'Car', 2025, 300)",
            "insert into model_sales values ('S', 'Car', 2026, null)",
            "insert into model_rates values ('Bike', 2)",
            "insert into model_rates values ('Car', 3)",
            "insert into model_years values (2027)",
            "insert into model_years values (2028)",
            "insert into model_targets values ('Bike', 2027)",
            "insert into model_targets values ('Car', 2028)"
        );
    }

    private void initializeFlashbackFixture() throws Exception {
        if (flashbackFixtureInitialized) {
            return;
        }
        executeStatements(
            "create table flashback_users (id number(19) primary key, name varchar2(100) not null)",
            "insert into flashback_users(id, name) values (1, 'Alice')",
            "insert into flashback_users(id, name) values (2, 'Bob')"
        );
        // ORA_ROWSCN gives a conservative commit boundary without requiring catalog/package grants.
        flashbackFixtureCommitScn = Long.parseLong(queryRows("select max(ora_rowscn) from flashback_users").getFirst());
        flashbackFixtureInitialized = true;
    }

    /**
     * Drops a fixture table, ignoring only Oracle's table-not-found error.
     *
     * @param tableName trusted fixture table name
     * @throws Exception when dropping the table fails for any other reason
     */
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
