package io.sqm.dbit.oracle;

import io.sqm.core.Query;
import io.sqm.core.RangeDirection;
import io.sqm.core.ReturnRows;
import io.sqm.core.Statement;
import io.sqm.dbit.support.DialectExecutionCase;
import io.sqm.parser.oracle.spi.OracleSpecs;
import io.sqm.parser.spi.ParseContext;

import java.sql.SQLException;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Deterministic MODEL queries, shared by the Oracle live factory and its offline checks. */
final class OracleModelExecutionCases {
    private static final String INPUT = "SELECT product, year_key, sales FROM model_sales WHERE region = 'N' MODEL ";
    private static final String DECLARATIONS = "DIMENSION BY (product, year_key) MEASURES (amount AS sales) ";
    private static final String ORDER = " ORDER BY product, year_key";
    private static final List<String> ORIGINAL = List.of("Bike|2025|10", "Bike|2026|20", "Car|2025|30", "Car|2026|null");

    private static final List<QueryCase> QUERIES = List.of(
        parsed("model-partition-isolation", OracleLiveFeature.MODEL_PARTITIONS, """
            SELECT region, product, year_key, sales FROM model_sales MODEL RETURN ALL ROWS
                PARTITION BY (region) DIMENSION BY (product, year_key) MEASURES (amount AS sales)
                RULES (sales['Bike', 2027] = sales['Bike', 2025] + sales['Bike', 2026])
            ORDER BY region, product, year_key
            """, List.of("N|Bike|2025|10", "N|Bike|2026|20", "N|Bike|2027|30", "N|Car|2025|30", "N|Car|2026|null",
                "S|Bike|2025|100", "S|Bike|2026|200", "S|Bike|2027|300", "S|Car|2025|300", "S|Car|2026|null")),
        standard("model-return-updated", OracleLiveFeature.MODEL_RETURN_ROWS,
            "RETURN UPDATED ROWS " + DECLARATIONS + "RULES (sales['Bike', 2026] = 21, sales['Bike', 2027] = 22)",
            List.of("Bike|2026|21", "Bike|2027|22")),
        rules("model-update-never-inserts", OracleLiveFeature.MODEL_UPDATE,
            "UPDATE (sales['Bike', 2026] = 21, sales['Bike', 2027] = 22)",
            List.of("Bike|2025|10", "Bike|2026|21", "Car|2025|30", "Car|2026|null")),
        rules("model-upsert-inserts-positional", OracleLiveFeature.MODEL_UPSERT,
            "UPSERT (sales['Bike', 2027] = 22)",
            List.of("Bike|2025|10", "Bike|2026|20", "Bike|2027|22", "Car|2025|30", "Car|2026|null")),
        rules("model-upsert-symbolic-does-not-insert", OracleLiveFeature.MODEL_UPSERT,
            "UPSERT (sales[product = 'Bike', 2027] = 22)", ORIGINAL),
        rules("model-upsert-all-symbolic-inserts", OracleLiveFeature.MODEL_UPSERT_ALL,
            "UPSERT ALL (sales[product IN ('Bike', 'Car'), 2027] = sales[CV(product), 2025] * 2)",
            List.of("Bike|2025|10", "Bike|2026|20", "Bike|2027|20", "Car|2025|30", "Car|2026|null", "Car|2027|60")),
        rules("model-per-rule-mode-overrides", OracleLiveFeature.MODEL_RULE_OVERRIDES,
            "UPDATE (UPSERT sales['Bike', 2027] = 22, UPDATE sales['Car', 2027] = 99)",
            List.of("Bike|2025|10", "Bike|2026|20", "Bike|2027|22", "Car|2025|30", "Car|2026|null")),
        rules("model-sequential-order", OracleLiveFeature.MODEL_SEQUENTIAL_ORDER,
            "SEQUENTIAL ORDER (sales['Bike', 2027] = sales['Bike', 2026], sales['Bike', 2026] = 99)",
            List.of("Bike|2025|10", "Bike|2026|99", "Bike|2027|20", "Car|2025|30", "Car|2026|null")),
        rules("model-automatic-dependency-order", OracleLiveFeature.MODEL_AUTOMATIC_ORDER,
            "AUTOMATIC ORDER (sales['Bike', 2027] = sales['Bike', 2026], sales['Bike', 2026] = 99)",
            List.of("Bike|2025|10", "Bike|2026|99", "Bike|2027|99", "Car|2025|30", "Car|2026|null")),
        standard("model-keep-nav", OracleLiveFeature.MODEL_NAVIGATION,
            "KEEP NAV " + DECLARATIONS + "RULES (sales['Bike', 2027] = sales['Car', 2026] + 1, sales['Car', 2027] = sales['Missing', 2025] + 1)",
            List.of("Bike|2025|10", "Bike|2026|20", "Bike|2027|null", "Car|2025|30", "Car|2026|null", "Car|2027|null")),
        standard("model-ignore-nav", OracleLiveFeature.MODEL_NAVIGATION,
            "IGNORE NAV " + DECLARATIONS + "RULES (sales['Bike', 2027] = sales['Car', 2026] + 1, sales['Car', 2027] = sales['Missing', 2025] + 1)",
            List.of("Bike|2025|10", "Bike|2026|20", "Bike|2027|1", "Car|2025|30", "Car|2026|null", "Car|2027|1")),
        standard("model-unique-dimension", OracleLiveFeature.MODEL_UNIQUENESS,
            "UNIQUE DIMENSION " + DECLARATIONS + "RULES (sales['Bike', 2025] = sales['Bike', 2025] + 1)",
            List.of("Bike|2025|11", "Bike|2026|20", "Car|2025|30", "Car|2026|null")),
        parsed("model-unique-single-reference-duplicate-targets", OracleLiveFeature.MODEL_UNIQUENESS, """
            SELECT product, sales FROM model_sales WHERE region = 'N' AND product = 'Bike'
            MODEL UNIQUE SINGLE REFERENCE DIMENSION BY (product) MEASURES (amount AS sales)
                RULES UPDATE (sales['Bike'] = 7)
            ORDER BY product, sales
            """, List.of("Bike|7", "Bike|7")),
        standard("model-symbolic-multicell-current-dimension", OracleLiveFeature.MODEL_SYMBOLIC_CELLS,
            "DIMENSION BY (product, year_key) MEASURES (amount AS sales, amount AS original_sales) "
                + "RULES UPDATE (sales[product IN ('Bike', 'Car'), year_key BETWEEN 2025 AND 2026] = original_sales[CV(product), CV(year_key)] + 1)",
            List.of("Bike|2025|11", "Bike|2026|21", "Car|2025|31", "Car|2026|null")),
        rules("model-wildcard-self-update", OracleLiveFeature.MODEL_SYMBOLIC_CELLS,
            "UPDATE (sales[ANY, ANY] = sales[CV(product), CV(year_key)] + 1)",
            List.of("Bike|2025|11", "Bike|2026|21", "Car|2025|31", "Car|2026|null")),
        rules("model-aggregate-cell-range", OracleLiveFeature.MODEL_AGGREGATES,
            "(sales['Total', 2027] = SUM(sales)[ANY, year_key BETWEEN 2025 AND 2026])",
            List.of("Bike|2025|10", "Bike|2026|20", "Car|2025|30", "Car|2026|null", "Total|2027|60")),
        rules("model-ordered-rule-descending", OracleLiveFeature.MODEL_ORDERED_RULES,
            "UPDATE (sales[product = 'Bike', year_key BETWEEN 2025 AND 2026] ORDER BY year_key DESC = NVL(sales[CV(product), CV(year_key) + 1], 0) + 1)",
            List.of("Bike|2025|2", "Bike|2026|1", "Car|2025|30", "Car|2026|null")),
        rules("model-for-scalar-values", OracleLiveFeature.MODEL_FOR_VALUES,
            "(sales['Bike', FOR year_key IN (2027, 2028)] = CV(year_key) - 2020)", bikeYears()),
        rules("model-for-tuple-values", OracleLiveFeature.MODEL_FOR_TUPLES,
            "(sales[FOR (product, year_key) IN (('Bike', 2027), ('Car', 2028))] = CV(year_key) - 2020)", tupleYears()),
        rules("model-for-scalar-query", OracleLiveFeature.MODEL_FOR_QUERY,
            "(sales['Bike', FOR year_key IN (SELECT year_key FROM model_years)] = CV(year_key) - 2020)", bikeYears()),
        rules("model-for-tuple-query", OracleLiveFeature.MODEL_FOR_QUERY,
            "(sales[FOR (product, year_key) IN (SELECT product, year_key FROM model_targets)] = CV(year_key) - 2020)", tupleYears()),
        rules("model-for-increment", OracleLiveFeature.MODEL_FOR_RANGE,
            "(sales['Bike', FOR year_key FROM 2027 TO 2028 INCREMENT 1] = CV(year_key) - 2020)", bikeYears()),
        rules("model-for-decrement", OracleLiveFeature.MODEL_FOR_RANGE,
            "(sales['Bike', FOR year_key FROM 2028 TO 2027 DECREMENT 1] = CV(year_key) - 2020)", bikeYears()),
        rules("model-for-like-template", OracleLiveFeature.MODEL_FOR_LIKE,
            "(sales[FOR product LIKE 'P%' FROM 3 TO 1 DECREMENT 1, 2027] = 7)",
            List.of("Bike|2025|10", "Bike|2026|20", "Car|2025|30", "Car|2026|null", "P1|2027|7", "P2|2027|7", "P3|2027|7")),
        rules("model-iterate-iteration-number", OracleLiveFeature.MODEL_ITERATION,
            "ITERATE (3) (sales['Bike', 2025] = sales['Bike', 2025] + ITERATION_NUMBER)",
            List.of("Bike|2025|13", "Bike|2026|20", "Car|2025|30", "Car|2026|null")),
        rules("model-iteration-generates-cells", OracleLiveFeature.MODEL_ITERATION,
            "ITERATE (2) (sales['Bike', 2027 + ITERATION_NUMBER] = 7 + ITERATION_NUMBER)", bikeYears()),
        parsed("model-self-contained-iterative-forecast", OracleLiveFeature.MODEL_ITERATION, """
            SELECT year_key, sales FROM dual MODEL
                DIMENSION BY (2025 AS year_key) MEASURES (100 AS sales)
                RULES ITERATE (3) (
                    sales[2026 + ITERATION_NUMBER] = sales[2025 + ITERATION_NUMBER] * 2
                )
            ORDER BY year_key
            """, List.of("2025|100", "2026|200", "2027|400", "2028|800")),
        parsed("model-until-previous-stops-early", OracleLiveFeature.MODEL_UNTIL_PREVIOUS, """
            SELECT product, year_key, sales, steps FROM model_sales WHERE region = 'N' MODEL
                DIMENSION BY (product, year_key) MEASURES (amount AS sales, 0 AS steps)
                RULES ITERATE (10) UNTIL (PREVIOUS(sales['Bike', 2025]) = sales['Bike', 2025]) (
                    sales['Bike', 2025] = LEAST(13, sales['Bike', 2025] + 1),
                    steps['Bike', 2025] = ITERATION_NUMBER
                )
            ORDER BY product, year_key
            """, List.of("Bike|2025|13|3", "Bike|2026|20|0", "Car|2025|30|0", "Car|2026|null|0")),
        rules("model-presence-input-snapshot-and-null", OracleLiveFeature.MODEL_PRESENCE,
            "SEQUENTIAL ORDER (sales['Bike', 2027] = PRESENTV(sales['Car', 2026], 1, 0), "
                + "sales['Car', 2027] = PRESENTV(sales['Missing', 2025], 1, 0), "
                + "sales['Bike', 2028] = PRESENTNNV(sales['Car', 2026], 1, 0), "
                + "sales['Car', 2028] = PRESENTNNV(sales['Bike', 2025], 1, 0), "
                + "sales['Bike', 2029] = CASE WHEN sales['Bike', 2027] IS PRESENT THEN 1 ELSE 0 END, "
                + "sales['Car', 2029] = CASE WHEN sales['Car', 2026] IS PRESENT THEN 1 ELSE 0 END)",
            List.of("Bike|2025|10", "Bike|2026|20", "Bike|2027|1", "Bike|2028|0", "Bike|2029|0",
                "Car|2025|30", "Car|2026|null", "Car|2027|0", "Car|2028|1", "Car|2029|1")),
        standard("model-reference-qualified-and-main-name", OracleLiveFeature.MODEL_REFERENCE,
            "REFERENCE growth ON (SELECT product, rate FROM model_rates) DIMENSION BY (product) MEASURES (rate) "
                + "MAIN forecast " + DECLARATIONS
                + "RULES (sales['Bike', 2027] = forecast.sales['Bike', 2025] * growth.rate['Bike'], "
                + "sales['Car', 2027] = sales['Car', 2025] * growth.rate['Car'])",
            List.of("Bike|2025|10", "Bike|2026|20", "Bike|2027|20", "Car|2025|30", "Car|2026|null", "Car|2027|90")),
        standard("model-nested-cell-read", OracleLiveFeature.MODEL_NESTED_CELLS,
            "DIMENSION BY (product, year_key) MEASURES (amount AS sales, 2025 AS lookup_year) "
                + "RULES (sales['Bike', 2027] = sales['Bike', lookup_year['Car', 2026]])",
            List.of("Bike|2025|10", "Bike|2026|20", "Bike|2027|10", "Car|2025|30", "Car|2026|null")),
        standard("model-nested-static-cell-target", OracleLiveFeature.MODEL_NESTED_CELLS,
            "DIMENSION BY (product, year_key) MEASURES (amount AS sales, 2025 AS lookup_year) "
                + "RULES AUTOMATIC ORDER (sales['Bike', lookup_year['Car', 2026]] = 77)",
            List.of("Bike|2025|77", "Bike|2026|20", "Car|2025|30", "Car|2026|null")),
        standard("model-analytic-update", OracleLiveFeature.MODEL_ANALYTICS,
            "DIMENSION BY (product, year_key) MEASURES (amount AS sales, amount AS original_sales) "
                + "RULES UPDATE (sales[ANY, ANY] = SUM(original_sales) OVER (PARTITION BY product))",
            List.of("Bike|2025|30", "Bike|2026|30", "Car|2025|30", "Car|2026|30")),
        parsed("model-quoted-declarations", OracleLiveFeature.MODEL_QUOTED_NAMES, """
            SELECT "Product", "Year", "Sales" FROM model_sales WHERE region = 'N' MODEL
                DIMENSION BY (product AS "Product", year_key AS "Year") MEASURES (amount AS "Sales")
                RULES ("Sales"['Bike', 2027] = "Sales"['Bike', 2025] + 1)
            ORDER BY "Product", "Year"
            """, List.of("Bike|2025|10", "Bike|2026|20", "Bike|2027|11", "Car|2025|30", "Car|2026|null")),
        new QueryCase("model-representative-fluent-dsl", Set.of(OracleLiveFeature.MODEL_DSL),
            select(col("product"), col("year_key"), col("sales")).from(tbl("model_sales")).where(col("region").eq("N"))
                .model(model().returnRows(ReturnRows.UPDATED).dimension("product").dimension("year_key").measure("amount", "sales")
                    .rule(cellTarget("sales", cellForValues("product", "Bike", "Car"),
                            cellForRange("year_key", 2027L, 2028L, RangeDirection.INCREMENT, 1L)),
                        cellRef("sales", currentDimension(), 2025L).mul(lit(2L))).build())
                .orderBy(col("product"), col("year_key")).build(),
            List.of("Bike|2027|20", "Bike|2028|20", "Car|2027|60", "Car|2028|60"))
    );

