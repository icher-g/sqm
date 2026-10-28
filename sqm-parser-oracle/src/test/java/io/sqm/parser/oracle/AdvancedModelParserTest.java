package io.sqm.parser.oracle;

import io.sqm.core.*;
import io.sqm.parser.oracle.spi.OracleSpecs;
import io.sqm.parser.spi.ParseContext;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class AdvancedModelParserTest {
    private final ParseContext parser = ParseContext.of(new OracleSpecs());
    private static final String PREFIX = "SELECT sales FROM sales_data MODEL DIMENSION BY (product, year_key) MEASURES (sales) ";

    private SelectQuery query(String rules) {
        var parsed = parser.parse(Query.class, PREFIX + rules);
        assertTrue(parsed.ok(), () -> rules + ": " + parsed.errorMessage());
        return assertInstanceOf(SelectQuery.class, parsed.value());
    }

    @Test
    void parsesSymbolicPredicatesWithoutConvertingPositionalValues() {
        var parsed = query("RULES (sales[product = 'Bike', year_key BETWEEN 2025 AND 2027] = 1, "
            + "sales[product IN ('Bike', 'Car'), year_key IS NULL] = 2, "
            + "sales[product IS ANY, ANY] = 3, "
            + "sales[(product = 'Bike' OR product = 'Car'), NOT (year_key = 2025)] = 4, "
            + "sales['Bike', 2026] = 5)");
        var rules = parsed.model().main().rules().rules();
        assertEquals(cellCondition(col("product").eq(lit("Bike"))), rules.getFirst().target().addresses().getFirst());
        assertEquals(cellCondition(col("year_key").between(2025L, 2027L)), rules.getFirst().target().addresses().get(1));
        assertInstanceOf(InPredicate.class, ((CellSelector.Condition) rules.get(1).target().addresses().getFirst()).predicate());
        assertEquals(cellCondition(col("product").isAny()), rules.get(2).target().addresses().getFirst());
        assertEquals(cellAny(), rules.get(2).target().addresses().get(1));
        assertInstanceOf(OrPredicate.class, ((CellSelector.Condition) rules.get(3).target().addresses().getFirst()).predicate());
        assertInstanceOf(CellSelector.Value.class, rules.get(4).target().addresses().getFirst());
    }

    @Test
    void parsesEveryForSourceAndPreservesTupleShape() {
        var rules = query("RULES UPSERT ALL ("
            + "sales[FOR product IN ('Bike'), FOR year_key FROM 2025 TO 2027 INCREMENT 1] = 1, "
            + "sales[FOR product LIKE 'P%' FROM 3 TO 1 DECREMENT 1, 2026] = 2, "
            + "sales[FOR (product, year_key) IN (('Bike', 2026), ('Car', 2027))] = 3, "
            + "sales[FOR (product, year_key) IN (SELECT p, y FROM forecast)] = 4, "
            + "sales['Bike', FOR year_key IN (SELECT y FROM forecast)] = 5)"
        ).model().main().rules();
        assertEquals(RuleMode.UPSERT_ALL, rules.defaultMode());
        var first = rules.rules().getFirst().target().addresses();
        assertEquals(cellForValues("product", "Bike"), first.getFirst());
        assertEquals(cellForRange("year_key", 2025L, 2027L, RangeDirection.INCREMENT, 1L), first.get(1));
        var template = assertInstanceOf(CellFor.Range.class, rules.rules().get(1).target().addresses().getFirst());
        assertEquals(lit("P%"), template.likePattern());
        assertEquals(RangeDirection.DECREMENT, template.direction());
        var tuples = assertInstanceOf(CellFor.Values.class, rules.rules().get(2).target().addresses().getFirst());
        assertEquals(List.of(id("product"), id("year_key")), tuples.dimensions());
        assertEquals(rows(row("Bike", 2026L), row("Car", 2027L)), tuples.values());
        assertInstanceOf(QueryExpr.class, ((CellFor.Values) rules.rules().get(3).target().addresses().getFirst()).values());
        assertInstanceOf(QueryExpr.class, ((CellFor.Values) rules.rules().get(4).target().addresses().get(1)).values());
    }

    @Test
    void keepsAggregatesNestedCellsAndSpecialFunctionsTyped() {
        var rules = query("RULES ITERATE (10) UNTIL (ABS(PREVIOUS(sales['Bike', 2026]) - sales['Bike', 2026]) < 1) ("
            + "sales[ANY, 2026] ORDER BY product DESC = SUM(sales)[ANY, year_key BETWEEN 2023 AND 2025], "
            + "sales['Bike', 2027] = sales['Bike', sales['Bike', 2025]] + ITERATION_NUMBER, "
            + "sales['Car', 2027] = PRESENTV(sales['Car', 2026], CV(year_key) + 1, 0), "
            + "sales['Car', 2028] = PRESENTNNV(sales['Car', 2026], CV(), 0), "
            + "sales['Car', 2029] = CASE WHEN sales['Car', 2026] IS PRESENT THEN 1 ELSE 0 END)"
        ).model().main().rules();
        assertNotNull(rules.iteration().until());
        assertEquals(lit(10L), rules.iteration().limit());
        assertNotNull(rules.rules().getFirst().orderBy());
        var aggregate = assertInstanceOf(ModelAggregateExpr.class, rules.rules().getFirst().value());
        assertEquals("SUM", aggregate.aggregate().name().values().getFirst());
        var sum = assertInstanceOf(AddArithmeticExpr.class, rules.rules().get(1).value());
        var cell = assertInstanceOf(CellRefExpr.class, sum.lhs());
        assertInstanceOf(CellRefExpr.class, ((CellSelector.Value) cell.selectors().get(1)).value());
        assertInstanceOf(IterationNumberExpr.class, sum.rhs());
        assertEquals(PresenceMode.CELL, ((PresenceValueExpr) rules.rules().get(2).value()).mode());
        assertEquals(PresenceMode.NON_NULL_VALUE, ((PresenceValueExpr) rules.rules().get(3).value()).mode());
        assertInstanceOf(CaseExpr.class, rules.rules().get(4).value());
    }

    @Test
    void resolvesQuotedReferenceAndMainNamesWithEffectiveOptions() {
        var sql = "SELECT sales FROM t MODEL IGNORE NAV UNIQUE SINGLE REFERENCE "
            + "REFERENCE \"Rates\" ON (SELECT code, rate FROM fx) DIMENSION BY (code) MEASURES (rate) "
            + "KEEP NAV UNIQUE DIMENSION MAIN \"Forecast\" DIMENSION BY (product, year_key) MEASURES (sales) "
            + "RULES (sales['Bike', 2026] = \"Forecast\".sales['Bike', 2025] * \"Rates\".rate['USD'])";
        var result = parser.parse(SelectQuery.class, sql);
        assertTrue(result.ok(), result::errorMessage);
        var model = result.value().model();
        var reference = model.references().getFirst();
        assertEquals(id("Rates", QuoteStyle.DOUBLE_QUOTE), reference.name());
        assertEquals(NavigationMode.KEEP, reference.navigationMode());
        assertEquals(UniquenessMode.DIMENSION, reference.uniquenessMode());
        assertEquals(NavigationMode.IGNORE, model.main().navigationMode());
        var product = assertInstanceOf(MulArithmeticExpr.class, model.main().rules().rules().getFirst().value());
        assertEquals(id("Forecast", QuoteStyle.DOUBLE_QUOTE), ((CellRefExpr) product.lhs()).model());
        assertEquals(id("Rates", QuoteStyle.DOUBLE_QUOTE), ((CellRefExpr) product.rhs()).model());
        assertTrue(parser.callstack().isEmpty());
    }

    @Test
    void rejectsMalformedAdvancedGrammar() {
        for (var rules : List.of(
            "RULES ITERATE () (sales['Bike', 2026] = 1)",
            "RULES ITERATE (0) (sales['Bike', 2026] = 1)",
            "RULES ITERATE (1.5) (sales['Bike', 2026] = 1)",
            "RULES ITERATE (1) UNTIL () (sales['Bike', 2026] = 1)",
            "RULES (sales[FOR product IN (), 2026] = 1)",
            "RULES (sales[FOR (product, year_key) IN (('Bike'))] = 1)",
            "RULES (sales['Bike', FOR year_key FROM 1 TO 2 INCREMENT 0] = 1)",
            "RULES (sales['Bike', 2026] = sales['Bike', FOR year_key IN (1)])",
            "RULES (sales['Bike', 2026] = sales['Bike', (SELECT y FROM t)])",
            "RULES (sales[product IN (SELECT p FROM t), 2026] = 1)",
            "RULES (sales['Bike', 2026] ORDER BY 1 = 1)",
            "RULES (sales['Bike', 2026] = PREVIOUS(1))",
            "RULES (sales['Bike', 2026] = PRESENTV(sales['Bike', 2025], 1))",
            "RULES (sales['Bike', 2026] = CV(1))",
            "RULES (sales['Bike', 2026] = ITERATION_NUMBER())",
            "RULES (sales['Bike', 2026] = unknown.sales[1])",
            "RULES (sales['Bike', 2026] = sales[1][2])"
        )) {
            var result = parser.parse(Query.class, PREFIX + rules);
            assertTrue(result.isError(), rules);
            assertFalse(result.errorMessage().contains("#506"), result::errorMessage);
            assertTrue(parser.callstack().isEmpty());
        }
    }

    @Test
    void keepsModelSyntaxOutOfOrdinaryAndNestedQueryScopes() {
        for (var sql : List.of(
            "SELECT CV() FROM t",
            "SELECT ITERATION_NUMBER FROM t",
            "SELECT PREVIOUS(sales[1]) FROM t",
            "SELECT PRESENTV(sales[1], 1, 0) FROM t",
            "SELECT * FROM t WHERE product IS ANY",
            "SELECT * FROM t WHERE sales IS PRESENT",
            PREFIX + "RULES (sales['Bike', 2026] = (SELECT CV() FROM t))",
            PREFIX + "RULES (sales['Bike', FOR year_key IN (SELECT CV() FROM t)] = 1)"
        )) {
            var parsed = parser.parse(Query.class, sql);
            assertTrue(parsed.isError(), sql);
            assertTrue(parser.callstack().isEmpty());
        }
        assertTrue(parser.parse(Query.class, "SELECT * FROM t WHERE product = ANY (SELECT product FROM t)").ok());
        query("RULES (sales['Bike', 2026] = CV(year_key))");
    }

    @Test
    void leavesValueAndQuerySemanticsToTheValidator() {
        for (var rules : List.of(
            "RULES ITERATE (-1) (sales['Bike', 2026] = 1)",
            "RULES ITERATE (n) (sales['Bike', 2026] = 1)",
            "RULES AUTOMATIC ORDER ITERATE (1) (sales['Bike', 2026] = 1)",
            "RULES (sales[FOR (product, year_key) IN (SELECT p FROM t)] = 1)",
            "RULES (sales['Bike', FOR year_key FROM 1 TO 2 INCREMENT -1] = 1)",
            "RULES (sales['Bike', FOR year_key FROM 1 TO 10001 INCREMENT 1] = 1)",
            "RULES (sales[FOR product LIKE 'P' FROM 1 TO 2 INCREMENT 1, 2026] = 1)",
            "RULES (sales['Bike', FOR year_key IN (SELECT outside.y FROM t)] = 1)")) {
            query(rules);
        }
        var reference = parser.parse(Query.class, "SELECT sales FROM t MODEL "
            + "REFERENCE r ON (SELECT outside.y, rate FROM rates) DIMENSION BY (y) MEASURES (rate) "
            + "DIMENSION BY (y) MEASURES (sales) (sales[1] = r.rate[1])");
        assertTrue(reference.ok(), reference::errorMessage);
    }
}
