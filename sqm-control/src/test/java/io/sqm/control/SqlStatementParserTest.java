package io.sqm.control;

import io.sqm.control.execution.ExecutionContext;
import io.sqm.control.execution.ExecutionMode;
import io.sqm.core.DeleteStatement;
import io.sqm.control.pipeline.SqlStatementParser;
import io.sqm.core.InsertStatement;
import io.sqm.core.Query;
import io.sqm.core.UpdateStatement;
import io.sqm.parser.ansi.AnsiSpecs;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlStatementParserTest {

    @Test
    void parses_postgresql_query() {
        var parser = SqlStatementParser.standard();
        var context = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);

        var query = parser.parse("select 1", context);
        assertInstanceOf(Query.class, query);
    }

    @Test
    void parses_postgres_alias_dialect() {
        var parser = SqlStatementParser.standard();
        var context = ExecutionContext.of("postgres", ExecutionMode.ANALYZE);

        var query = parser.parse("select 1", context);
        assertInstanceOf(Query.class, query);
    }

    @Test
    void parses_mysql_query() {
        var parser = SqlStatementParser.standard();
        var context = ExecutionContext.of("mysql", ExecutionMode.ANALYZE);

        var query = parser.parse("select 1", context);
        assertInstanceOf(Query.class, query);
    }

    @Test
    void parses_mysql_insert_statement() {
        var parser = SqlStatementParser.standard();
        var context = ExecutionContext.of("mysql", ExecutionMode.ANALYZE);

        var statement = parser.parse("insert into users (id, name) values (1, 'alice')", context);
        assertInstanceOf(InsertStatement.class, statement);
    }

    @Test
    void parses_sqlserver_query() {
        var parser = SqlStatementParser.standard();
        var context = ExecutionContext.of("sqlserver", ExecutionMode.ANALYZE);

        var query = parser.parse("select [u].[id] from [users] as [u]", context);
        assertInstanceOf(Query.class, query);
    }

    @Test
    void parses_sqlserver_alias_dialect() {
        var parser = SqlStatementParser.standard();
        var context = ExecutionContext.of("mssql", ExecutionMode.ANALYZE);

        var query = parser.parse("select 1", context);
        assertInstanceOf(Query.class, query);
    }

    @Test
    void parses_sqlserver_insert_statement() {
        var parser = SqlStatementParser.standard();
        var context = ExecutionContext.of("sqlserver", ExecutionMode.ANALYZE);

        var statement = parser.parse("INSERT INTO [users] ([id]) VALUES (1)", context);
        assertInstanceOf(InsertStatement.class, statement);
    }

    @Test
    void parses_sqlserver_insert_statement_with_output() {
        var parser = SqlStatementParser.standard();
        var context = ExecutionContext.of("sqlserver", ExecutionMode.ANALYZE);

        var statement = parser.parse("INSERT INTO [users] ([name]) OUTPUT inserted.[id] VALUES ('alice')", context);
        assertInstanceOf(InsertStatement.class, statement);
    }

    @Test
    void parses_sqlserver_update_statement() {
        var parser = SqlStatementParser.standard();
        var context = ExecutionContext.of("sqlserver", ExecutionMode.ANALYZE);

        var statement = parser.parse("UPDATE [users] SET [name] = 'alice'", context);
        assertInstanceOf(UpdateStatement.class, statement);
    }

    @Test
    void parses_sqlserver_delete_statement() {
        var parser = SqlStatementParser.standard();
        var context = ExecutionContext.of("sqlserver", ExecutionMode.ANALYZE);

        var statement = parser.parse("DELETE FROM [users]", context);
        assertInstanceOf(DeleteStatement.class, statement);
    }

    @Test
    void parses_sqlserver_delete_statement_with_output() {
        var parser = SqlStatementParser.standard();
        var context = ExecutionContext.of("sqlserver", ExecutionMode.ANALYZE);

        var statement = parser.parse("DELETE FROM [users] OUTPUT deleted.[id] WHERE [id] = 1", context);
        assertInstanceOf(DeleteStatement.class, statement);
    }

    @Test
    void rejects_parse_errors() {
        var parser = SqlStatementParser.standard();
        var context = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);

        assertThrows(IllegalArgumentException.class, () -> parser.parse("select from", context));
    }

    @Test
    void validates_null_arguments() {
        var parser = SqlStatementParser.standard();
        var context = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);

        assertThrows(NullPointerException.class, () -> parser.parse(null, context));
        assertThrows(NullPointerException.class, () -> parser.parse("select 1", null));
    }

    @Test
    void custom_dialect_aware_factory_validates_configuration() {
        assertThrows(NullPointerException.class, () -> SqlStatementParser.dialectAware(null));

        var parser = SqlStatementParser.dialectAware(Map.of("ansi", AnsiSpecs::new));
        var context = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);

        assertThrows(IllegalArgumentException.class, () -> parser.parse("select 1", context));
    }
}
