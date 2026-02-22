package io.sqm.control;

import io.sqm.core.Query;
import io.sqm.parser.ansi.AnsiSpecs;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SqlQueryParserTest {

    @Test
    void parses_postgresql_query() {
        var parser = SqlQueryParser.standard();
        var context = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);

        var query = parser.parse("select 1", context);
        assertInstanceOf(Query.class, query);
    }

    @Test
    void parses_postgres_alias_dialect() {
        var parser = SqlQueryParser.standard();
        var context = ExecutionContext.of("postgres", ExecutionMode.ANALYZE);

        var query = parser.parse("select 1", context);
        assertInstanceOf(Query.class, query);
    }

    @Test
    void rejects_unsupported_dialect() {
        var parser = SqlQueryParser.standard();
        var context = ExecutionContext.of("mysql", ExecutionMode.ANALYZE);

        assertThrows(IllegalArgumentException.class, () -> parser.parse("select 1", context));
    }

    @Test
    void rejects_parse_errors() {
        var parser = SqlQueryParser.standard();
        var context = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);

        assertThrows(IllegalArgumentException.class, () -> parser.parse("select from", context));
    }

    @Test
    void validates_null_arguments() {
        var parser = SqlQueryParser.standard();
        var context = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);

        assertThrows(NullPointerException.class, () -> parser.parse(null, context));
        assertThrows(NullPointerException.class, () -> parser.parse("select 1", null));
    }

    @Test
    void custom_dialect_aware_factory_validates_configuration() {
        assertThrows(NullPointerException.class, () -> SqlQueryParser.dialectAware(null));

        var parser = SqlQueryParser.dialectAware(Map.of("ansi", AnsiSpecs::new));
        var context = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);

        assertThrows(IllegalArgumentException.class, () -> parser.parse("select 1", context));
    }
}
