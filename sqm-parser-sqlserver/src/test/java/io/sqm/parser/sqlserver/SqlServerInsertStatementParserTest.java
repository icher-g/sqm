package io.sqm.parser.sqlserver;

import io.sqm.core.InsertStatement;
import io.sqm.core.RowExpr;
import io.sqm.core.Statement;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.sqlserver.spi.SqlServerSpecs;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
