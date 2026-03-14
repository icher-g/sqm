package io.sqm.control;

import io.sqm.control.execution.ExecutionContext;
import io.sqm.control.execution.ExecutionMode;
import io.sqm.control.pipeline.SqlStatementParser;
import io.sqm.core.InsertStatement;
import io.sqm.core.Query;
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