    private OracleModelExecutionCases() {
    }

    static List<QueryCase> queries() {
        return QUERIES;
    }

    static List<DialectExecutionCase<OracleLiveFeature, OracleExecutionHarness>> cases() {
        return Stream.concat(QUERIES.stream().map(sample -> new DialectExecutionCase<>(sample.id(), sample.features(),
                (OracleExecutionHarness harness) -> assertRows(harness, sample.query(), sample.expectedRows()))),
            Stream.concat(dmlCases().stream(), runtimeErrorCases().stream())).toList();
    }

    private static List<DialectExecutionCase<OracleLiveFeature, OracleExecutionHarness>> runtimeErrorCases() {
        return List.of(
            rejected("model-runtime-rejects-duplicate-dimensions", OracleLiveFeature.MODEL_UNIQUENESS,
                "SELECT product, sales FROM model_sales WHERE region = 'N' AND product = 'Bike' "
                    + "MODEL UNIQUE DIMENSION DIMENSION BY (product) MEASURES (amount AS sales) "
                    + "RULES UPDATE (sales['Bike'] = 7) ORDER BY product, sales", 32638),
            rejected("model-runtime-rejects-self-cyclic-multicell-rule", OracleLiveFeature.MODEL_SYMBOLIC_CELLS,
                INPUT + DECLARATIONS + "RULES UPDATE (sales[product IN ('Bike', 'Car'), year_key BETWEEN 2025 AND 2026] "
                    + "= sales[CV(product), CV(year_key)] + 1)" + ORDER, 32637)
        );
    }

