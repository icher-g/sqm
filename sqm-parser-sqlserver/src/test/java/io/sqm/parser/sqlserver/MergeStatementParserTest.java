package io.sqm.parser.sqlserver;

import io.sqm.core.MergeStatement;
import io.sqm.core.Statement;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.sqlserver.spi.SqlServerSpecs;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MergeStatementParserTest {

    @Test
    void parsesSqlServerMergeFirstSlice() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(
            MergeStatement.class,
            """
                MERGE INTO [users] WITH (HOLDLOCK)
                USING [src_users] AS [s]
                ON [users].[id] = [s].[id]
                WHEN MATCHED THEN UPDATE SET [name] = [s].[name]
                WHEN NOT MATCHED THEN INSERT ([id], [name]) VALUES ([s].[id], [s].[name])
                """
        );

        assertTrue(result.ok(), result.errorMessage());
        assertEquals("users", result.value().target().name().value());
        assertEquals(1, result.value().target().lockHints().size());
        assertEquals(2, result.value().clauses().size());
    }

    @Test
    void statementEntryPointParsesSqlServerMerge() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(
            Statement.class,
            "MERGE users USING src ON users.id = src.id WHEN MATCHED THEN DELETE"
        );

        assertTrue(result.ok(), result.errorMessage());
        assertInstanceOf(MergeStatement.class, result.value());
    }

    @Test
    void rejectsMergeOutputInFirstSlice() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(
            MergeStatement.class,
            "MERGE users USING src ON users.id = src.id WHEN MATCHED THEN DELETE OUTPUT deleted.id"
        );

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("OUTPUT"));
    }

    @Test
    void rejectsMergeActionPredicatesInFirstSlice() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(
            MergeStatement.class,
            "MERGE users USING src ON users.id = src.id WHEN MATCHED AND src.active = 1 THEN DELETE"
        );

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("predicates"));
    }

    @Test
    void rejectsDuplicateMatchedUpdateClauseInFirstSlice() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(
            MergeStatement.class,
            """
                MERGE users USING src ON users.id = src.id
                WHEN MATCHED THEN UPDATE SET name = src.name
                WHEN MATCHED THEN UPDATE SET name = src.other_name
                """
        );

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("at most one WHEN MATCHED THEN UPDATE"));
    }

    @Test
    void rejectsMergeTopInFirstSlice() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(
            MergeStatement.class,
            "MERGE TOP (10) users USING src ON users.id = src.id WHEN MATCHED THEN DELETE"
        );

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("TOP"));
    }

    @Test
    void rejectsNotMatchedBySourceInFirstSlice() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(
            MergeStatement.class,
            "MERGE users USING src ON users.id = src.id WHEN NOT MATCHED BY SOURCE THEN DELETE"
        );

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("BY"));
    }

    @Test
    void rejectsWhenMatchedInsertAction() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(
            MergeStatement.class,
            "MERGE users USING src ON users.id = src.id WHEN MATCHED THEN INSERT (id) VALUES (src.id)"
        );

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("cannot use INSERT"));
    }

    @Test
    void rejectsWhenNotMatchedUpdateAction() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(
            MergeStatement.class,
            "MERGE users USING src ON users.id = src.id WHEN NOT MATCHED THEN UPDATE SET name = src.name"
        );

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("must use INSERT"));
    }

    @Test
    void rejectsDuplicateMatchedDeleteClauseInFirstSlice() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(
            MergeStatement.class,
            """
                MERGE users USING src ON users.id = src.id
                WHEN MATCHED THEN DELETE
                WHEN MATCHED THEN DELETE
                """
        );

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("at most one WHEN MATCHED THEN DELETE"));
    }

    @Test
    void rejectsDuplicateNotMatchedInsertClauseInFirstSlice() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(
            MergeStatement.class,
            """
                MERGE users USING src ON users.id = src.id
                WHEN NOT MATCHED THEN INSERT (id) VALUES (src.id)
                WHEN NOT MATCHED THEN INSERT (id) VALUES (src.id)
                """
        );

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("at most one WHEN NOT MATCHED THEN INSERT"));
    }
}
