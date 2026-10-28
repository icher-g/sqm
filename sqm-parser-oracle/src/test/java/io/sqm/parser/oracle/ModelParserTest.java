package io.sqm.parser.oracle;

import io.sqm.core.*;
import io.sqm.core.dialect.SqlDialectVersion;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.core.oracle.dialect.OracleCapabilities;
import io.sqm.parser.ansi.AnsiSpecs;
import io.sqm.parser.oracle.spi.OracleSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class ModelParserTest {
    private static final String COLUMNS = "DIMENSION BY (year_key) MEASURES (amount AS sales)";
    private static final String RULE = "(sales[2026] = sales[2025] * 1.1)";
    private final ParseContext ctx = ParseContext.of(new OracleSpecs());

    private SelectQuery query(String clause) {
        var parsed = ctx.parse(Query.class, "SELECT sales FROM sales_data MODEL " + clause);
        assertTrue(parsed.ok(), parsed::errorMessage);
        return assertInstanceOf(SelectQuery.class, parsed.value());
    }

    @Test
    void parsesMinimalModelAndNormalizesDeclarations() {
        var main = query(COLUMNS + " " + RULE).model().main();
        assertEquals(List.of(modelColumn("year_key")), main.dimensions());
        assertEquals(List.of(modelColumn("amount", "sales")), main.measures());
        assertEquals(NavigationMode.KEEP, main.navigationMode());
        assertEquals(UniquenessMode.DIMENSION, main.uniquenessMode());
        assertEquals(RuleMode.UPSERT, main.rules().defaultMode());
        assertEquals(RuleOrder.SEQUENTIAL, main.rules().order());
        assertEquals(cellTarget("sales", 2026L), main.rules().rules().getFirst().target());
        assertEquals(cellRef("sales", 2025L).mul(lit(1.1)), main.rules().rules().getFirst().value());
        assertNull(main.rules().rules().getFirst().mode());
    }

    @Test
    void canonicalizesClauseDefaultsAndMainOverrides() {
        var inherited = query("IGNORE NAV UNIQUE SINGLE REFERENCE RETURN UPDATED ROWS MAIN forecast "
            + "PARTITION BY (country) " + COLUMNS + " RULES UPDATE AUTOMATIC ORDER " + RULE);
        var local = query("RETURN UPDATED ROWS MAIN forecast PARTITION BY (country) " + COLUMNS
            + " IGNORE NAV UNIQUE SINGLE REFERENCE RULES UPDATE AUTOMATIC ORDER " + RULE);
        assertEquals(inherited, local);
        assertEquals(ReturnRows.UPDATED, local.model().returnRows());
        assertEquals(id("forecast"), local.model().main().name());
        var reset = query("IGNORE NAV UNIQUE SINGLE REFERENCE " + COLUMNS
            + " KEEP NAV UNIQUE DIMENSION RULES UPSERT SEQUENTIAL ORDER " + RULE);
        assertEquals(query(COLUMNS + " " + RULE), reset);
        assertEquals(query(COLUMNS + " " + RULE),
            query("RETURN ALL ROWS " + COLUMNS + " " + RULE));
    }

    @Test
    void parsesEveryModeAndMultipleRules() {
        for (var mode : RuleMode.values()) {
            var sqlMode = mode.name().replace('_', ' ');
            var parsed = query(COLUMNS + " RULES " + sqlMode
                + " (" + sqlMode + " sales[2026] = 1, sales[2027] = 2)");
            assertEquals(mode, parsed.model().main().rules().defaultMode());
            assertEquals(mode, parsed.model().main().rules().rules().getFirst().mode());
            assertEquals(2, parsed.model().main().rules().rules().size());
        }
    }

    @Test
    void parsesComputedQuotedAndQualifiedDeclarations() {
        var parsed = query("DIMENSION BY (s.year_key, product AS \"Product\") "
            + "MEASURES (amount * 2 \"Sales\") (\"Sales\"[2026, 'Bike'] = ABS(\"Sales\"[2025, 'Bike']))");
        var main = parsed.model().main();
        assertEquals(id("year_key"), main.dimensions().getFirst().name());
        assertEquals(id("Product", QuoteStyle.DOUBLE_QUOTE), main.dimensions().get(1).name());
        assertEquals(id("Sales", QuoteStyle.DOUBLE_QUOTE), main.measures().getFirst().name());
        assertEquals(main.measures().getFirst().name(), main.rules().rules().getFirst().target().measure());
        assertTrue(ctx.parse(ModelColumn.class, "amount + 1").errorMessage().contains("alias"));
        assertTrue(ctx.parse(ModelColumn.class, "amount AS").isError());
    }

    @Test
    void respectsQueryPositionAndScopedNames() {
        var parsed = ctx.parse(Query.class, "SELECT sales FROM sales_data GROUP BY year_key, amount HAVING amount > 0 "
            + "MODEL " + COLUMNS + " " + RULE + " ORDER BY sales");
        assertTrue(parsed.ok(), parsed::errorMessage);
        assertNotNull(assertInstanceOf(SelectQuery.class, parsed.value()).orderBy());
        assertTrue(ctx.parse(Query.class, "SELECT sales FROM sales_data ORDER BY sales MODEL " + COLUMNS + RULE).isError());
        assertTrue(ctx.parse(Query.class, "SELECT sales[2025] FROM sales_data").isError());
        assertTrue(ctx.parse(Query.class, "SELECT sales FROM sales_data MODEL " + COLUMNS
            + " (missing[1] = 2)").errorMessage().contains("Unknown MODEL measure"));
        assertTrue(ctx.parse(Query.class, "SELECT sales FROM sales_data MODEL " + COLUMNS
            + " (sales[1] = missing[2])").errorMessage().contains("Unknown MODEL measure"));
        assertTrue(ctx.parse(Query.class, "SELECT sales FROM sales_data MODEL " + COLUMNS
            + " (sales[1] = (SELECT sales[2] FROM other_data))").isError());
        assertTrue(ctx.callstack().isEmpty());
        query(COLUMNS + RULE);
        assertTrue(ctx.parse(Query.class, "SELECT \"reference\", \"present\", \"main\" FROM t").ok());
    }

    @Test
    void rejectsMalformedClauses() {
        for (var clause : List.of(
            "", "MAIN", "RETURN ROWS " + COLUMNS + RULE, "RETURN UPDATED " + COLUMNS + RULE,
            "KEEP " + COLUMNS + RULE, "UNIQUE REFERENCE " + COLUMNS + RULE,
            "UNIQUE SINGLE " + COLUMNS + RULE,
            "KEEP NAV IGNORE NAV " + COLUMNS + RULE,
            "UNIQUE DIMENSION KEEP NAV " + COLUMNS + RULE,
            "UNIQUE DIMENSION UNIQUE DIMENSION " + COLUMNS + RULE,
            "PARTITION (country) " + COLUMNS + RULE,
            "PARTITION BY () " + COLUMNS + RULE,
            "DIMENSION (year_key) MEASURES (sales) " + RULE,
            "DIMENSION BY () MEASURES (sales) " + RULE,
            "DIMENSION BY (year_key,) MEASURES (sales) " + RULE,
            "DIMENSION BY (year_key) MEASURES () " + RULE,
            "MEASURES (sales) DIMENSION BY (year_key) " + RULE,
            COLUMNS + " RULES ()", COLUMNS + " RULES AUTOMATIC " + RULE,
            COLUMNS + " RULES AUTOMATIC ORDER UPDATE " + RULE,
            COLUMNS + " RULES UPDATE UPDATE " + RULE,
            COLUMNS + " (sales[] = 1)", COLUMNS + " (sales[1,] = 1)",
            COLUMNS + " (sales[1] 1)", COLUMNS + " (sales[1] = )",
            COLUMNS + " (sales[1] = 1,)", COLUMNS + " (sales[1 = 1)",
            COLUMNS + " " + RULE + " RETURN ALL ROWS", COLUMNS + " " + RULE + " MODEL " + COLUMNS + RULE)) {
            var parsed = ctx.parse(Query.class, "SELECT sales FROM sales_data MODEL " + clause);
            assertTrue(parsed.isError(), clause);
        }
    }

    @Test
    void parsesFormerlyDeferredAdvancedSyntax() {
        for (var clause : List.of(
            "REFERENCE baseline ON (SELECT year_key, amount FROM t) " + COLUMNS + " " + COLUMNS + RULE,
            COLUMNS + " RULES ITERATE (2) " + RULE,
            COLUMNS + " (sales[FOR year_key IN (2025, 2026)] = 1)",
            COLUMNS + " (sales[ANY] = 1)",
            COLUMNS + " (sales[year_key = 2026] = 1)",
            COLUMNS + " (sales[year_key IN (2025)] = 1)",
            COLUMNS + " (sales[year_key BETWEEN 2025 AND 2026] = 1)",
            COLUMNS + " (sales[year_key IS NULL] = 1)",
            COLUMNS + " (sales[1] ORDER BY year_key = 1)",
            COLUMNS + " (sales[1] = SUM(sales)[2])",
            COLUMNS + " (sales[1] = CV())",
            COLUMNS + " (sales[1] = ITERATION_NUMBER)",
            COLUMNS + " (sales[1] = PREVIOUS(sales[2]))",
            COLUMNS + " (sales[1] = PRESENTV(sales[2], 1, 0))",
            COLUMNS + " (sales[1] = PRESENTNNV(sales[2], 1, 0))")) {
            var parsed = ctx.parse(Query.class, "SELECT sales FROM sales_data MODEL " + clause);
            assertTrue(parsed.ok(), () -> clause + ": " + parsed.errorMessage());
        }
    }

    @Test
    void enablesModelForEveryOracleBaselineAndRejectsAnsi() {
        for (var version : List.of(SqlDialectVersion.of(11, 2), SqlDialectVersion.of(12, 1),
            SqlDialectVersion.of(19, 0), SqlDialectVersion.of(23, 0))) {
            assertTrue(OracleCapabilities.of(version).supports(SqlFeature.MODEL_CLAUSE));
            var parsed = ParseContext.of(new OracleSpecs(version)).parse(Query.class,
                "SELECT sales FROM t MODEL " + COLUMNS + RULE);
            assertTrue(parsed.ok(), parsed::errorMessage);
        }
        var result = ParseContext.of(new AnsiSpecs()).parse(Query.class, "SELECT sales FROM t MODEL " + COLUMNS + RULE);
        assertTrue(result.isError());
        assertTrue(result.errorMessage().contains("MODEL"));
    }
}