    private static DialectExecutionCase<OracleLiveFeature, OracleExecutionHarness> rejected(
        String id, OracleLiveFeature feature, String sql, int errorCode
    ) {
        var query = parse(sql);
        return new DialectExecutionCase<>(id, Set.of(feature), harness -> {
            var rendered = harness.render(query);
            var error = assertThrows(SQLException.class, () -> harness.queryRows(rendered), rendered);
            assertEquals(errorCode, error.getErrorCode(), rendered);
            assertEquals(ORIGINAL, harness.queryRows(
                "SELECT product, year_key, amount FROM model_sales WHERE region = 'N' ORDER BY product, year_key"));
        });
    }

    private static List<DialectExecutionCase<OracleLiveFeature, OracleExecutionHarness>> dmlCases() {
        var source = parse(INPUT + "RETURN UPDATED ROWS " + DECLARATIONS
            + "RULES (sales['Bike', 2027] = 22, sales['Car', 2027] = 33)" + ORDER);
        var seed = "insert into model_output (product, year_key, sales) values ('Bike', 2027, 1)";
        return List.of(
            dml("model-insert-query-source", insert("model_output").columns(id("product"), id("year_key"), id("sales")).query(source).build(),
                null, 2, List.of("Bike|2027|22", "Car|2027|33")),
            dml("model-merge-query-source", merge("model_output").target(tbl("model_output").as("t"))
                    .source(tbl(source).as("s")).on(col("t", "product").eq(col("s", "product")))
                    .whenMatchedUpdate(set("sales", col("s", "sales")))
                    .whenNotMatchedInsert(List.of(id("product"), id("year_key"), id("sales")), row(col("s", "product"), col("s", "year_key"), col("s", "sales")))
                    .build(), seed, 2, List.of("Bike|2027|22", "Car|2027|33")),
            dml("model-update-scalar-query-source", update("model_output")
                    .set("sales", expr(select(col("sales")).from(tbl(source).as("s")).where(col("product").eq("Bike")).build())).build(),
                seed, 1, List.of("Bike|2027|22")),
            dml("model-delete-exists-query-source", delete("model_output")
                    .where(exists(select(lit(1)).from(tbl(source).as("s")).where(col("s", "product").eq("Bike")).build())).build(),
                seed, 1, List.of())
        );
    }

