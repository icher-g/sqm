package io.sqm.parser.sqlserver;

import io.sqm.core.DeleteStatement;
import io.sqm.core.Statement;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.sqlserver.spi.SqlServerSpecs;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlServerDeleteStatementParserTest {

    @Test
    void parsesBracketQuotedDeleteStatement() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(DeleteStatement.class, "DELETE FROM [users] WHERE [id] = 1");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals("users", result.value().table().name().value());
        assertNotNull(result.value().where());
    }

    @Test
    void statementEntryPointParsesSqlServerDelete() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(Statement.class, "DELETE FROM [users]");

        assertTrue(result.ok(), result.errorMessage());
        assertInstanceOf(DeleteStatement.class, result.value());
    }

    @Test
    void rejectsDeleteUsingInSqlServerBaseline() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(DeleteStatement.class, "DELETE FROM [users] USING [src_users]");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("DELETE ... USING is not supported by this dialect"));
    }

    @Test
    void rejectsDeleteReturningInSqlServerBaseline() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(DeleteStatement.class, "DELETE FROM [users] RETURNING [id]");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("DELETE ... RETURNING is not supported by this dialect"));
    }
}
