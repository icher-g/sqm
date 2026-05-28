package io.sqm.transpile.builtin;

import io.sqm.core.*;
import io.sqm.core.transform.RecursiveNodeTransformer;
import io.sqm.core.walk.NodeVisitor;
import io.sqm.core.dialect.SqlDialectId;
import io.sqm.parser.oracle.spi.OracleSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.transpile.DefaultSqlTranspiler;
import io.sqm.transpile.RewriteFidelity;
import io.sqm.transpile.SqlTranspiler;
import io.sqm.transpile.TranspileContext;
import io.sqm.transpile.TranspileOptions;
import io.sqm.transpile.TranspileStatus;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class OracleToSqlServerPivotUnpivotRuleTest {
    private static SelectQuery parseOracle(String sql) {
        var result = ParseContext.of(new OracleSpecs()).parse(Query.class, sql);
        assertTrue(result.ok(), result.errorMessage());
        return assertInstanceOf(SelectQuery.class, result.value());
    }

    private static TranspileContext context() {
        return new TranspileContext(
            SqlDialectId.ORACLE,
            SqlDialectId.SQLSERVER,
            TranspileOptions.defaults(),
            Optional.empty(),
            Optional.empty()
        );
    }

    private static SqlTranspiler oracleToSqlServer() {
        return new DefaultSqlTranspiler.Builder()
            .sourceDialect(SqlDialectId.ORACLE)
            .targetDialect(SqlDialectId.SQLSERVER)
            .build();
    }

    private static SqlTranspiler oracleToSqlServerWithApproximateRewrites() {
        return new DefaultSqlTranspiler.Builder()
            .sourceDialect(SqlDialectId.ORACLE)
            .targetDialect(SqlDialectId.SQLSERVER)
            .options(new TranspileOptions(true, false, true, true))
            .build();
    }

    private static void assertContains(String actual, String expectedPart) {
        assertTrue(
            actual.contains(normalizeSql(expectedPart)),
            () -> "Expected SQL to contain:\n" + normalizeSql(expectedPart) + "\nActual SQL:\n" + actual
        );
    }

    private static String normalizeSql(String sql) {
        return sql.replaceAll("\\s+", " ").trim();
    }

    private static SelectQuery multiColumnUnpivotQuery(TableRef source, SelectItem... items) {
        var unpivot = UnpivotTable.of(
            source,
            java.util.List.of(id("amount"), id("quantity")),
            id("quarter"),
            java.util.List.of(unpivotInput(java.util.List.of(id("q1_amount"), id("q1_quantity")), lit("Q1"))),
            UnpivotTable.NullTreatment.EXCLUDE_NULLS
        );
        return SelectQuery.builder()
            .select(java.util.List.of(items))
            .from(unpivot)
            .build();
    }

    @Test
    void exposesSupportedDialectPair() {
        var rule = new OracleToSqlServerPivotUnpivotRule();

        assertEquals("oracle-to-sqlserver-pivot-unpivot-rule", rule.id());
        assertEquals(Set.of(SqlDialectId.ORACLE), rule.sourceDialects());
        assertEquals(Set.of(SqlDialectId.SQLSERVER), rule.targetDialects());
        assertTrue(rule.order() < 0);
    }

    @Test
    void rewritesOraclePivotAliasesToSqlServerPivotShape() {
        var query = parseOracle("""
            SELECT region, q1, q2
            FROM sales
            PIVOT (sum(amount) FOR quarter IN ('Q1' AS q1, 'Q2' AS q2))
            """);

        var result = new OracleToSqlServerPivotUnpivotRule().apply(query, context());

        assertTrue(result.changed());
        assertEquals(RewriteFidelity.EXACT, result.fidelity());
        var rewritten = assertInstanceOf(SelectQuery.class, result.statement());
        var pivot = assertInstanceOf(PivotTable.class, rewritten.from());
        assertTrue(pivot.values().stream().allMatch(value -> value.alias() == null));
        assertEquals("Q1", pivot.values().get(0).value().matchExpression().column(c -> c.name().value()).orElse(null));
        assertEquals("Q2", pivot.values().get(1).value().matchExpression().column(c -> c.name().value()).orElse(null));
    }

    @Test
    void rewritesOraclePivotWhileLeavingNonMappedSelectItemsAlone() {
        var query = SelectQuery.builder()
            .select(java.util.List.of(
                SelectItem.star(),
                func("coalesce", col("region"), lit("unknown")).toSelectItem(),
                col("q1").toSelectItem()
            ))
            .from(pivot(
                tbl("sales"),
                java.util.List.of(pivotMeasure(func("sum", col("amount")))),
                col("quarter"),
                pivotValue(lit("Q1"), "q1")
            ))
            .build();

        var result = new OracleToSqlServerPivotUnpivotRule().apply(query, context());

        assertTrue(result.changed());
        var rewritten = assertInstanceOf(SelectQuery.class, result.statement());
        assertInstanceOf(StarSelectItem.class, rewritten.items().get(0));
        assertSame(query.items().get(1), rewritten.items().get(1));
    }

    @Test
    void transpilerRendersOraclePivotAsSqlServerPivotWithProjectedAliases() {
        var result = oracleToSqlServer().transpile("""
            SELECT region, q1, q2
            FROM sales
            PIVOT (sum(amount) FOR quarter IN ('Q1' AS q1, 'Q2' AS q2))
            """);

        assertEquals(TranspileStatus.SUCCESS, result.status());
        assertTrue(result.steps().stream().anyMatch(step ->
            "oracle-to-sqlserver-pivot-unpivot-rule".equals(step.ruleId())
                && step.fidelity() == RewriteFidelity.EXACT
        ));
        var sql = normalizeSql(result.sql().orElseThrow());
        assertContains(sql, "SELECT region, [Q1] AS q1, [Q2] AS q2 FROM sales");
        assertContains(sql, "PIVOT ( sum(amount) FOR quarter IN ( [Q1], [Q2] ) )");
    }

    @Test
    void transpilerRendersQualifiedOraclePivotAliasAsSqlServerProjectionAlias() {
        var result = oracleToSqlServer().transpile("""
            SELECT p.region, p.q1
            FROM sales
            PIVOT (sum(amount) FOR quarter IN ('Q1' AS q1)) p
            """);

        assertEquals(TranspileStatus.SUCCESS, result.status());
        var sql = normalizeSql(result.sql().orElseThrow());
        assertContains(sql, "SELECT p.region, p.[Q1] AS q1 FROM sales");
        assertContains(sql, "PIVOT ( sum(amount) FOR quarter IN ( [Q1] ) ) AS p");
    }

    @Test
    void transpilerRendersSingleColumnOracleUnpivotAsSqlServerUnpivotWithLabelCase() {
        var result = oracleToSqlServer().transpile("""
            SELECT region, amount, quarter
            FROM sales
            UNPIVOT (amount FOR quarter IN (q1 AS 'Q1', q2 AS 'Q2'))
            """);

        assertEquals(TranspileStatus.SUCCESS, result.status());
        var sql = normalizeSql(result.sql().orElseThrow());
        assertContains(sql, "SELECT region, amount, CASE WHEN quarter = 'q1' THEN 'Q1' WHEN quarter = 'q2' THEN 'Q2' END AS quarter FROM sales");
        assertContains(sql, "UNPIVOT ( amount FOR quarter IN ( q1, q2 ) )");
    }

    @Test
    void transpilerRendersMultiColumnOracleUnpivotAsSqlServerCrossApply() {
        var result = oracleToSqlServerWithApproximateRewrites().transpile("""
            SELECT region, amount, quantity, quarter
            FROM sales
            UNPIVOT (
                (amount, quantity)
                FOR quarter IN (
                    (q1_amount, q1_quantity) AS 'Q1',
                    (q2_amount, q2_quantity) AS 'Q2'
                )
            )
            """);

        assertEquals(TranspileStatus.SUCCESS, result.status(), () -> result.problems().toString());
        assertTrue(result.steps().stream().anyMatch(step ->
            "oracle-to-sqlserver-pivot-unpivot-rule".equals(step.ruleId())
                && step.fidelity() == RewriteFidelity.APPROXIMATE
                && "UNPIVOT was rewritten approximately with CROSS APPLY".equals(step.description())
        ));
        var sql = normalizeSql(result.sql().orElseThrow());
        assertContains(sql, "SELECT s.region, upt.amount, upt.quantity, upt.quarter FROM sales AS s");
        assertContains(sql, "CROSS APPLY (VALUES (s.q1_amount, s.q1_quantity, 'Q1'), (s.q2_amount, s.q2_quantity, 'Q2')) AS upt(amount, quantity, quarter)");
        assertContains(sql, "WHERE upt.amount IS NOT NULL OR upt.quantity IS NOT NULL");
    }

    @Test
    void transpilerRendersMultiColumnIncludeNullsUnpivotWithoutCrossApplyFilter() {
        var result = oracleToSqlServerWithApproximateRewrites().transpile("""
            SELECT region, amount, quantity, quarter
            FROM sales upt
            UNPIVOT INCLUDE NULLS (
                (amount, quantity)
                FOR quarter IN ((q1_amount, q1_quantity) AS 'Q1')
            )
            """);

        assertEquals(TranspileStatus.SUCCESS, result.status(), () -> result.problems().toString());
        var sql = normalizeSql(result.sql().orElseThrow());
        assertContains(sql, "SELECT upt.region, upt0.amount, upt0.quantity, upt0.quarter FROM sales AS upt");
        assertContains(sql, "CROSS APPLY (VALUES (upt.q1_amount, upt.q1_quantity, 'Q1')) AS upt0(amount, quantity, quarter)");
        assertFalse(sql.contains("IS NOT NULL"), sql);
    }

    @Test
    void rewritesSingleColumnUnpivotWhileLeavingStarSelectItemsAlone() {
        var query = parseOracle("""
            SELECT *, amount, quarter
            FROM sales
            UNPIVOT (amount FOR quarter IN (q1 AS 'Q1'))
            """);

        var result = new OracleToSqlServerPivotUnpivotRule().apply(query, context());

        assertTrue(result.changed());
        var rewritten = assertInstanceOf(SelectQuery.class, result.statement());
        assertInstanceOf(StarSelectItem.class, rewritten.items().getFirst());
        assertInstanceOf(ExprSelectItem.class, rewritten.items().get(1));
        assertInstanceOf(ExprSelectItem.class, rewritten.items().get(2));
    }

    @Test
    void combinesExistingWhereWithGeneratedCrossApplyNullFilter() {
        var query = SelectQuery.builder()
            .select(col("region"), col("amount"), col("quarter"))
            .from(UnpivotTable.of(
                tbl("sales"),
                java.util.List.of(id("amount"), id("quantity")),
                id("quarter"),
                java.util.List.of(unpivotInput(java.util.List.of(id("q1_amount"), id("q1_quantity")), lit("Q1"))),
                UnpivotTable.NullTreatment.DIALECT_DEFAULT
            ))
            .where(col("active").eq(lit(1)))
            .build();

        var result = new OracleToSqlServerPivotUnpivotRule().apply(query, context());

        assertTrue(result.changed());
        var rewritten = assertInstanceOf(SelectQuery.class, result.statement());
        assertNotNull(rewritten.where());
        assertEquals(RewriteFidelity.APPROXIMATE, result.fidelity());
    }

    @Test
    void rewritesCrossApplyForFunctionSourceAndStarProjections() {
        var source = FunctionTable.of(func("sales_rows")).as("f");
        var query = multiColumnUnpivotQuery(
            source,
            SelectItem.star(),
            SelectItem.star(id("f")),
            new TestDialectSelectItem(),
            func("coalesce", col("region"), lit("unknown")).toSelectItem(),
            col("amount").toSelectItem(),
            col("quarter").toSelectItem()
        );

        var result = new OracleToSqlServerPivotUnpivotRule().apply(query, context());

        assertTrue(result.changed());
        assertEquals(RewriteFidelity.APPROXIMATE, result.fidelity());
        var rewritten = assertInstanceOf(SelectQuery.class, result.statement());
        assertEquals(source, rewritten.from());
        assertEquals(1, rewritten.joins().size());
        assertInstanceOf(QualifiedStarSelectItem.class, rewritten.items().get(0));
        assertInstanceOf(QualifiedStarSelectItem.class, rewritten.items().get(1));
        assertSame(query.items().get(2), rewritten.items().get(2));
        assertSame(query.items().get(3), rewritten.items().get(3));
    }

    @Test
    void rewritesCrossApplyForQueryValuesLateralPivotAndVariableSources() {
        var querySource = QueryTable.of(select(col("q1_amount"), col("q1_quantity")).from(tbl("base_sales")).build());
        var valuesSource = ValuesTable.of(RowListExpr.of(java.util.List.of(RowExpr.of(java.util.List.of(lit(1), lit(2))))));
        var lateralSource = Lateral.of(valuesSource);
        var pivotSource = pivot(
            tbl("sales"),
            java.util.List.of(pivotMeasure(func("sum", col("amount")))),
            col("quarter"),
            pivotValue(lit("Q1"), "q1")
        );
        var variableSource = VariableTable.of("audit_rows");
        var nestedUnpivotSource = unpivot(
            tbl("wide_sales"),
            "amount",
            "quarter",
            unpivotInput("q1_amount", lit("Q1"))
        ).as("inner_unpivot");

        for (var source : java.util.List.of(querySource, valuesSource, lateralSource, pivotSource, variableSource, nestedUnpivotSource)) {
            var query = multiColumnUnpivotQuery(
                source,
                col("region").toSelectItem(),
                col("amount").toSelectItem(),
                col("quarter").toSelectItem()
            );

            var result = new OracleToSqlServerPivotUnpivotRule().apply(query, context());

            assertTrue(result.changed(), () -> source.toString());
            assertEquals(RewriteFidelity.APPROXIMATE, result.fidelity());
            assertEquals(1, assertInstanceOf(SelectQuery.class, result.statement()).joins().size());
        }
    }

    @Test
    void rewritesCrossApplyForAliasedDerivedSourcesAndUnaliasedFunctionSource() {
        var querySource = QueryTable.of(select(col("q1_amount"), col("q1_quantity")).from(tbl("base_sales")).build()).as("q");
        var valuesSource = ValuesTable.of(RowListExpr.of(java.util.List.of(RowExpr.of(java.util.List.of(lit(1), lit(2)))))).as("v");
        var functionSource = FunctionTable.of(func("sales_rows"));
        var sampledSource = sampled(
            tbl("sampled_sales"),
            tableSample(TableSampleSpec.SampleMethod.BLOCK, TableSampleSpec.SampleUnit.PERCENT, lit(10), lit(42))).as(id("ss"));
        var pivotSource = pivot(
            tbl("sales"),
            java.util.List.of(pivotMeasure(func("sum", col("amount")))),
            col("quarter"),
            pivotValue(lit("Q1"), "q1")
        ).as("p");
        var unpivotSource = unpivot(
            tbl("wide_sales"),
            "amount",
            "quarter",
            unpivotInput("q1_amount", lit("Q1"))
        );

        for (var source : java.util.List.of(querySource, valuesSource, functionSource, sampledSource, pivotSource, unpivotSource)) {
            var query = multiColumnUnpivotQuery(
                source,
                col("region").toSelectItem(),
                col("amount").toSelectItem(),
                col("quarter").toSelectItem()
            );

            var result = new OracleToSqlServerPivotUnpivotRule().apply(query, context());

            assertTrue(result.changed(), () -> source.toString());
            assertEquals(RewriteFidelity.APPROXIMATE, result.fidelity());
            assertEquals(1, assertInstanceOf(SelectQuery.class, result.statement()).joins().size());
        }
    }

    @Test
    void rewritesCrossApplyForDialectSpecificSource() {
        var source = new TestDialectTableRef();
        var query = multiColumnUnpivotQuery(
            source,
            col("amount").toSelectItem(),
            col("quarter").toSelectItem()
        );

        var result = new OracleToSqlServerPivotUnpivotRule().apply(query, context());

        assertTrue(result.changed());
        var rewritten = assertInstanceOf(SelectQuery.class, result.statement());
        assertSame(source, rewritten.from());
        assertEquals(1, rewritten.joins().size());
    }

    @Test
    void leavesOuterSelectQueryUnchangedAfterNestedPivotRewrite() {
        var inner = parseOracle("""
            SELECT region, q1
            FROM sales
            PIVOT (sum(amount) FOR quarter IN ('Q1' AS q1))
            """);
        var outer = select(Expression.subquery(inner)).build();

        var result = new OracleToSqlServerPivotUnpivotRule().apply(outer, context());

        assertTrue(result.changed());
        assertInstanceOf(SelectQuery.class, result.statement());
    }

    private static final class TestDialectSelectItem implements DialectSelectItem {
        @Override
        @SuppressWarnings("unchecked")
        public <R> R accept(NodeVisitor<R> v) {
            if (v instanceof RecursiveNodeTransformer) {
                return (R) this;
            }
            return null;
        }
    }

    private static final class TestDialectTableRef implements DialectTableRef {
        @Override
        @SuppressWarnings("unchecked")
        public <R> R accept(NodeVisitor<R> v) {
            if (v instanceof RecursiveNodeTransformer) {
                return (R) this;
            }
            return null;
        }
    }

    @Test
    void ruleLeavesStatementsWithoutPivotOrUnpivotUnchanged() {
        var query = parseOracle("SELECT id FROM users");

        var result = new OracleToSqlServerPivotUnpivotRule().apply(query, context());

        assertFalse(result.changed());
        assertSame(query, result.statement());
        assertEquals("No PIVOT or UNPIVOT usage detected", result.description());
    }
}
