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

    protected void resetDslSchema() throws Exception {
        dropSequenceIfExists("users_seq");
        dropTableIfExists("src_users");
        dropTableIfExists("users");
        executeStatements(
            "create table users (id number(19) primary key, name varchar2(100) not null, active number(1) not null)",
            "create table src_users (id number(19) primary key, name varchar2(100) not null, active number(1) not null)",
            "insert into users(id, name, active) values (1, 'Alice', 1)",
            "insert into users(id, name, active) values (2, 'Bob', 0)",
            "insert into src_users(id, name, active) values (1, 'Alicia', 1)",
            "insert into src_users(id, name, active) values (3, 'Carol', 1)",
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
