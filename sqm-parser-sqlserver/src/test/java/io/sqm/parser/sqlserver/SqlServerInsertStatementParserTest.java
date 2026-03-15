package io.sqm.parser.sqlserver;

import io.sqm.core.InsertStatement;
import io.sqm.core.OutputColumnExpr;
import io.sqm.core.OutputRowSource;
import io.sqm.core.RowExpr;
import io.sqm.core.Statement;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.sqlserver.spi.SqlServerSpecs;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class SqlServerInsertStatementParserTest {

    @Test
    void parsesBracketQuotedInsertValuesStatement() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(InsertStatement.class, "INSERT INTO [users] ([id], [name]) VALUES (1, 'alice')");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals("users", result.value().table().name().value());
        assertEquals(2, result.value().columns().size());
        assertInstanceOf(RowExpr.class, result.value().source());
    }

    @Test
    void statementEntryPointParsesSqlServerInsert() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(Statement.class, "INSERT INTO [users] ([id]) VALUES (1)");

        assertTrue(result.ok(), result.errorMessage());
        assertInstanceOf(InsertStatement.class, result.value());
    }

    @Test
    void rejectsOnConflictInSqlServerDialect() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(InsertStatement.class, "INSERT INTO [users] VALUES (1) ON CONFLICT ([id]) DO NOTHING");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("INSERT ... ON CONFLICT is not supported by this dialect"));
    }

    @Test
    void rejectsReturningInSqlServerDialect() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(InsertStatement.class, "INSERT INTO [users] VALUES (1) RETURNING [id]");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("INSERT ... RETURNING is not supported by this dialect"));
    }

    @Test
    void parsesInsertOutputClause() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(
            InsertStatement.class,
            "INSERT INTO [users] ([name]) OUTPUT inserted.[id] AS [user_id] VALUES ('alice')"
        );

        assertTrue(result.ok(), result.errorMessage());
        assertNotNull(result.value().output());
        assertEquals(1, result.value().output().items().size());
        assertEquals("user_id", result.value().output().items().getFirst().alias().value());
        assertInstanceOf(OutputColumnExpr.class, result.value().output().items().getFirst().expression());
        assertEquals(OutputRowSource.INSERTED, ((OutputColumnExpr) result.value().output().items().getFirst().expression()).source());
    }

    @Test
    void parsesInsertOutputExpressionUsingPseudoColumn() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(
            InsertStatement.class,
            "INSERT INTO [users] ([name]) OUTPUT inserted.[id] + 1 AS [next_id] VALUES ('alice')"
        );

        assertTrue(result.ok(), result.errorMessage());
        assertEquals("next_id", result.value().output().items().getFirst().alias().value());
    }

    @Test
    void rejectsInsertOutputDeletedReference() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(
            InsertStatement.class,
            "INSERT INTO [users] ([name]) OUTPUT deleted.[id] VALUES ('alice')"
        );

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("deleted.<column>"));
    }
}
