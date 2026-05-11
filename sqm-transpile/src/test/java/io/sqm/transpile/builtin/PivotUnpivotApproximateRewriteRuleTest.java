package io.sqm.transpile.builtin;

import io.sqm.core.PivotTable;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.parser.oracle.spi.OracleSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.transpile.DefaultSqlTranspiler;
import io.sqm.transpile.RewriteFidelity;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileOptions;
import io.sqm.transpile.TranspileStatus;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PivotUnpivotApproximateRewriteRuleTest {
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
        assertNotNull(rewritten.groupBy());
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
        assertEquals(
            normalizeSql("SELECT region, sum(CASE WHEN quarter = 'Q1' THEN amount ELSE NULL END) AS q1 FROM sales GROUP BY region"),
            normalizeSql(result.sql().orElseThrow())
        );
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
        assertEquals(
            normalizeSql("SELECT region, sum(CASE WHEN quarter = 'Q1' THEN amount ELSE NULL END) AS q1 FROM sales GROUP BY region"),
            normalizeSql(result.sql().orElseThrow())
        );
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
        assertTrue(result.problems().getFirst().message().contains("explicit expression SELECT items"));
    }

    @Test
    void rejectsNestedPivotTableTransforms() {
        var inner = parseOracle("""
            SELECT region, q1
            FROM sales
            PIVOT (sum(amount) FOR quarter IN ('Q1' AS q1))
            """);
        var outer = io.sqm.dsl.Dsl.select(io.sqm.core.Expression.subquery(inner)).build();

        var result = new PivotUnpivotApproximateRewriteRule()
            .apply(outer, context(SqlDialectId.ORACLE, SqlDialectId.POSTGRESQL));

        assertFalse(result.changed());
        assertEquals(RewriteFidelity.UNSUPPORTED, result.fidelity());
        assertTrue(result.problems().getFirst().message().contains("Nested PIVOT/UNPIVOT"));
    }

    private static SelectQuery parseOracle(String sql) {
        var result = ParseContext.of(new OracleSpecs()).parse(Query.class, sql);
        assertTrue(result.ok(), result.errorMessage());
        return assertInstanceOf(SelectQuery.class, result.value());
    }

    private static TranspileContext context(SqlDialectId source, SqlDialectId target) {
        return new TranspileContext(source, target, TranspileOptions.defaults(), Optional.empty(), Optional.empty());
    }

    private static String normalizeSql(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }
}
