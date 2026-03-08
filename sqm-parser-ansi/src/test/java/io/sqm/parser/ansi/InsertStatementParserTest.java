package io.sqm.parser.ansi;

import io.sqm.core.InsertStatement;
import io.sqm.core.RowListExpr;
import io.sqm.core.SelectQuery;
import io.sqm.core.Statement;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InsertStatementParserTest {

    @Test
    void parsesInsertValuesWithTargetColumns() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(InsertStatement.class, "INSERT INTO users (id, name) VALUES (1, 'alice'), (2, 'bob')");

        assertTrue(result.ok(), result.errorMessage());
        var statement = result.value();
        assertEquals("users", statement.table().name().value());
        assertEquals(2, statement.columns().size());
        assertInstanceOf(RowListExpr.class, statement.source());
    }

    @Test
    void parsesInsertSelectWithTargetColumns() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(InsertStatement.class, "INSERT INTO users (id) SELECT id FROM source_users");

        assertTrue(result.ok(), result.errorMessage());
        var statement = result.value();
        assertEquals("users", statement.table().name().value());
        assertEquals(1, statement.columns().size());
        assertInstanceOf(SelectQuery.class, statement.source());
    }

    @Test
    void statementEntryPointParsesInsert() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(Statement.class, "INSERT INTO users (id) VALUES (1)");

        assertTrue(result.ok(), result.errorMessage());
        assertInstanceOf(InsertStatement.class, result.value());
    }

    @Test
    void rejectsInsertWithoutIntoKeyword() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(InsertStatement.class, "INSERT users (id) VALUES (1)");

        assertTrue(result.isError());
        assertEquals("Expected INTO after INSERT at 7", result.errorMessage());
    }
}



