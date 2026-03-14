package io.sqm.parser.sqlserver;

import io.sqm.core.Query;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.sqlserver.spi.SqlServerSpecs;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlServerSelectQueryParserTest {

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
    void rejects_top_percent() {
        var context = ParseContext.of(new SqlServerSpecs());
        var result = context.parse(Query.class, "SELECT TOP (10) PERCENT [u].[id] FROM [users] AS [u]");

        assertTrue(result.isError());
    }

    @Test
    void rejects_top_with_ties() {
        var context = ParseContext.of(new SqlServerSpecs());
        var result = context.parse(Query.class, "SELECT TOP (10) WITH TIES [u].[id] FROM [users] AS [u] ORDER BY [u].[id]");

        assertTrue(result.isError());
    }

    @Test
    void rejects_top_combined_with_offset_fetch() {
        var context = ParseContext.of(new SqlServerSpecs());
        var result = context.parse(Query.class, "SELECT TOP (10) [u].[id] FROM [users] AS [u] ORDER BY [u].[id] OFFSET 5 ROWS");

        assertTrue(result.isError());
    }
}
