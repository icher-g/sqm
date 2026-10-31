package io.sqm.codegen;

import io.sqm.core.Statement;
import io.sqm.parser.oracle.spi.OracleSpecs;
import io.sqm.parser.spi.ParseContext;
import io.sqm.render.oracle.spi.OracleDialect;
import io.sqm.render.spi.RenderContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;

import static io.sqm.dsl.Dsl.*;

import static org.junit.jupiter.api.Assertions.*;

class ModelCodegenTest {
    @TempDir
    Path tempDir;

    @Test
    void emitsReadableBuildersInsteadOfCoreFactoriesOrImplementationClasses() {
        var emitted = new SqmJavaEmitter().emit(parse(fixtures().get("minimal")));
        assertTrue(emitted.contains(".model(model()"));
        assertTrue(emitted.contains(".dimension(\"y\")"));
        assertTrue(emitted.contains(".measure(\"amount\", \"sales\")"));
        assertTrue(emitted.contains(".rule(cellTarget("));
        assertFalse(emitted.contains(".dimension(modelColumn("));
        assertFalse(emitted.contains(".rule(modelRule("));
        assertTrue(emitted.contains("cellTarget(\"sales\", 2026L)"));
        assertTrue(emitted.contains("cellRef(\"sales\", 2025L)"));
        assertFalse(emitted.contains(".Impl"));
        assertFalse(emitted.contains("CellRefExpr.of"));
        var named = new SqmJavaEmitter().emit(parse(fixtures().get("declarations")));
        assertTrue(named.contains(".name(\"forecast\")"));
        assertTrue(named.contains(".dimension(col(\"d\", \"product\"))"));
        assertTrue(named.contains(".measure(col(\"d\", \"amount\"), \"sales\")"));
        assertTrue(named.contains(".measure(lit(0L), \"calculated\")"));
        assertFalse(named.contains("modelColumn("));
        var ordered = new SqmJavaEmitter().emit(parse(fixtures().get("rule_options")));
        assertTrue(ordered.contains(".rule(RuleMode.UPDATE, cellTarget("));
        assertTrue(ordered.contains(".rule(null, cellTarget("));
        assertFalse(ordered.contains("modelRule("));
        var example = new SqmJavaEmitter().emit(parse(fixtures().get("user_forecast")));
        assertTrue(example.contains("cellTarget(\"sales\", \"Bike\", cellForRange(\"year_key\", 2026L, 2028L, RangeDirection.INCREMENT, 1L))"));
        assertTrue(example.contains("cellRef(\"sales\", \"Bike\", 2025L)"));
        assertTrue(example.contains("cellRef(\"rate\", \"Bike\").inModel(\"growth\")"));
        assertTrue(example.contains("currentDimension(\"year_key\")"));
        assertFalse(example.contains(".selector("));
        assertFalse(example.contains(".address("));
    }

    @Test
    void generatedJavaCompilesAndReconstructsAllModelFamiliesAndDmlSources() throws Exception {
        var sqlDir = tempDir.resolve("sql");
        var fixtures = fixtures();
        for (var fixture : fixtures.entrySet()) {
            var path = sqlDir.resolve("model/" + fixture.getKey() + ".sql");
            Files.createDirectories(path.getParent());
            Files.writeString(path, fixture.getValue(), StandardCharsets.UTF_8);
        }
        var generated = SqlFileCodeGenerator.of(SqlFileCodegenOptions.of(
            sqlDir, tempDir.resolve("generated"), "io.sqm.codegen.generated", SqlCodegenDialect.ORACLE)).generate();
        try (var loader = compile(generated, tempDir.resolve("classes"))) {
            var type = Class.forName("io.sqm.codegen.generated.ModelQueries", true, loader);
            for (var fixture : fixtures.entrySet()) {
                var actual = assertInstanceOf(Statement.class, type.getMethod(NameNormalizer.toMethodName(fixture.getKey())).invoke(null));
                var expected = parse(fixture.getValue());
                assertEquals(expected, actual, fixture.getKey());
                var renderer = RenderContext.of(new OracleDialect());
                assertEquals(renderer.render(expected).sql(), renderer.render(actual).sql(), fixture.getKey());
            }
        }
    }

