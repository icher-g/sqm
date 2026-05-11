package io.sqm.parser.sqlserver;

import io.sqm.core.VariableTable;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.sqlserver.spi.SqlServerSpecs;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VariableTableParserTest {

    @Test
    void parsesSqlServerVariableTable() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(VariableTable.class, "@audit");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals("audit", result.value().name().value());
    }

    @Test
    void parserMatchesOnlyAtSignIdentifierPattern() {
        var parser = new VariableTableParser();
        var ctx = ParseContext.of(new SqlServerSpecs());

        assertTrue(parser.match(Cursor.of("@audit", ctx.identifierQuoting()), ctx));
        assertFalse(parser.match(Cursor.of("@", ctx.identifierQuoting()), ctx));
        assertFalse(parser.match(Cursor.of("audit", ctx.identifierQuoting()), ctx));
        assertEquals(VariableTable.class, parser.targetType());
    }

    @Test
    void parserReportsErrorWhenCursorDoesNotContainVariableTable() {
        var parser = new VariableTableParser();
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = parser.parse(Cursor.of("audit", ctx.identifierQuoting()), ctx);

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("Expected SQL Server table variable"));
    }
}
