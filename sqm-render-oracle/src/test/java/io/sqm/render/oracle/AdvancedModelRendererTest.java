package io.sqm.render.oracle;

import io.sqm.core.*;
import io.sqm.parser.oracle.spi.OracleSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.render.oracle.spi.OracleDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;
import java.util.List;
import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class AdvancedModelRendererTest {
    private final ParseContext parser = ParseContext.of(new OracleSpecs());
    private final RenderContext renderer = RenderContext.of(new OracleDialect());
    private static final String PREFIX = "SELECT sales FROM sales_data MODEL DIMENSION BY (product, year_key) MEASURES (sales) ";

    @Test
    void roundTripsEveryDesignedAdvancedGrammarFamily() {
        for (String suffix : List.of(
            "RULES (sales[product IS ANY, ANY] = 1)",
            "RULES (sales[product IN ('Bike', 'Car'), year_key NOT BETWEEN 2025 AND 2027] = 1)",
            "RULES (sales[(product = 'Bike' OR product IS NULL), year_key >= 2026] = 1)",
            "RULES (sales[product LIKE 'B%', year_key NOT IN (2025, 2026)] = 1)",
            "RULES UPSERT ALL (sales[FOR product IN ('Bike', 'Car'), FOR year_key FROM 2025 TO 2027 INCREMENT 1] = sales[CV(product), CV(year_key)-1])",
            "RULES (sales[FOR product LIKE 'P%' FROM 3 TO 1 DECREMENT 1, 2026] = 1)",
            "RULES (sales[FOR (product, year_key) IN (('Bike', 2026), ('Car', 2027))] = 1)",
            "RULES (sales[FOR (product, year_key) IN (SELECT product, year_key FROM forecast)] = 1)",
            "RULES (sales['Bike', FOR year_key IN (SELECT year_key FROM forecast)] = 1)",
            "RULES (sales['Bike', FOR year_key IN (WITH forecast AS (SELECT year_key FROM years) SELECT year_key FROM forecast)] = 1)",
            "RULES (sales['Bike', FOR (year_key) IN ((2026), (2027))] = 1)",
            "RULES UPDATE AUTOMATIC ORDER (sales[ANY, 2026] = SUM(sales)[ANY, year_key BETWEEN 2023 AND 2025])",
            "RULES (sales[ANY, 2026] ORDER BY product DESC, year_key ASC = AVG(sales)[ANY, ANY])",
            "RULES ITERATE (1) (sales['Bike', 2026] = ITERATION_NUMBER)",
            "RULES ITERATE (+1) (sales['Bike', 2026] = ITERATION_NUMBER)",
            "RULES UPDATE ITERATE (10) UNTIL (ABS(PREVIOUS(sales['Bike', 2026]) - sales['Bike', 2026]) < 1) (sales['Bike', 2026] = sales['Bike', 2026] / 2)",
            "RULES (sales['Bike', 2026] = sales['Bike', sales['Bike', 2025]])",
            "RULES (sales['Bike', 2026] = PRESENTV(sales['Bike', 2025], 1, 0))",
            "RULES (sales['Bike', 2026] = PRESENTNNV(sales['Bike', 2025], sales['Bike', CV()], CV(year_key)))",
            "RULES (sales['Bike', 2026] = CASE WHEN NOT (sales['Bike', 2025] IS PRESENT) THEN 0 ELSE 1 END)",
            "RULES (sales['Bike', 2026] = CASE WHEN (sales['Bike', 2025] IS PRESENT) THEN 1 ELSE 0 END)",
            "RULES (sales['Bike', 2026] = CASE WHEN sales['Bike', 2025] IS PRESENT AND sales['Bike', 2025] > 0 THEN 1 ELSE 0 END)"
        )) {
            var first = parser.parse(Query.class, PREFIX + suffix);
            assertTrue(first.ok(), () -> suffix + ": " + first.errorMessage());
            var sql = renderer.render(first.value()).sql();
            var second = parser.parse(Query.class, sql);
            assertTrue(second.ok(), () -> sql + ": " + second.errorMessage());
            assertEquals(first.value(), second.value(), sql);
        }
    }

    @Test
    void formatsReferenceModelsAndNamedMainWithNestedIndentation() {
        var reference = referenceModel("rates", select(col("code"), col("rate")).from(tbl("fx")).build())
            .dimension("code").measure("rate").navigationMode(NavigationMode.IGNORE).build();
        var main = mainModel().name("forecast").dimension("product").dimension("year_key").measure("sales")
            .rules(modelRules().defaultMode(RuleMode.UPDATE)
                .iteration(iterationSpec(lit(3L), previousModelValueExpr(cellRef("sales", "Bike", 2026L))
                    .sub(cellRef("sales", "Bike", 2026L)).lt(lit(1L))))
                .rule(cellTarget("sales", "Bike", 2026L),
                    cellRef("sales", "Bike", 2026L)
                        .mul(cellRef("rate").model("rates").selector("USD").build()))
                .build()).build();
        var query = select(col("sales")).from(tbl("sales_data"))
            .model(model().returnRows(ReturnRows.UPDATED).reference(reference).main(main).build())
            .orderBy(col("sales")).build();
        var sql = renderer.render(query).sql();
        assertEquals("""
            SELECT sales
            FROM sales_data
            MODEL RETURN UPDATED ROWS
              REFERENCE rates ON (
                SELECT code, rate
                FROM fx
              )
                DIMENSION BY (code)
                MEASURES (rate) IGNORE NAV
              MAIN forecast
                DIMENSION BY (product, year_key)
                MEASURES (sales)
                RULES UPDATE ITERATE (3) UNTIL (PREVIOUS(sales['Bike', 2026]) - sales['Bike', 2026] < 1) (sales['Bike', 2026] = sales['Bike', 2026] * rates.rate['USD'])
            ORDER BY sales""", sql);
        var parsed = parser.parse(Query.class, sql);
        assertTrue(parsed.ok(), parsed::errorMessage);
        assertEquals(query, parsed.value());
    }

    @Test
    void canonicalizesMultipleReferenceDefaultsWithoutChangingTheirEffectiveModes() {
        var sql = "SELECT sales FROM sales_data MODEL IGNORE NAV UNIQUE SINGLE REFERENCE RETURN ALL ROWS "
            + "REFERENCE base ON (SELECT y, rate FROM rates) DIMENSION BY (y) MEASURES (rate) "
            + "REFERENCE \"Exact\" ON (SELECT y, rate FROM rates) DIMENSION BY (y) MEASURES (rate) KEEP NAV UNIQUE DIMENSION "
            + "MAIN forecast DIMENSION BY (year_key) MEASURES (sales) KEEP NAV UNIQUE DIMENSION "
            + "RULES (sales[2026] = base.rate[2025] + \"Exact\".rate[2025])";
        var first = parser.parse(Query.class, sql);
        assertTrue(first.ok(), first::errorMessage);
        var rendered = renderer.render(first.value()).sql();
        assertTrue(rendered.contains("MEASURES (rate) IGNORE NAV UNIQUE SINGLE REFERENCE"));
        assertTrue(rendered.contains("REFERENCE \"Exact\" ON"));
        var second = parser.parse(Query.class, rendered);
        assertTrue(second.ok(), second::errorMessage);
        assertEquals(first.value(), second.value());
    }
}
