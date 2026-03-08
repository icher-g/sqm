package io.sqm.parser.ansi;

import io.sqm.core.UpdateStatement;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UpdateStatementParserTest {

    @Test
    void parsesUpdateWithAssignmentsAndWhere() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(UpdateStatement.class, "UPDATE users SET name = 'alice', active = TRUE WHERE id = 1");

        assertTrue(result.ok(), result.errorMessage());
        var statement = result.value();
        assertEquals("users", statement.table().name().value());
        assertEquals(2, statement.assignments().size());
        assertNotNull(statement.where());
    }

    @Test
    void parsesUpdateWithoutWhere() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(UpdateStatement.class, "UPDATE users SET name = 'alice'");

        assertTrue(result.ok(), result.errorMessage());
        var statement = result.value();
        assertEquals(1, statement.assignments().size());
        assertNull(statement.where());
    }

    @Test
    void statementEntryPointParsesUpdate() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(io.sqm.core.Statement.class, "UPDATE users SET name = 'alice'");

        assertTrue(result.ok(), result.errorMessage());
        assertInstanceOf(UpdateStatement.class, result.value());
    }

    @Test
    void rejectsUpdateWithoutSetKeyword() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(UpdateStatement.class, "UPDATE users name = 'alice'");

        assertTrue(result.isError());
        assertEquals("Expected SET after UPDATE table at 18", result.errorMessage());
    }
}



