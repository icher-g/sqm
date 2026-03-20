package io.sqm.parser.sqlserver;

import io.sqm.core.MergeStatement;
import io.sqm.core.Statement;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.sqlserver.spi.SqlServerSpecs;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class MergeStatementParserTest {

    @Test
    void parsesSqlServerMergeFirstSlice() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(
            MergeStatement.class,
            """
                MERGE TOP (10) PERCENT INTO [users] WITH (HOLDLOCK)
                USING [src_users] AS [s]
                ON [users].[id] = [s].[id]
                WHEN MATCHED AND [s].[active] = 1 THEN UPDATE SET [name] = [s].[name]
                WHEN NOT MATCHED AND [s].[name] IS NOT NULL THEN INSERT ([id], [name]) VALUES ([s].[id], [s].[name])
                """
        );

        assertTrue(result.ok(), result.errorMessage());
        assertEquals("users", result.value().target().name().value());
        assertEquals(1, result.value().target().lockHints().size());
        assertNotNull(result.value().topSpec());
        assertTrue(result.value().topSpec().percent());
        assertEquals(2, result.value().clauses().size());
        assertTrue(result.value().clauses().stream().allMatch(clause -> clause.condition() != null));
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
    void parsesMergeOutput() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(
            MergeStatement.class,
            "MERGE users USING src ON users.id = src.id WHEN MATCHED THEN DELETE OUTPUT deleted.id"
        );

        assertTrue(result.ok(), result.errorMessage());
        assertNotNull(result.value().result());
        assertEquals(1, result.value().result().items().size());
    }

    @Test
    void parsesMergeOutputInto() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(
            MergeStatement.class,
            "MERGE users USING src ON users.id = src.id WHEN MATCHED THEN DELETE OUTPUT deleted.id INTO audit (user_id)"
        );

        assertTrue(result.ok(), result.errorMessage());
        assertNotNull(result.value().result());
        assertNotNull(result.value().result().into());
        assertEquals("audit", result.value().result().into().target().name().value());
        assertEquals(1, result.value().result().into().columns().size());
    }

    @Test
    void rejectsMergeOutputIntoTargetHints() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(
            MergeStatement.class,
            "MERGE users USING src ON users.id = src.id WHEN MATCHED THEN DELETE OUTPUT deleted.id INTO audit WITH (NOLOCK)"
        );

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("OUTPUT INTO targets"));
    }

    @Test
    void parsesMergeActionPredicatesInCurrentSlice() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(
            MergeStatement.class,
            "MERGE users USING src ON users.id = src.id WHEN MATCHED AND src.active = 1 THEN DELETE"
        );

        assertTrue(result.ok(), result.errorMessage());
        assertNotNull(result.value().clauses().getFirst().condition());
    }

    @Test
    void acceptsTwoMatchedClausesWithFirstPredicateAndDistinctActions() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(
            MergeStatement.class,
            """
                MERGE users USING src ON users.id = src.id
                WHEN MATCHED AND src.active = 1 THEN UPDATE SET name = src.name
                WHEN MATCHED THEN DELETE
                """
        );

        assertTrue(result.ok(), result.errorMessage());
        assertEquals(2, result.value().clauses().size());
    }

    @Test
    void rejectsMergeTopWithTies() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(
            MergeStatement.class,
            "MERGE TOP (10) WITH TIES users USING src ON users.id = src.id WHEN MATCHED THEN DELETE"
        );

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("WITH TIES"));
    }

    @Test
    void parsesNotMatchedBySourceInCurrentSlice() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(
            MergeStatement.class,
            "MERGE users USING src ON users.id = src.id WHEN NOT MATCHED BY SOURCE AND users.active = 1 THEN UPDATE SET name = src.name"
        );

        assertTrue(result.ok(), result.errorMessage());
        assertEquals(1, result.value().clauses().size());
        assertEquals(io.sqm.core.MergeClause.MatchType.NOT_MATCHED_BY_SOURCE, result.value().clauses().getFirst().matchType());
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
    void rejectsDoNothingAction() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(
            MergeStatement.class,
            "MERGE users USING src ON users.id = src.id WHEN MATCHED THEN DO NOTHING"
        );

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("DO NOTHING"));
    }

    @Test
    void rejectsTwoMatchedClausesWithoutPredicateOnFirstClause() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(
            MergeStatement.class,
            """
                MERGE users USING src ON users.id = src.id
                WHEN MATCHED THEN DELETE
                WHEN MATCHED THEN UPDATE SET name = src.name
                """
        );

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("first WHEN MATCHED"));
    }

    @Test
    void rejectsTwoMatchedClausesWithSameActionFamily() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(
            MergeStatement.class,
            """
                MERGE users USING src ON users.id = src.id
                WHEN MATCHED AND src.active = 1 THEN DELETE
                WHEN MATCHED THEN DELETE
                """
        );

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("one UPDATE and one DELETE"));
    }

    @Test
    void rejectsTwoNotMatchedBySourceClausesWithoutPredicateOnFirstClause() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(
            MergeStatement.class,
            """
                MERGE users USING src ON users.id = src.id
                WHEN NOT MATCHED BY SOURCE THEN DELETE
                WHEN NOT MATCHED BY SOURCE THEN UPDATE SET name = src.name
                """
        );

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("first WHEN NOT MATCHED BY SOURCE"));
    }

    @Test
    void rejectsTwoNotMatchedBySourceClausesWithSameActionFamily() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(
            MergeStatement.class,
            """
                MERGE users USING src ON users.id = src.id
                WHEN NOT MATCHED BY SOURCE AND users.active = 1 THEN DELETE
                WHEN NOT MATCHED BY SOURCE THEN DELETE
                """
        );

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("one UPDATE and one DELETE"));
    }

    @Test
    void rejectsMoreThanTwoMatchedClauses() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(
            MergeStatement.class,
            """
                MERGE users USING src ON users.id = src.id
                WHEN MATCHED AND src.active = 1 THEN UPDATE SET name = src.name
                WHEN MATCHED THEN DELETE
                WHEN MATCHED THEN DELETE
                """
        );

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("at most two WHEN MATCHED"));
    }

    @Test
    void rejectsMalformedMatchedClausePredicate() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(
            MergeStatement.class,
            "MERGE users USING src ON users.id = src.id WHEN MATCHED AND THEN DELETE"
        );

        assertTrue(result.isError());
    }

    @Test
    void rejectsDuplicateNotMatchedInsertClauseInCurrentSlice() {
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