    @Test
    void compactCellsPreserveLiteralBoxingAndExplicitPredicateValuedCoordinates() throws Exception {
        var coordinates = new Object[]{"Bike", 'X', (byte) 1, (short) 2, 3, 4L, 5.5F, 6.5, true, null};
        var builder = model().measure(lit(0), "sales");
        for (var index = 0; index < coordinates.length; index++) builder.dimension(lit(0), "d" + index);
        var literals = select(lit(1)).model(builder.rule(cellTarget("sales", coordinates), cellRef("sales", coordinates)).build()).build();
        // An explicit value containing a predicate must not turn into a condition selector.
        var coordinate = cellValue(lit(1).eq(1));
        var predicate = select(lit(1)).model(model().dimension(lit(0), "y").measure(lit(0), "sales")
            .rule(cellTarget("sales", coordinate), cellRef("sales", coordinate)).build()).build();
        var source = tempDir.resolve("CellQueries.java");
        Files.writeString(source, """
            import io.sqm.core.*;
            import java.util.List;
            import static io.sqm.dsl.Dsl.*;
            public class CellQueries {
              public static Statement literals() { return %s; }
              public static Statement predicate() { return %s; }
            }
            """.formatted(new SqmJavaEmitter().emit(literals), new SqmJavaEmitter().emit(predicate)), StandardCharsets.UTF_8);
        try (var loader = compile(List.of(source), tempDir.resolve("literal-classes"))) {
            var type = Class.forName("CellQueries", true, loader);
            assertEquals(literals, type.getMethod("literals").invoke(null));
            assertEquals(predicate, type.getMethod("predicate").invoke(null));
        }
    }

