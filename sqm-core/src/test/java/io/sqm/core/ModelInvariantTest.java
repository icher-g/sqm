package io.sqm.core;

import org.junit.jupiter.api.Test;
import java.util.*;
import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class ModelInvariantTest {
    @Test
    void cellsRequireAddressesAndTargetsSupportTypedGenerators() {
        assertThrows(IllegalArgumentException.class, () -> cellRef("sales").build());
        assertThrows(IllegalArgumentException.class, () -> cellTarget("sales").build());
        assertThrows(NullPointerException.class, () -> cellRefExpr(null, null, List.of(cellAny())));
        var wildcard = cellAny();
        var generator = cellForValues("year", 2026, lit(2027));
        var target = cellTarget("sales", wildcard, generator, lit("Bike"), "Car");
        assertSame(wildcard, target.addresses().get(0));
        assertSame(generator, target.addresses().get(1));
        assertEquals(row(2026, 2027), generator.values());
        var read = cellRef(id("rates"), id("rate"), cellValue(lit(2026)));
        assertEquals(id("rates"), read.model());
        assertNull(currentDimension().dimension());
        assertEquals(id("year"), currentDimension("year").dimension());
    }

    @Test
    void tupleGeneratorsDefensivelyCopyBothCollectionLevels() {
        var dimensions = new ArrayList<>(List.of(id("year"), id("product")));
        var tuple = new ArrayList<Object>(List.of(2026, "Bike"));
        var inputRows = new ArrayList<List<Object>>();
        inputRows.add(tuple);
        var source = rows(inputRows);
        var generator = cellForValues(dimensions, source);
        dimensions.clear();
        tuple.clear();
        inputRows.clear();
        assertEquals(2, generator.dimensions().size());
        assertSame(source, generator.values());
        assertEquals(rows(row(2026, "Bike")), generator.values());
        assertThrows(UnsupportedOperationException.class, () -> source.rows().getFirst().items().clear());
        assertThrows(UnsupportedOperationException.class, () -> source.rows().clear());
        assertThrows(UnsupportedOperationException.class, () -> generator.dimensions().clear());
    }

    @Test
    void valueGeneratorsRejectEmptySourcesAndMismatchedDimensions() {
        assertThrows(IllegalArgumentException.class, () -> cellForValues(List.of(), row(1)));
        assertThrows(IllegalArgumentException.class, () -> cellForValues("year"));
        assertThrows(IllegalArgumentException.class, () -> cellForValues("year", rows(new RowExpr[0])));
        assertThrows(IllegalArgumentException.class, () -> cellForValues(List.of(id("year"), id("product")), row(1, 2)));
        assertThrows(IllegalArgumentException.class, () -> cellForValues("year", rows(row(1, 2))));
        assertThrows(IllegalArgumentException.class, () -> cellForValues("year", rows(row())));
        assertThrows(IllegalArgumentException.class, () -> cellForValues(
            List.of(id("year"), id("product")), rows(row(2026, "Bike"), row(2027))));
        assertThrows(IllegalArgumentException.class, () -> cellForValues(List.of(), select(lit(1)).build()));
        assertThrows(NullPointerException.class, () -> cellForValues("year", (ValueSet) null));
        assertThrows(NullPointerException.class, () -> cellForValues("year", (Query) null));
    }

    @Test
    void iterationAndRangeRejectKnownInvalidNumericBounds() {
        for (var value : List.of(0, -1, 0.5, Double.NaN, Double.POSITIVE_INFINITY)) {
            assertThrows(IllegalArgumentException.class, () -> iterationSpec(lit(value), null));
        }
        assertEquals(lit(1.0), iterationSpec(lit(1.0), null).limit());
        // Non-literals require dialect-aware validation; core retains them.
        assertEquals(col("n"), iterationSpec(col("n"), null).limit());
        assertEquals(lit("n"), iterationSpec(lit("n"), null).limit());
        for (var value : List.of(0, -1, Double.NaN)) {
            assertThrows(IllegalArgumentException.class, () -> cellForRange(id("year"), null,
                lit(2026), lit(2020), RangeDirection.DECREMENT, lit(value)));
        }
        var range = cellForRange(id("year"), null, lit(2020), lit(2026), RangeDirection.INCREMENT, lit(0.5));
        assertEquals(List.of(id("year")), range.dimensions());
        assertEquals(lit(0.5), range.step());
    }

    @Test
    void missingRequiredDeclarationsAndRulesFailAtConstruction() {
        assertThrows(IllegalArgumentException.class, () -> modelRules().build());
        assertThrows(IllegalArgumentException.class, () -> mainModel().build());
        assertThrows(IllegalArgumentException.class, () -> mainModel().dimension("year").build());
        assertThrows(IllegalArgumentException.class, () -> mainModel().measure("sales")
            .rule(cellTarget("sales", 2026), lit(1)).build());
        assertThrows(IllegalArgumentException.class, () -> mainModel().dimension("year")
            .rule(cellTarget("sales", 2026), lit(1)).build());
        assertThrows(IllegalArgumentException.class, () -> referenceModel("r", select(lit(1)).build()).build());
        assertThrows(IllegalArgumentException.class, () -> referenceModel("r", select(lit(1)).build()).dimension("year").build());
        assertThrows(NullPointerException.class, () -> modelColumn((Expression) null, "year"));
        assertThrows(NullPointerException.class, () -> modelColumn(lit(1), (Identifier) null));
        assertThrows(NullPointerException.class, () -> modelColumn((String) null));
        assertThrows(IllegalArgumentException.class, () -> modelColumn(""));
        assertThrows(NullPointerException.class, () -> modelClause(ReturnRows.ALL, List.of(), null));
        assertThrows(IllegalArgumentException.class, () -> modelAggregateExpr(func("SUM", col("sales")), List.of()));
        assertThrows(NullPointerException.class, () -> cellCondition(null));
    }

    @Test
    void selectorListsAndRulesAreImmutableSnapshots() {
        var selectors = new ArrayList<CellSelector>(List.of(cellValue(lit(2026))));
        var read = cellRefExpr(null, id("sales"), selectors);
        selectors.clear();
        assertEquals(1, read.selectors().size());
        assertThrows(UnsupportedOperationException.class, () -> read.selectors().clear());
        var predicate = col("year").between(2023, 2026).negated(true);
        var condition = cellCondition(predicate);
        assertSame(predicate, condition.predicate());
        assertTrue(predicate.negated());
        assertEquals(PresenceMode.NON_NULL_VALUE, presenceNonNullValue(read, lit(1), lit(0)).mode());
    }
}
