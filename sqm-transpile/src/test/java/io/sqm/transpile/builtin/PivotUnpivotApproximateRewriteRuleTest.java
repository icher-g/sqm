package io.sqm.transpile.builtin;

import io.sqm.core.PivotTable;
import io.sqm.core.Query;
import io.sqm.core.QueryTable;
import io.sqm.core.SelectQuery;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.parser.oracle.spi.OracleSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.transpile.*;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PivotUnpivotApproximateRewriteRuleTest {
    private static SelectQuery parseOracle(String sql) {
        var result = ParseContext.of(new OracleSpecs()).parse(Query.class, sql);
        assertTrue(result.ok(), result.errorMessage());
        return assertInstanceOf(SelectQuery.class, result.value());
    }

    private static TranspileContext context(SqlDialectId source, SqlDialectId target) {
        return new TranspileContext(source, target, TranspileOptions.defaults(), Optional.empty(), Optional.empty());
    }

    private static SqlTranspiler postgresTranspiler(SqlDialectId sourceDialect) {
        return new DefaultSqlTranspiler.Builder()
            .sourceDialect(sourceDialect)
            .targetDialect(SqlDialectId.POSTGRESQL)
            .options(new TranspileOptions(true, false, true, true))
            .build();
    }

    private static void assertContains(String actual, String expectedPart) {
        assertTrue(actual.contains(normalizeSql(expectedPart)), () -> "Expected SQL to contain:\n" + normalizeSql(expectedPart) + "\nActual SQL:\n" + actual);
    }

    private static String normalizeSql(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    @Test
    void exposesSupportedDialectPairs() {
        var rule = new PivotUnpivotApproximateRewriteRule();

        assertEquals("pivot-unpivot-approximate-rewrite", rule.id());
        assertEquals(Set.of(SqlDialectId.ORACLE, SqlDialectId.SQLSERVER), rule.sourceDialects());
        assertEquals(Set.of(SqlDialectId.ANSI, SqlDialectId.MYSQL, SqlDialectId.POSTGRESQL), rule.targetDialects());
        assertTrue(rule.order() < 0);
    }

    @Test
    void rewritesPivotToConditionalAggregationAst() {
        var query = parseOracle("""
            SELECT region, q1
            FROM sales
            PIVOT (sum(amount) FOR quarter IN ('Q1' AS q1))
            """);

        var result = new PivotUnpivotApproximateRewriteRule()
            .apply(query, context(SqlDialectId.ORACLE, SqlDialectId.POSTGRESQL));

        assertTrue(result.changed());
        assertEquals(RewriteFidelity.APPROXIMATE, result.fidelity());
        assertEquals("APPROXIMATE_PIVOT_UNPIVOT_REWRITE", result.warnings().getFirst().code());
        var rewritten = assertInstanceOf(SelectQuery.class, result.statement());
        assertFalse(rewritten.from() instanceof PivotTable);
        var rewrittenFrom = assertInstanceOf(QueryTable.class, rewritten.from());
        var core = assertInstanceOf(SelectQuery.class, rewrittenFrom.query());
        assertNotNull(core.groupBy());
        assertFalse(StatementFeatureInspector.hasPivotOrUnpivotTable(rewritten));
    }

    @Test
    void transpilerRendersApproximatePivotWhenEnabled() {
        var transpiler = new DefaultSqlTranspiler.Builder()
            .sourceDialect(SqlDialectId.ORACLE)
            .targetDialect(SqlDialectId.POSTGRESQL)
            .options(new TranspileOptions(true, false, true, true))
            .build();

        var result = transpiler.transpile("""
            SELECT region, q1
            FROM sales
            PIVOT (sum(amount) FOR quarter IN ('Q1' AS q1))
            """);

        assertEquals(TranspileStatus.SUCCESS_WITH_WARNINGS, result.status());
        assertEquals("APPROXIMATE_PIVOT_UNPIVOT_REWRITE", result.warnings().getFirst().code());
        var sql = normalizeSql(result.sql().orElseThrow());
        assertContains(sql, "SELECT region, q1 FROM (");
        assertContains(sql, "sum(CASE WHEN quarter = 'Q1' THEN amount ELSE NULL END) AS q1");
        assertContains(sql, "FROM sales GROUP BY region");
    }

    @Test
    void transpilerRendersSqlServerPivotValuesAsComparisonLiterals() {
        var transpiler = new DefaultSqlTranspiler.Builder()
            .sourceDialect(SqlDialectId.SQLSERVER)
            .targetDialect(SqlDialectId.POSTGRESQL)
            .options(new TranspileOptions(true, false, true, true))
            .build();

        var result = transpiler.transpile("""
            SELECT region, [Q1]
            FROM sales
            PIVOT (sum(amount) FOR quarter IN ([Q1])) p
            """);

        assertEquals(TranspileStatus.SUCCESS_WITH_WARNINGS, result.status());
        var sql = normalizeSql(result.sql().orElseThrow());
        assertContains(sql, "SELECT region, \"Q1\" FROM (");
        assertContains(sql, "sum(CASE WHEN quarter = 'Q1' THEN amount ELSE NULL END) AS q1");
        assertContains(sql, "FROM sales GROUP BY region");
    }

    @Test
    void transpilerRejectsApproximatePivotWhenDisabled() {
        var transpiler = new DefaultSqlTranspiler.Builder()
            .sourceDialect(SqlDialectId.ORACLE)
            .targetDialect(SqlDialectId.POSTGRESQL)
            .build();

        var result = transpiler.transpile("""
            SELECT region, q1
            FROM sales
            PIVOT (sum(amount) FOR quarter IN ('Q1' AS q1))
            """);

        assertEquals(TranspileStatus.UNSUPPORTED, result.status());
        assertEquals("APPROXIMATE_REWRITE_DISABLED", result.problems().getLast().code());
        assertTrue(result.sql().isEmpty());
    }

    @Test
    void transpilerRendersApproximateUnpivotWhenEnabled() {
        var transpiler = new DefaultSqlTranspiler.Builder()
            .sourceDialect(SqlDialectId.ORACLE)
            .targetDialect(SqlDialectId.POSTGRESQL)
            .options(new TranspileOptions(true, false, true, true))
            .build();

        var result = transpiler.transpile("""
            SELECT region, amount, quarter
            FROM sales
            UNPIVOT (amount FOR quarter IN (q1 AS 'Q1', q2 AS 'Q2'))
            """);

        assertEquals(TranspileStatus.SUCCESS_WITH_WARNINGS, result.status());
        var sql = normalizeSql(result.sql().orElseThrow());
        assertTrue(sql.contains("SELECT region, q1 AS amount, 'Q1' AS quarter FROM sales WHERE q1 IS NOT NULL"));
        assertTrue(sql.contains("UNION ALL"));
        assertTrue(sql.contains("SELECT region, q2 AS amount, 'Q2' AS quarter FROM sales WHERE q2 IS NOT NULL"));
    }

    @Test
    void rewritesIncludeNullsUnpivotWithoutBranchFilter() {
        var transpiler = new DefaultSqlTranspiler.Builder()
            .sourceDialect(SqlDialectId.ORACLE)
            .targetDialect(SqlDialectId.POSTGRESQL)
            .options(new TranspileOptions(true, false, true, true))
            .build();

        var result = transpiler.transpile("""
            SELECT region, amount, quarter
            FROM sales
            UNPIVOT INCLUDE NULLS (amount FOR quarter IN (q1 AS 'Q1'))
            """);

        assertEquals(TranspileStatus.SUCCESS_WITH_WARNINGS, result.status());
        assertFalse(normalizeSql(result.sql().orElseThrow()).contains("IS NOT NULL"));
    }

    @Test
    void transpilerRendersPivotThroughDerivedTableWhenEnabled() {
        var result = postgresTranspiler(SqlDialectId.ORACLE).transpile("""
            SELECT region, q1
            FROM sales
            PIVOT (sum(amount) FOR quarter IN ('Q1' AS q1))
            """);

        assertEquals(TranspileStatus.SUCCESS_WITH_WARNINGS, result.status());
        var sql = normalizeSql(result.sql().orElseThrow());
        assertContains(sql, "SELECT region, q1 FROM ( SELECT region AS region, sum(CASE WHEN quarter = 'Q1' THEN amount ELSE NULL END) AS q1 FROM sales GROUP BY region )");
    }

    @Test
    void transpilerRendersMultiMeasurePivot() {
        var result = postgresTranspiler(SqlDialectId.ORACLE).transpile("""
            SELECT region, q1_total, q1_cnt, q2_total, q2_cnt
            FROM sales
            PIVOT (
                sum(amount) AS total,
                count(*) AS cnt
                FOR quarter IN ('Q1' AS q1, 'Q2' AS q2)
            )
            """);

        assertEquals(TranspileStatus.SUCCESS_WITH_WARNINGS, result.status());
        var sql = normalizeSql(result.sql().orElseThrow());
        assertContains(sql, "sum(CASE WHEN quarter = 'Q1' THEN amount ELSE NULL END) AS q1_total");
        assertContains(sql, "count(CASE WHEN quarter = 'Q1' THEN 1 ELSE NULL END) AS q1_cnt");
        assertContains(sql, "sum(CASE WHEN quarter = 'Q2' THEN amount ELSE NULL END) AS q2_total");
        assertContains(sql, "count(CASE WHEN quarter = 'Q2' THEN 1 ELSE NULL END) AS q2_cnt");
        assertContains(sql, "GROUP BY region");
    }

    @Test
    void transpilerPreservesOuterWhereAfterPivot() {
        var result = postgresTranspiler(SqlDialectId.ORACLE).transpile("""
            SELECT region, q1
            FROM sales
            PIVOT (sum(amount) FOR quarter IN ('Q1' AS q1))
            WHERE q1 > 100
            """);

        assertEquals(TranspileStatus.SUCCESS_WITH_WARNINGS, result.status());
        var sql = normalizeSql(result.sql().orElseThrow());
        assertContains(sql, "FROM ( SELECT region AS region, sum(CASE WHEN quarter = 'Q1' THEN amount ELSE NULL END) AS q1 FROM sales GROUP BY region )");
        assertContains(sql, "WHERE q1 > 100");
    }

    @Test
    void transpilerPreservesOuterJoinAroundPivot() {
        var result = postgresTranspiler(SqlDialectId.ORACLE).transpile("""
            SELECT p.region, p.q1, m.manager_name
            FROM sales
            PIVOT (sum(amount) FOR quarter IN ('Q1' AS q1)) p
            JOIN managers m ON m.region = p.region
            WHERE m.active = 1
            ORDER BY p.region
            """);

        assertEquals(TranspileStatus.SUCCESS_WITH_WARNINGS, result.status());
        var sql = normalizeSql(result.sql().orElseThrow());
        assertContains(sql, "FROM ( SELECT region AS region, sum(CASE WHEN quarter = 'Q1' THEN amount ELSE NULL END) AS q1 FROM sales GROUP BY region )");
        assertTrue(sql.contains(" AS p") || sql.contains(" p"), sql);
        assertContains(sql, "JOIN managers AS m ON m.region = p.region");
        assertContains(sql, "WHERE m.active = 1");
        assertContains(sql, "ORDER BY p.region");
    }

    @Test
    void transpilerPreservesOuterGroupByAndHavingAfterPivot() {
        var result = postgresTranspiler(SqlDialectId.ORACLE).transpile("""
            SELECT country, sum(q1) AS total_q1
            FROM (
                SELECT s.country, s.region, s.quarter, s.amount
                FROM sales s
            )
            PIVOT (sum(amount) FOR quarter IN ('Q1' AS q1))
            GROUP BY country
            HAVING sum(q1) > 100
            ORDER BY country
            """);

        assertEquals(TranspileStatus.SUCCESS_WITH_WARNINGS, result.status());
        var sql = normalizeSql(result.sql().orElseThrow());
        assertContains(sql, "SELECT country, sum(q1) AS total_q1 FROM (");
        assertContains(sql, "SELECT country AS country, region AS region, sum(CASE WHEN quarter = 'Q1' THEN amount ELSE NULL END) AS q1");
        assertContains(sql, "GROUP BY country, region");
        assertContains(sql, "GROUP BY country HAVING sum(q1) > 100 ORDER BY country");
    }

    @Test
    void transpilerPreservesOrderByAndFetchAfterPivot() {
        var result = postgresTranspiler(SqlDialectId.ORACLE).transpile("""
            SELECT region, q1
            FROM sales
            PIVOT (sum(amount) FOR quarter IN ('Q1' AS q1))
            ORDER BY q1 DESC
            FETCH FIRST 10 ROWS ONLY
            """);

        assertEquals(TranspileStatus.SUCCESS_WITH_WARNINGS, result.status());
        var sql = normalizeSql(result.sql().orElseThrow());
        assertContains(sql, "ORDER BY q1 DESC");
        assertTrue(sql.contains("LIMIT 10") || sql.contains("FETCH FIRST 10 ROWS ONLY"), sql);
    }

    @Test
    void transpilerPreservesWindowExpressionAfterPivot() {
        var result = postgresTranspiler(SqlDialectId.ORACLE).transpile("""
            SELECT region,
                   q1,
                   row_number() OVER (ORDER BY q1 DESC) AS rn
            FROM sales
            PIVOT (sum(amount) FOR quarter IN ('Q1' AS q1))
            ORDER BY rn
            """);

        assertEquals(TranspileStatus.SUCCESS_WITH_WARNINGS, result.status());
        var sql = normalizeSql(result.sql().orElseThrow());
        assertContains(sql, "row_number() OVER (ORDER BY q1 DESC) AS rn");
        assertContains(sql, "ORDER BY rn");
    }

    @Test
    void transpilerPreservesSourceQueryClausesBeforePivot() {
        var result = postgresTranspiler(SqlDialectId.ORACLE).transpile("""
            SELECT region, q1
            FROM (
                SELECT s.region, s.quarter, sum(s.amount) AS amount
                FROM sales s
                JOIN regions r ON r.id = s.region_id
                WHERE s.amount > 0
                GROUP BY s.region, s.quarter
                HAVING sum(s.amount) > 10
            )
            PIVOT (sum(amount) FOR quarter IN ('Q1' AS q1))
            ORDER BY region
            """);

        assertEquals(TranspileStatus.SUCCESS_WITH_WARNINGS, result.status());
        var sql = normalizeSql(result.sql().orElseThrow());
        assertContains(sql, "JOIN regions AS r ON r.id = s.region_id");
        assertContains(sql, "WHERE s.amount > 0");
        assertContains(sql, "GROUP BY s.region, s.quarter HAVING sum(s.amount) > 10");
        assertContains(sql, "sum(CASE WHEN quarter = 'Q1' THEN amount ELSE NULL END) AS q1");
        assertContains(sql, "ORDER BY region");
    }

    @Test
    void transpilerPreservesOuterWhereAndOrderByAfterUnpivot() {
        var result = postgresTranspiler(SqlDialectId.ORACLE).transpile("""
            SELECT region, amount, quarter
            FROM sales
            UNPIVOT (amount FOR quarter IN (q1 AS 'Q1', q2 AS 'Q2'))
            WHERE amount > 0
            ORDER BY region, quarter
            """);

        assertEquals(TranspileStatus.SUCCESS_WITH_WARNINGS, result.status());
        var sql = normalizeSql(result.sql().orElseThrow());
        assertContains(sql, "UNION ALL");
        assertContains(sql, "WHERE amount > 0");
        assertContains(sql, "ORDER BY region, quarter");
    }

    @Test
    void rejectsAmbiguousPivotProjection() {
        var query = parseOracle("""
            SELECT *
            FROM sales
            PIVOT (sum(amount) FOR quarter IN ('Q1' AS q1))
            """);

        var result = new PivotUnpivotApproximateRewriteRule()
            .apply(query, context(SqlDialectId.ORACLE, SqlDialectId.POSTGRESQL));

        assertFalse(result.changed());
        assertEquals(RewriteFidelity.UNSUPPORTED, result.fidelity());
        assertEquals("UNSUPPORTED_PIVOT_UNPIVOT_REWRITE", result.problems().getFirst().code());
        assertTrue(result.problems().getFirst().message().contains("cannot infer implicit grouping columns"));
    }

    @Test
    void rewritesNestedPivotTableTransforms() {
        var inner = parseOracle("""
            SELECT region, q1
            FROM sales
            PIVOT (sum(amount) FOR quarter IN ('Q1' AS q1))
            """);
        var outer = io.sqm.dsl.Dsl.select(io.sqm.core.Expression.subquery(inner)).build();

        var result = new PivotUnpivotApproximateRewriteRule()
            .apply(outer, context(SqlDialectId.ORACLE, SqlDialectId.POSTGRESQL));

        assertTrue(result.changed());
        assertEquals(RewriteFidelity.APPROXIMATE, result.fidelity());
        assertTrue(result.problems().isEmpty());
        assertFalse(StatementFeatureInspector.hasPivotOrUnpivotTable(result.statement()));
    }
}