    private URLClassLoader compile(List<Path> generated, Path classes) throws Exception {
        Files.createDirectories(classes);
        var diagnostics = new DiagnosticCollector<JavaFileObject>();
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler);
        try (var manager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            var success = compiler.getTask(null, manager, diagnostics,
                List.of("-classpath", System.getProperty("java.class.path"), "-d", classes.toString()),
                null, manager.getJavaFileObjectsFromPaths(generated)).call();
            assertEquals(Boolean.TRUE, success, () -> diagnostics.getDiagnostics().toString());
        }
        return URLClassLoader.newInstance(new URL[]{classes.toUri().toURL()}, getClass().getClassLoader());
    }

    private static LinkedHashMap<String, String> fixtures() {
        var fixtures = new LinkedHashMap<String, String>();
        var prefix = "SELECT sales FROM data MODEL DIMENSION BY (p, y) MEASURES (amount AS sales) ";
        fixtures.put("minimal", "SELECT sales FROM data MODEL DIMENSION BY (y) MEASURES (amount AS sales) (sales[2026] = sales[2025] + 1)");
        fixtures.put("reference", """
            SELECT sales FROM data MODEL RETURN UPDATED ROWS
              REFERENCE "Growth" ON (SELECT p, rate FROM rates)
                DIMENSION BY (p) MEASURES (rate) IGNORE NAV UNIQUE SINGLE REFERENCE
              MAIN "Forecast" PARTITION BY (country) DIMENSION BY (p, y) MEASURES (amount AS sales)
                RULES UPSERT ALL SEQUENTIAL ORDER (
                  sales['Bike', FOR y FROM 2026 TO 2028 INCREMENT 1] =
                    "Forecast".sales['Bike', 2025] * "Growth".rate['Bike']
                )
            """);
        fixtures.put("symbolic", prefix + """
            RULES UPDATE AUTOMATIC ORDER (
              sales[p IS ANY, y BETWEEN 2026 AND 2028] = SUM(sales)[ANY, y IN (2024, 2025)],
              UPDATE sales['Bike', 2028] ORDER BY p DESC = AVG(sales)[p <> 'Car', ANY]
            )
            """);
        fixtures.put("generators", prefix + """
            RULES (
              sales[FOR p IN ('Bike', 'Car'), 2026] = 1,
              sales[FOR (p, y) IN (('Bike', 2026), ('Car', 2027))] = 2,
              sales[FOR (p, y) IN (SELECT p, y FROM years)] = 3,
              sales['Bike', FOR y IN (WITH years AS (SELECT y FROM source_years) SELECT y FROM years)] = 4,
              sales[FOR p LIKE 'P%' FROM 3 TO 1 DECREMENT 1, 2026] = 5
            )
            """);
        fixtures.put("iteration", prefix + """
            RULES ITERATE (3) UNTIL (PREVIOUS(sales['Bike', 2026]) = sales['Bike', 2026]) (
              sales['Bike', 2026] = sales['Bike', CV(y) - 1] + ITERATION_NUMBER,
              sales['Car', 2026] = PRESENTV(sales['Car', 2025], CV(y), 0),
              sales['Car', 2027] = PRESENTNNV(sales['Car', 2025], sales['Car', CV()], 0),
              sales['Car', 2028] = CASE WHEN sales['Car', 2025] IS PRESENT THEN 1 ELSE 0 END
            )
            """);
        fixtures.put("no_until", prefix + "RULES ITERATE (2) (sales['Bike', 2026] = sales['Bike', sales['Bike', 2025]])");
        fixtures.put("insert_source", "INSERT INTO forecasts (sales) " + fixtures.get("minimal"));
        fixtures.put("quoted_cte", "WITH \"Years\" (\"YearKey\") AS (SELECT y FROM years) SELECT sales FROM data MODEL "
            + "DIMENSION BY (y) MEASURES (amount AS sales) (sales[FOR y IN (SELECT \"YearKey\" FROM \"Years\")] = 1)");
        fixtures.put("quoted_source", """
            SELECT "Sales" FROM (
              SELECT "a"."Y" AS "YearKey", "a"."Amount" AS "Amount" FROM "Schema"."Data" "a"
            ) "S" MODEL DIMENSION BY ("S"."YearKey" AS "Y") MEASURES ("S"."Amount" AS "Sales")
              ("Sales"[2026] = "Sales"[2025])
            """);
        fixtures.put("declarations", """
            SELECT sales FROM data d MODEL MAIN forecast
              PARTITION BY (d.country AS region)
              DIMENSION BY (d.product, "year_key")
              MEASURES (d.amount AS sales, amount AS "Mixed", 0 AS calculated)
              (sales['Bike', 2026] = 1)
            """);
        fixtures.put("rule_options", prefix + """
            RULES (UPDATE sales['Bike', 2026] = 1, sales['Bike', 2027] ORDER BY y = 2)
            """);
        fixtures.put("user_forecast", """
            SELECT sales FROM data MODEL
              REFERENCE growth ON (SELECT product, rate FROM rates) DIMENSION BY (product) MEASURES (rate)
              MAIN forecast DIMENSION BY (product, year_key) MEASURES (amount AS sales)
              (sales['Bike', FOR year_key FROM 2026 TO 2028 INCREMENT 1] =
                sales['Bike', 2025] * POWER(growth.rate['Bike'], CV(year_key) - 2025))
            """);
        fixtures.put("quoted_range", """
            SELECT "Sales" FROM data MODEL DIMENSION BY ("Year") MEASURES (amount AS "Sales")
              ("Sales"[FOR "Year" FROM 2026 TO 2028 INCREMENT 1] = "Sales"[CV("Year") - 1])
            """);
        fixtures.put("null_coordinate", "SELECT sales FROM data MODEL DIMENSION BY (y) MEASURES (amount AS sales) (sales[NULL] = sales[NULL])");
        return fixtures;
    }

    private static Statement parse(String sql) {
        var result = ParseContext.of(new OracleSpecs()).parse(Statement.class, sql);
        assertTrue(result.ok(), result::errorMessage);
        return result.value();
    }
}
