package io.sqm.parser.ansi;

import io.sqm.core.UpdateStatement;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateStatementParserTest {

    @Test
    void parsesUpdateWithAssignmentsAndWhere() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(UpdateStatement.class, "UPDATE users SET name = 'alice', active = TRUE WHERE id = 1");

        assertTrue(result.ok(), result.errorMessage());
        var statement = result.value();
        assertEquals("users", statement.table().name().value());
        assertEquals(2, statement.assignments().size());
        assertTrue(statement.joins().isEmpty());
        assertTrue(statement.from().isEmpty());
        assertNotNull(statement.where());
    }

    @Test
    void parsesUpdateWithQualifiedAssignmentTarget() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(UpdateStatement.class, "UPDATE users AS u SET u.name = 'alice'");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals(java.util.List.of("u", "name"), result.value().assignments().getFirst().column().values());
    }

    @Test
    void parsesUpdateWithoutWhere() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(UpdateStatement.class, "UPDATE users SET name = 'alice'");

        assertTrue(result.ok(), result.errorMessage());
        var statement = result.value();
        assertEquals(1, statement.assignments().size());
        assertTrue(statement.joins().isEmpty());
        assertTrue(statement.from().isEmpty());
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

    @Test
    void rejectsUpdateWithoutAssignments() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(UpdateStatement.class, "UPDATE users SET");

        assertTrue(result.isError());
    }

    @Test
    void rejectsUpdateJoinInAnsiDialect() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(UpdateStatement.class, "UPDATE users JOIN orders ON users.id = orders.user_id SET name = 'alice'");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).startsWith("UPDATE ... JOIN is not supported by this dialect"));
    }

    @Test
    void rejectsUpdateFromInAnsiDialect() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(UpdateStatement.class, "UPDATE users SET name = 'alice' FROM src_users");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).startsWith("UPDATE ... FROM is not supported by this dialect"));
    }

    @Test
    void rejectsUpdateWithInvalidWhereClause() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(UpdateStatement.class, "UPDATE users SET name = 'alice' WHERE");

        assertTrue(result.isError());
    }

    @Test
    void rejectsUpdateWithInvalidTable() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(UpdateStatement.class, "UPDATE SET name = 'alice'");

        assertTrue(result.isError());
    }

    @Test
    void exposesUpdateStatementTargetType() {
        assertEquals(UpdateStatement.class, new UpdateStatementParser().targetType());
    }
}