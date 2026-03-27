package io.sqm.parser.sqlserver;

import io.sqm.core.*;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.sqlserver.spi.SqlServerSpecs;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class SelectQueryParserTest {

    @Test
    void parses_distinct_top_query() {
        var context = ParseContext.of(new SqlServerSpecs());
        var result = context.parse(Query.class, "SELECT DISTINCT TOP (10) [u].[id] FROM [users] AS [u]");

        assertFalse(result.isError());
    }

    @Test
    void parses_order_by_offset_fetch_query() {
        var context = ParseContext.of(new SqlServerSpecs());
        var result = context.parse(Query.class, "SELECT [u].[id] FROM [users] AS [u] ORDER BY [u].[id] OFFSET 5 ROWS FETCH NEXT 10 ROWS ONLY");

        assertFalse(result.isError());
    }

    @Test
    void rejects_offset_fetch_without_order_by() {
        var context = ParseContext.of(new SqlServerSpecs());
        var result = context.parse(Query.class, "SELECT [u].[id] FROM [users] AS [u] OFFSET 5 ROWS FETCH NEXT 10 ROWS ONLY");

        assertTrue(result.isError());
    }

    @Test
    void parses_top_percent() {
        var context = ParseContext.of(new SqlServerSpecs());
        var result = context.parse(SelectQuery.class, "SELECT TOP (10) PERCENT [u].[id] FROM [users] AS [u]");

        assertFalse(result.isError());
        assertTrue(result.value().topSpec().percent());
        assertFalse(result.value().topSpec().withTies());
    }

    @Test
    void parses_top_with_ties() {
        var context = ParseContext.of(new SqlServerSpecs());
        var result = context.parse(SelectQuery.class, "SELECT TOP (10) WITH TIES [u].[id] FROM [users] AS [u] ORDER BY [u].[id]");

        assertFalse(result.isError());
        assertFalse(result.value().topSpec().percent());
        assertTrue(result.value().topSpec().withTies());
    }

    @Test
    void rejects_top_with_ties_without_order_by() {
        var context = ParseContext.of(new SqlServerSpecs());
        var result = context.parse(Query.class, "SELECT TOP (10) WITH TIES [u].[id] FROM [users] AS [u]");

        assertTrue(result.isError());
    }

    @Test
    void rejects_top_combined_with_offset_fetch() {
        var context = ParseContext.of(new SqlServerSpecs());
        var result = context.parse(Query.class, "SELECT TOP (10) [u].[id] FROM [users] AS [u] ORDER BY [u].[id] OFFSET 5 ROWS");

        assertTrue(result.isError());
    }

    @Test
    void parses_top_without_parentheses() {
        var context = ParseContext.of(new SqlServerSpecs());
        var result = context.parse(SelectQuery.class, "SELECT TOP 10 [u].[id] FROM [users] AS [u]");

        assertFalse(result.isError());
        assertNotNull(result.value().topSpec());
        assertEquals(10L, result.value().topSpec().count().matchExpression().literal(l -> l.value()).orElse(null));
    }

    @Test
    void parses_fetch_first_variant() {
        var context = ParseContext.of(new SqlServerSpecs());
        var result = context.parse(Query.class, "SELECT [u].[id] FROM [users] AS [u] ORDER BY [u].[id] OFFSET 5 ROWS FETCH FIRST 10 ROWS ONLY");

        assertFalse(result.isError());
    }

    @Test
    void rejects_missing_top_closing_parenthesis() {
        var context = ParseContext.of(new SqlServerSpecs());
        var result = context.parse(Query.class, "SELECT TOP (10 [u].[id] FROM [users] AS [u]");

        assertTrue(result.isError());
    }

    @Test
    void parses_sqlServerTableHintsOnFromTable() {
        var context = ParseContext.of(new SqlServerSpecs());
        var result = context.parse(SelectQuery.class, "SELECT [u].[id] FROM [users] AS [u] WITH (UPDLOCK, HOLDLOCK)");

        assertFalse(result.isError(), result.errorMessage());
        assertEquals(2, result.value().from().matchTableRef().table(t -> t.hints().size()).orElseThrow(AssertionError::new));
        assertEquals(
            "UPDLOCK",
            result.value().from().matchTableRef().table(t -> t.hints().getFirst().name().value()).orElseThrow(AssertionError::new)
        );
    }

    @Test
    void parses_crossApply_asLateralCrossJoin() {
        var context = ParseContext.of(new SqlServerSpecs());
        var result = context.parse(
            SelectQuery.class,
            "SELECT [u].[id] FROM [users] AS [u] CROSS APPLY (SELECT [id] FROM [users]) AS [sq]"
        );

        assertFalse(result.isError(), result.errorMessage());
        assertEquals(1, result.value().joins().size());
        assertInstanceOf(CrossJoin.class, result.value().joins().getFirst());
        assertInstanceOf(Lateral.class, result.value().joins().getFirst().right());
    }

    @Test
    void parses_outerApply_asLeftLateralJoin() {
        var context = ParseContext.of(new SqlServerSpecs());
        var result = context.parse(
            SelectQuery.class,
            "SELECT [u].[id] FROM [users] AS [u] OUTER APPLY (SELECT [id] FROM [users]) AS [sq]"
        );

        assertFalse(result.isError(), result.errorMessage());
        assertEquals(1, result.value().joins().size());
        assertInstanceOf(OnJoin.class, result.value().joins().getFirst());
        assertEquals(JoinKind.LEFT, ((io.sqm.core.OnJoin) result.value().joins().getFirst()).kind());
        assertInstanceOf(Lateral.class, result.value().joins().getFirst().right());
    }

    @Test
    void parses_regularJoinThroughSqlServerJoinParserFallback() {
        var context = ParseContext.of(new SqlServerSpecs());
        var result = context.parse(
            SelectQuery.class,
            "SELECT [u].[id] FROM [users] AS [u] INNER JOIN [orders] AS [o] ON [u].[id] = [o].[user_id]"
        );

        assertFalse(result.isError(), result.errorMessage());
        assertEquals(1, result.value().joins().size());
        assertInstanceOf(OnJoin.class, result.value().joins().getFirst());
        assertEquals(JoinKind.INNER, ((OnJoin) result.value().joins().getFirst()).kind());
    }

    @Test
    void rejects_crossApply_without_table_reference() {
        var context = ParseContext.of(new SqlServerSpecs());
        var result = context.parse(
            Query.class,
            "SELECT [u].[id] FROM [users] AS [u] CROSS APPLY"
        );

        assertTrue(result.isError());
    }

    @Test
    void rejects_outerApply_without_table_reference() {
        var context = ParseContext.of(new SqlServerSpecs());
        var result = context.parse(
            Query.class,
            "SELECT [u].[id] FROM [users] AS [u] OUTER APPLY"
        );

        assertTrue(result.isError());
    }

    @Test
    void rejects_conflicting_sqlServerTableHints() {
        var context = ParseContext.of(new SqlServerSpecs());
        var result = context.parse(Query.class, "SELECT [u].[id] FROM [users] AS [u] WITH (NOLOCK, UPDLOCK)");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("NOLOCK"));
    }
}
