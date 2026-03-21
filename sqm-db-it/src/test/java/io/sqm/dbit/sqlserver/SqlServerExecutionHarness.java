package io.sqm.dbit.sqlserver;

import io.sqm.core.Statement;
import io.sqm.dbit.support.DialectExecutionHarness;
import io.sqm.render.spi.RenderContext;
import io.sqm.render.sqlserver.spi.SqlServerDialect;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;

import java.sql.Connection;
import java.sql.DriverManager;

abstract class SqlServerExecutionHarness extends DialectExecutionHarness {
    @Container
    protected static final MSSQLServerContainer<?> SQL_SERVER =
        new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-CU14-ubuntu-22.04")
            .acceptLicense();

    private final RenderContext renderContext = RenderContext.of(new SqlServerDialect());

    @Override
    protected Connection openConnection() throws Exception {
        return DriverManager.getConnection(
            SQL_SERVER.getJdbcUrl(),
            SQL_SERVER.getUsername(),
            SQL_SERVER.getPassword()
        );
    }

    @Override
    protected String render(Statement statement) {
        return renderContext.render(statement).sql();
    }

    @Override
    public java.util.List<String> queryRows(String sql, java.util.List<Object> params) throws Exception {
        return super.queryRows(normalizeExecutableSql(sql), params);
    }

    @Override
    public int executeUpdate(String sql, java.util.List<Object> params) throws Exception {
        return super.executeUpdate(normalizeExecutableSql(sql), params);
    }

    protected void resetDslSchema() throws Exception {
        executeStatements(
            "drop table if exists [audit_names]",
            "drop table if exists [src_users]",
            "drop table if exists [users]",
            "create table [users] (" +
                "[id] bigint primary key," +
                "[name] nvarchar(100) not null," +
                "[nickname] nvarchar(100) null," +
                "[active] bit not null," +
                "[score] int not null," +
                "[created_at] datetime2 not null)",
            "create table [src_users] (" +
                "[id] bigint primary key," +
                "[name] nvarchar(100) not null," +
                "[active] bit not null," +
                "[score] int not null," +
                "[created_at] datetime2 not null)",
            "create table [audit_names] (" +
                "[old_name] nvarchar(100) not null," +
                "[new_name] nvarchar(100) not null)",
            "insert into [users]([id], [name], [nickname], [active], [score], [created_at]) values " +
                "(1, 'Alice', null, 1, 100, '2024-01-01T10:15:00')," +
                "(2, 'Bob', null, 0, 100, '2024-01-02T09:00:00')," +
                "(3, 'Carol', 'C', 1, 90, '2024-01-03T08:30:00')," +
                "(4, 'Dana', null, 1, 80, '2024-01-04T07:45:00')",
            "insert into [src_users]([id], [name], [active], [score], [created_at]) values " +
                "(1, 'Alicia', 1, 100, '2024-01-01T10:15:00')," +
                "(4, 'Dana', 1, 80, '2024-01-04T07:45:00')," +
                "(5, 'Eve', 1, 60, '2024-01-05T06:00:00')"
        );
    }

    private String normalizeExecutableSql(String sql) {
        var trimmed = sql.trim();
        if (!trimmed.regionMatches(true, 0, "MERGE ", 0, "MERGE ".length())) {
            return sql;
        }
        if (trimmed.endsWith(";")) {
            return sql;
        }
        return trimmed + ";";
    }
}