    private static DialectExecutionCase<OracleLiveFeature, OracleExecutionHarness> dml(
        String id, Statement statement, String seed, int affectedRows, List<String> expectedRows
    ) {
        return new DialectExecutionCase<>(id, Set.of(OracleLiveFeature.MODEL_DML_SOURCES), harness -> {
            if (seed != null) harness.executeStatements(seed);
            var sql = harness.render(statement);
            assertEquals(affectedRows, harness.executeUpdate(sql, List.of()), sql);
            assertEquals(expectedRows, harness.queryRows("SELECT product, year_key, sales FROM model_output ORDER BY product, year_key"));
        });
    }

    private static QueryCase rules(String id, OracleLiveFeature feature, String rules, List<String> expectedRows) {
        return standard(id, feature, DECLARATIONS + "RULES " + rules, expectedRows);
    }

    private static QueryCase standard(String id, OracleLiveFeature feature, String clause, List<String> expectedRows) {
        return parsed(id, feature, INPUT + clause + ORDER, expectedRows);
    }

    private static QueryCase parsed(String id, OracleLiveFeature feature, String sql, List<String> expectedRows) {
        return new QueryCase(id, EnumSet.of(feature), parse(sql), expectedRows);
    }

    private static Query parse(String sql) {
        var result = ParseContext.of(new OracleSpecs()).parse(Query.class, sql);
        if (result.isError()) throw new IllegalArgumentException("Invalid Oracle MODEL fixture: " + result.errorMessage() + "\n" + sql);
        return result.value();
    }

    private static void assertRows(OracleExecutionHarness harness, Query query, List<String> expectedRows) throws Exception {
        var rendered = harness.render(query);
        assertTrue(rendered.contains("MODEL"));
        assertEquals(expectedRows, harness.queryRows(rendered), rendered);
    }

    private static List<String> bikeYears() {
        return List.of("Bike|2025|10", "Bike|2026|20", "Bike|2027|7", "Bike|2028|8", "Car|2025|30", "Car|2026|null");
    }

    private static List<String> tupleYears() {
        return List.of("Bike|2025|10", "Bike|2026|20", "Bike|2027|7", "Car|2025|30", "Car|2026|null", "Car|2028|8");
    }

    record QueryCase(String id, Set<OracleLiveFeature> features, Query query, List<String> expectedRows) {
        QueryCase {
            features = Set.copyOf(features);
            expectedRows = List.copyOf(expectedRows);
        }
    }
}
