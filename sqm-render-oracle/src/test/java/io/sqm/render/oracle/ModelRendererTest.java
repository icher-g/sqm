package io.sqm.render.oracle;

import io.sqm.core.*;
import io.sqm.core.dialect.UnsupportedDialectFeatureException;
import io.sqm.parser.oracle.spi.OracleSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.render.ansi.SelectQueryRenderer;
import io.sqm.render.ansi.spi.AnsiDialect;
import io.sqm.render.defaults.DefaultSqlWriter;
import io.sqm.render.oracle.spi.OracleDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class ModelRendererTest {
    private final RenderContext ctx = RenderContext.of(new OracleDialect());
    private final ParseContext parser = ParseContext.of(new OracleSpecs());

    @Test
    void rendersFluentModelAndRoundTrips() {
        var query = select(col("product"), col("year_key"), col("sales")).from(tbl("sales_data"))
            .model(model().partition("country").dimension("product").dimension("sales_year", "year_key")
                .measure("amount", "sales")
                .rule(cellTarget("sales").address("Bike").address(2026L).build(),
                    cellRef("sales").selector("Bike").selector(2025L).build().mul(lit(1.1)))
                .build()).orderBy(col("year_key")).build();
        var sql = ctx.render(query).sql();
        assertEquals("""
            SELECT product, year_key, sales
            FROM sales_data
            MODEL
              PARTITION BY (country)
              DIMENSION BY (product, sales_year AS year_key)
              MEASURES (amount AS sales)
              RULES (sales['Bike', 2026] = sales['Bike', 2025] * 1.1)
            ORDER BY year_key""", sql);
        var parsed = parser.parse(Query.class, sql);
        assertTrue(parsed.ok(), parsed::errorMessage);
        assertEquals(query, parsed.value());
    }

    @Test
    void indentsMultipleRulesOnSeparateLinesAndRestoresQueryIndentation() {
        var query = select(col("country"), col("product"), col("sales_year"), col("sales"))
            .from(tbl("sales_data"))
            .model(model().partition("country").dimension("product").dimension("sales_year")
                .measure("amount", "sales")
                .rule(cellTarget("sales", "Bike", 2026L), cellRef("sales", "Bike", 2025L).mul(lit(1.1)))
                .rule(cellTarget("sales", "Car", 2026L), cellRef("sales", "Car", 2025L).mul(lit(1.05)))
                .build())
            .orderBy(col("country"), col("product"), col("sales_year")).build();

        var sql = ctx.render(query).sql();

        assertEquals("""
            SELECT country, product, sales_year, sales
            FROM sales_data
            MODEL
              PARTITION BY (country)
              DIMENSION BY (product, sales_year)
              MEASURES (amount AS sales)
              RULES (
                sales['Bike', 2026] = sales['Bike', 2025] * 1.1,
                sales['Car', 2026] = sales['Car', 2025] * 1.05
              )
            ORDER BY country, product, sales_year""", sql);
        var parsed = parser.parse(Query.class, sql);
        assertTrue(parsed.ok(), parsed::errorMessage);
        assertEquals(query, parsed.value());
    }

    @Test
    void preservesSurroundingIndentationForNestedModelQueries() {
        var inner = select(col("sales")).from(tbl("sales_data"))
            .model(model().dimension("sales_year").measure("amount", "sales")
                .rule(cellTarget("sales", 2026L), lit(100L))
                .rule(cellTarget("sales", 2027L), lit(110L)).build())
            .orderBy(col("sales")).build();
        var query = select(col("sales")).from(tbl(inner).as("forecast"))
            .orderBy(col("sales")).build();

        var sql = ctx.render(query).sql();

        assertEquals("""
            SELECT sales
            FROM (
              SELECT sales
              FROM sales_data
              MODEL
                DIMENSION BY (sales_year)
                MEASURES (amount AS sales)
                RULES (
                  sales[2026] = 100,
                  sales[2027] = 110
                )
              ORDER BY sales
            ) forecast
            ORDER BY sales""", sql);
        var parsed = parser.parse(Query.class, sql);
        assertTrue(parsed.ok(), parsed::errorMessage);
        assertEquals(query, parsed.value());
    }

    @Test
    void canonicalizesAllOptionsAndPlacement() {
        for (var mode : RuleMode.values()) {
            for (var order : RuleOrder.values()) {
                var main = mainModel().name("forecast").dimension("year_key").measure(lit(0L), "sales")
                    .navigationMode(NavigationMode.IGNORE).uniquenessMode(UniquenessMode.SINGLE_REFERENCE)
                    .rules(modelRules().defaultMode(mode).order(order)
                        .rule(modelRule(mode, cellTarget("sales", 2026L), null, lit(10L))).build()).build();
                var query = select(col("sales")).from(tbl("input"))
                    .groupBy("year_key").having(col("year_key").gt(0L))
                    .model(model().returnRows(ReturnRows.UPDATED).main(main).build())
                    .orderBy(col("sales")).build();
                var sql = ctx.render(query).sql();
                assertTrue(sql.contains("MAIN forecast"));
                assertTrue(sql.contains("RETURN UPDATED ROWS"));
                assertTrue(sql.contains("IGNORE NAV UNIQUE SINGLE REFERENCE"));
                assertTrue(sql.indexOf("HAVING") < sql.indexOf("MODEL"));
                assertTrue(sql.indexOf("MODEL") < sql.indexOf("ORDER BY"));
                var parsed = parser.parse(Query.class, sql);
                assertTrue(parsed.ok(), parsed::errorMessage);
                assertEquals(query, parsed.value());
            }
        }
    }

    @Test
    void roundTripsInputsWithAliasesDefaultsArithmeticAndQuotedNames() {
        for (var sql : List.of(
            "SELECT sales FROM t MODEL DIMENSION BY (year_key) MEASURES (sales) (sales[2026] = 1)",
            "SELECT sales FROM t MODEL KEEP NAV UNIQUE DIMENSION RETURN ALL ROWS DIMENSION BY (year_key) "
                + "MEASURES (sales) RULES UPSERT SEQUENTIAL ORDER (sales[2026] = sales[2025] + 1)",
            "SELECT \"Sales\" FROM t MODEL DIMENSION BY (year_key AS \"Year\") "
                + "MEASURES (amount + 1 AS \"Sales\") (\"Sales\"[2026] = ABS(\"Sales\"[2025]))",
            "SELECT sales FROM t MODEL IGNORE NAV DIMENSION BY (year_key) MEASURES (sales) "
                + "KEEP NAV (sales[NULL] = -2)",
            "SELECT sales FROM t MODEL DIMENSION BY (year_key) MEASURES (sales) "
                + "(sales[2026] = (sales[2025] + 1) * 2)")) {
            var first = parser.parse(Query.class, sql);
            assertTrue(first.ok(), first::errorMessage);
            var rendered = ctx.render(first.value()).sql();
            var second = parser.parse(Query.class, rendered);
            assertTrue(second.ok(), second::errorMessage);
            assertEquals(first.value(), second.value(), rendered);
        }
    }

    @Test
    void rejectsUnsupportedDialectBeforeWritingQuery() {
        var ansi = RenderContext.of(new AnsiDialect());
        var writer = new DefaultSqlWriter(ansi);
        var query = select(col("sales")).model(model().dimension("year_key").measure("sales")
            .rule(cellTarget("sales", 2026), lit(1)).build()).build();
        assertThrows(UnsupportedDialectFeatureException.class,
            () -> new SelectQueryRenderer().render(query, ansi, writer));
        assertEquals("", writer.toText(List.of()).sql());
    }

    @Test
    void rejectsInvalidModelGrammar() {
        var target = cellTarget("sales", 2026);
        for (Node node : List.of(
            modelRule(null, target, orderBy(order(1)), lit(1)),
            cellValue(col("year_key").eq(2026)),
            cellValue(expr(select(lit(1)).build())))) {
            var error = assertThrows(IllegalArgumentException.class, () -> ctx.render(node), node.toString());
            assertTrue(error.getMessage().contains("MODEL"), error::getMessage);
        }
    }

    @Test
    void leavesSemanticRestrictionsToTheValidator() {
        assertDoesNotThrow(() -> ctx.render(iterationSpec(col("limit_value"))));
        assertDoesNotThrow(() -> ctx.render(modelRules().order(RuleOrder.AUTOMATIC)
            .rule(cellTarget("sales", 2026), lit(1)).iteration(iterationSpec(lit(2))).build()));
        assertDoesNotThrow(() -> ctx.render(cellForRange("year_key", 1, 10001, RangeDirection.INCREMENT, 1)));
        assertDoesNotThrow(() -> ctx.render(cellForValues(List.of(id("p"), id("y")), select(lit(1)).build())));
    }
}
