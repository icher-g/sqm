package io.sqm.parser.ansi;

import io.sqm.core.DeleteStatement;
import io.sqm.core.Statement;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeleteStatementParserTest {

    @Test
    void parsesDeleteWithWhere() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(DeleteStatement.class, "DELETE FROM users WHERE id = 1");

        assertTrue(result.ok(), result.errorMessage());
        var statement = result.value();
        assertEquals("users", statement.table().name().value());
        assertTrue(statement.joins().isEmpty());
        assertNotNull(statement.where());
    }

    @Test
    void parsesDeleteWithoutWhere() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(DeleteStatement.class, "DELETE FROM users");

        assertTrue(result.ok(), result.errorMessage());
        var statement = result.value();
        assertEquals("users", statement.table().name().value());
        assertTrue(statement.using().isEmpty());
        assertTrue(statement.joins().isEmpty());
        assertNull(statement.where());
    }

    @Test
    void statementEntryPointParsesDelete() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(Statement.class, "DELETE FROM users");

        assertTrue(result.ok(), result.errorMessage());
        assertInstanceOf(DeleteStatement.class, result.value());
    }

    @Test
    void rejectsDeleteWithoutFromKeyword() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(DeleteStatement.class, "DELETE users WHERE id = 1");

        assertTrue(result.isError());
        assertEquals("Expected FROM after DELETE at 7", result.errorMessage());
    }

    @Test
    void rejectsDeleteWithoutTable() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(DeleteStatement.class, "DELETE FROM WHERE id = 1");

        assertTrue(result.isError());
    }

    @Test
    void rejectsDeleteUsingInAnsiDialect() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(DeleteStatement.class, "DELETE FROM users USING users JOIN orders ON users.id = orders.user_id");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("DELETE ... USING is not supported by this dialect"));
    }

    @Test
    void rejectsDeleteJoinInAnsiDialectWithoutUsing() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(DeleteStatement.class, "DELETE FROM users JOIN orders ON users.id = orders.user_id");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("DELETE ... JOIN is not supported by this dialect"));
    }

    @Test
    void rejectsDeleteWithResultInAnsiDialect() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(DeleteStatement.class, "DELETE FROM users OUTPUT deleted.id");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("DELETE ... OUTPUT is not supported by this dialect"));
    }

    @Test
    void rejectsDeleteReturningInAnsiDialect() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(DeleteStatement.class, "DELETE FROM users RETURNING id");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("DELETE ... RETURNING is not supported by this dialect"));
    }

    @Test
    void rejectsDeleteWithInvalidWhereExpression() {
        var ctx = ParseContext.of(new AnsiSpecs());
        var result = ctx.parse(DeleteStatement.class, "DELETE FROM users WHERE");

        assertTrue(result.isError());
    }

    @Test
    void exposesDeleteStatementTargetType() {
        assertEquals(DeleteStatement.class, new DeleteStatementParser().targetType());
    }
}
