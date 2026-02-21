package io.sqm.control;

import io.sqm.core.Query;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultSqlQueryParserTest {

    @Test
    void parses_postgresql_query() {
        var parser = DefaultSqlQueryParser.standard();
        var context = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);

        var query = parser.parse("select 1", context);
        assertInstanceOf(Query.class, query);
    }

    @Test
    void rejects_unsupported_dialect() {
        var parser = DefaultSqlQueryParser.standard();
        var context = ExecutionContext.of("mysql", ExecutionMode.ANALYZE);

        assertThrows(IllegalArgumentException.class, () -> parser.parse("select 1", context));
    }

    @Test
    void rejects_parse_errors() {
        var parser = DefaultSqlQueryParser.standard();
        var context = ExecutionContext.of("postgresql", ExecutionMode.ANALYZE);

        assertThrows(IllegalArgumentException.class, () -> parser.parse("select from", context));
    }
}

