package io.sqm.parser.sqlserver;

import io.sqm.core.OutputClause;
import io.sqm.core.Statement;
import io.sqm.core.UpdateStatement;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.sqlserver.spi.SqlServerSpecs;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlServerUpdateStatementParserTest {

    @Test
    void parsesBracketQuotedUpdateStatement() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(UpdateStatement.class, "UPDATE [users] SET [name] = 'alice' WHERE [id] = 1");

        assertTrue(result.ok(), result.errorMessage());
        assertEquals("users", result.value().table().name().value());
        assertEquals(1, result.value().assignments().size());
        assertNotNull(result.value().where());
    }

    @Test
    void statementEntryPointParsesSqlServerUpdate() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(Statement.class, "UPDATE [users] SET [name] = 'alice'");

        assertTrue(result.ok(), result.errorMessage());
        assertInstanceOf(UpdateStatement.class, result.value());
    }

    @Test
    void rejectsUpdateFromInSqlServerBaseline() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(UpdateStatement.class, "UPDATE [users] SET [name] = 'alice' FROM [src_users]");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("UPDATE ... FROM is not supported by this dialect"));
    }

    @Test
    void rejectsUpdateReturningInSqlServerBaseline() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(UpdateStatement.class, "UPDATE [users] SET [name] = 'alice' RETURNING [id]");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("UPDATE ... RETURNING is not supported by this dialect"));
    }

    @Test
    void parsesUpdateOutputClause() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(
            UpdateStatement.class,
            "UPDATE [users] SET [name] = 'alice' OUTPUT deleted.[name], inserted.[name] AS [new_name] WHERE [id] = 1"
        );

        assertTrue(result.ok(), result.errorMessage());
        OutputClause output = result.value().output();
        assertNotNull(output);
        assertEquals(2, output.items().size());
        assertEquals("new_name", output.items().get(1).alias().value());
    }

    @Test
    void parsesUpdateOutputExpressionUsingPseudoColumns() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(
            UpdateStatement.class,
            "UPDATE [users] SET [score] = 1 OUTPUT inserted.[score] + deleted.[score] AS [score_sum] WHERE [id] = 1"
        );

        assertTrue(result.ok(), result.errorMessage());
        assertEquals("score_sum", result.value().output().items().getFirst().alias().value());
    }
}
