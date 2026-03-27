package io.sqm.it;

import io.sqm.core.Query;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.sqlserver.spi.SqlServerSpecs;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class SqlServerRoundTripIntegrationTest {

    @Test
    void roundTrip_top_query_with_bracket_identifiers() {
        String sql = "SELECT TOP (5) [u].[id], [u].[name] FROM [users] AS [u] ORDER BY [u].[id]";

        Query parsed = Utils.parseSqlServer(sql);
        String rendered = Utils.renderSqlServer(parsed);
        Query reparsed = Utils.parseSqlServer(rendered);

        assertEquals(Utils.canonicalJson(parsed), Utils.canonicalJson(reparsed));
        assertEquals(
            "SELECT TOP (5) [u].[id], [u].[name] FROM [users] AS [u] ORDER BY [u].[id]",
            Utils.normalizeSql(rendered)
        );
    }

    @Test
    void roundTrip_offsetFetchQuery() {
        String sql = """
            SELECT [u].[id], [u].[name]
            FROM [users] AS [u]
            ORDER BY [u].[name]
            OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY
            """.trim();

        Query parsed = Utils.parseSqlServer(sql);
        String rendered = Utils.renderSqlServer(parsed);
        Query reparsed = Utils.parseSqlServer(rendered);

        assertEquals(Utils.canonicalJson(parsed), Utils.canonicalJson(reparsed));
        assertEquals(
            "SELECT [u].[id], [u].[name] FROM [users] AS [u] ORDER BY [u].[name] OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY",
            Utils.normalizeSql(rendered)
        );
    }

    @Test
    void roundTrip_sqlserver_functions_and_bit_boolean_rendering() {
        String sql = """
            SELECT LEN([name]) AS [name_len],
                   DATEADD(DAY, 1, [created_at]) AS [next_day],
                   ISNULL([nickname], 'n/a') AS [screen_name]
            FROM [users]
            WHERE [active] = 1
            """.trim();

        Query parsed = Utils.parseSqlServer(sql);
        String rendered = Utils.renderSqlServer(parsed);
        Query reparsed = Utils.parseSqlServer(rendered);

        assertEquals(Utils.canonicalJson(parsed), Utils.canonicalJson(reparsed));
        assertEquals(
            "SELECT LEN([name]) AS [name_len], DATEADD(DAY, 1, [created_at]) AS [next_day], "
                + "ISNULL([nickname], 'n/a') AS [screen_name] FROM [users] WHERE [active] = 1",
            Utils.normalizeSql(rendered)
        );
    }

    @Test
    void roundTrip_atTimeZone_query() {
        String sql = """
            SELECT [u].[created_at] AT TIME ZONE 'UTC' AS [created_utc]
            FROM [users] AS [u]
            """.trim();

        Query parsed = Utils.parseSqlServer(sql);
        String rendered = Utils.renderSqlServer(parsed);
        Query reparsed = Utils.parseSqlServer(rendered);

        assertEquals(Utils.canonicalJson(parsed), Utils.canonicalJson(reparsed));
        assertEquals(
            "SELECT [u].[created_at] AT TIME ZONE 'UTC' AS [created_utc] FROM [users] AS [u]",
            Utils.normalizeSql(rendered)
        );
    }

    @Test
    void roundTrip_crossApply_query() {
        String sql = """
            SELECT [u].[id]
            FROM [users] AS [u]
            CROSS APPLY (SELECT [id] FROM [users]) AS [sq]
            """.trim();

        Query parsed = Utils.parseSqlServer(sql);
        String rendered = Utils.renderSqlServer(parsed);
        Query reparsed = Utils.parseSqlServer(rendered);

        assertEquals(Utils.canonicalJson(parsed), Utils.canonicalJson(reparsed));
        assertEquals(
            "SELECT [u].[id] FROM [users] AS [u] CROSS APPLY ( SELECT [id] FROM [users] ) AS [sq]",
            Utils.normalizeSql(rendered)
        );
    }

    @Test
    void roundTrip_outerApply_query() {
        String sql = """
            SELECT [u].[id]
            FROM [users] AS [u]
            OUTER APPLY (SELECT TOP (1) [id] FROM [users]) AS [sq]
            """.trim();

        Query parsed = Utils.parseSqlServer(sql);
        String rendered = Utils.renderSqlServer(parsed);
        Query reparsed = Utils.parseSqlServer(rendered);

        assertEquals(Utils.canonicalJson(parsed), Utils.canonicalJson(reparsed));
        assertEquals(
            "SELECT [u].[id] FROM [users] AS [u] OUTER APPLY ( SELECT TOP (1) [id] FROM [users] ) AS [sq]",
            Utils.normalizeSql(rendered)
        );
    }

    @Test
    void parser_accepts_top_percent() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(Query.class, "SELECT TOP 10 PERCENT [id] FROM [users]");

        assertFalse(result.isError());
    }

    @Test
    void roundTrip_top_percent_query_with_table_hints() {
        String sql = """
            SELECT TOP (10) PERCENT [u].[id]
            FROM [users] AS [u] WITH (NOLOCK)
            ORDER BY [u].[id]
            """.trim();

        Query parsed = Utils.parseSqlServer(sql);
        String rendered = Utils.renderSqlServer(parsed);
        Query reparsed = Utils.parseSqlServer(rendered);

        assertEquals(Utils.canonicalJson(parsed), Utils.canonicalJson(reparsed));
        assertEquals(
            "SELECT TOP (10) PERCENT [u].[id] FROM [users] AS [u] WITH (NOLOCK) ORDER BY [u].[id]",
            Utils.normalizeSql(rendered)
        );
    }

    @Test
    void roundTrip_top_with_ties_query() {
        String sql = """
            SELECT TOP (5) WITH TIES [u].[id]
            FROM [users] AS [u]
            ORDER BY [u].[id]
            """.trim();

        Query parsed = Utils.parseSqlServer(sql);
        String rendered = Utils.renderSqlServer(parsed);
        Query reparsed = Utils.parseSqlServer(rendered);

        assertEquals(Utils.canonicalJson(parsed), Utils.canonicalJson(reparsed));
        assertEquals(
            "SELECT TOP (5) WITH TIES [u].[id] FROM [users] AS [u] ORDER BY [u].[id]",
            Utils.normalizeSql(rendered)
        );
    }

    @Test
    void parser_rejects_offsetFetch_without_orderBy() {
        var ctx = ParseContext.of(new SqlServerSpecs());
        var result = ctx.parse(Query.class, "SELECT [id] FROM [users] OFFSET 5 ROWS FETCH NEXT 10 ROWS ONLY");

        assertTrue(result.isError());
        assertTrue(Objects.requireNonNull(result.errorMessage()).contains("ORDER BY"));
    }
}
