package io.sqm.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class CellDslBuilderTest {
    @Test
    void fluentCellsReadLikeTheirCoordinatesAndMatchCompactHelpers() {
        var read = cellRef("sales").selector("Bike").selector(2025).build();
        var target = cellTarget("sales").address("Bike").address(2026).build();
        assertEquals(cellRef("sales", "Bike", 2025), read);
        assertEquals(cellTarget("sales", "Bike", 2026), target);
        var calculation = model().dimension("product").dimension("year").measure("sales")
            .rule(target, read.mul(lit(1.1))).build();
        assertEquals(target, calculation.main().rules().rules().getFirst().target());

        // Two strings still mean measure + coordinate, never model + measure.
        assertEquals(cellRef("sales").selector("Bike").build(), cellRef("sales", "Bike"));
        assertEquals(cellTarget("sales").address("Bike").build(), cellTarget("sales", "Bike"));
    }

    @Test
    void qualificationAndQuotedIdentifiersDoNotRequireSelectorLists() {
        var measure = id("Total Sales", QuoteStyle.DOUBLE_QUOTE);
        var qualifier = id("Baseline Model", QuoteStyle.DOUBLE_QUOTE);
        var qualified = cellRefExpr(qualifier, measure).selector(2026).build();
        assertEquals(qualifier, qualified.model());
        assertEquals(measure, qualified.measure());
        assertEquals(qualified, cellRef(measure).model(qualifier).selector(2026).build());
        assertEquals(id("baseline"), cellRef("sales").model("baseline").selector(2026).build().model());
        assertNull(CellRefExpr.builder(qualified).clearModel().build().model());
        assertNull(CellRefExpr.builder(qualified).model((Identifier) null).build().model());
        assertEquals(measure, cellTarget(measure).address(2026).build().measure());
        assertEquals(cellRef("sales", 2026), CellRefExpr.builder().measure("sales").selector(2026).build());
        assertEquals(cellTarget("sales", 2026), CellTarget.builder().measure("sales").address(2026).build());
    }

    @Test
    void selectorsPreservePredicatesExpressionsExplicitValuesAndSqlNull() {
        var predicate = col("year").between(2025, 2026);
        var expression = currentDimension("year");
        var explicit = cellValue(predicate);
        var read = cellRef("sales").selector("Bike").selector(predicate).selector(expression)
            .selector(isAny()).selector(cellAny()).selector(explicit).selector(null).build();
        assertEquals(cellRef("sales", "Bike", predicate, expression, isAny(), cellAny(), explicit, null), read);
        assertEquals(cellCondition(predicate), read.selectors().get(1));
        assertSame(expression, ((CellSelector.Value) read.selectors().get(2)).value());
        assertSame(explicit, read.selectors().get(5));
        assertSame(predicate, explicit.value());
        assertEquals(lit(null), ((CellSelector.Value) read.selectors().get(6)).value());
        assertEquals(cellValue(lit("Bike")), cellValue("Bike"));
        assertEquals(cellValue(lit(2026)), cellValue(2026));
        assertEquals(cellValue(lit(null)), cellValue(null));
        assertEquals(cellRef("sales", (Object) null), cellRef("sales").selector(null).build());
    }

    @Test
    void onlyWritableAddressesAcceptForGenerators() {
        var generator = cellForValues("year", 2026, 2027);
        var predicate = col("product").isAny();
        var explicit = cellValue(2026);
        var target = cellTarget("sales").address(predicate).address(generator).address(explicit)
            .address(null).build();
        assertEquals(cellTarget("sales", predicate, generator, explicit, null), target);
        assertSame(generator, target.addresses().get(1));
        assertSame(explicit, target.addresses().get(2));
        assertThrows(IllegalArgumentException.class, () -> cellRef("sales").selector(generator));
        assertThrows(IllegalArgumentException.class, () -> cellRef("sales").selector(tbl("input")));
        assertThrows(IllegalArgumentException.class, () -> cellTarget("sales").address(tbl("input")));
        assertThrows(IllegalArgumentException.class, () -> cellValue(generator));
        assertThrows(IllegalArgumentException.class, () -> cellValue(cellAny()));
    }

    @Test
    void readBuildersCopyListsAndProduceIndependentSnapshots() {
        var inputs = new ArrayList<CellSelector>(List.of(cellValue(2025)));
        var builder = cellRef("sales").selectors(inputs);
        inputs.clear();
        var original = builder.build();
        assertEquals(cellRef("sales", 2025), original);
        assertEquals(cellRef("sales", 2025, 2026), builder.selector(2026).build());
        assertEquals(cellRef("sales", 2027), builder.clearSelectors().selector(2027).build());
        assertEquals(cellRef("sales", 2030), builder.selectors(List.of(cellValue(2030))).build());
        assertThrows(NullPointerException.class, () -> builder.selectors(Arrays.asList(cellValue(1), null)));
        assertEquals(cellRef("sales", 2030), builder.build());
        assertEquals(original, CellRefExpr.builder(original).build());
        assertEquals(cellRef("profit", 2028), CellRefExpr.builder(original).measure("profit")
            .clearSelectors().selector(2028).build());
        assertEquals(cellRef("sales", 2025), original);
        assertThrows(UnsupportedOperationException.class, () -> original.selectors().clear());
    }

    @Test
    void targetBuildersCopyListsAndProduceIndependentSnapshots() {
        var inputs = new ArrayList<CellAddress>(List.of(cellValue(2025)));
        var builder = cellTarget("sales").addresses(inputs);
        inputs.clear();
        var original = builder.build();
        assertEquals(cellTarget("sales", 2025), original);
        assertEquals(cellTarget("sales", 2025, 2026), builder.address(2026).build());
        assertEquals(cellTarget("sales", 2027), builder.clearAddresses().address(2027).build());
        assertEquals(cellTarget("sales", 2030), builder.addresses(List.of(cellValue(2030))).build());
        assertThrows(NullPointerException.class, () -> builder.addresses(Arrays.asList(cellValue(1), null)));
        assertEquals(cellTarget("sales", 2030), builder.build());
        assertEquals(original, CellTarget.builder(original).build());
        assertEquals(cellTarget("profit", 2028), CellTarget.builder(original).measure("profit")
            .clearAddresses().address(2028).build());
        assertEquals(cellTarget("sales", 2025), original);
        assertThrows(UnsupportedOperationException.class, () -> original.addresses().clear());
    }

    @Test
    void incompleteBuildersCannotProduceInvalidCells() {
        assertThrows(NullPointerException.class, () -> CellRefExpr.builder().selector(2026).build());
        assertThrows(NullPointerException.class, () -> CellTarget.builder().address(2026).build());
        assertThrows(IllegalArgumentException.class, () -> cellRef("sales").build());
        assertThrows(IllegalArgumentException.class, () -> cellTarget("sales").build());
        assertThrows(IllegalArgumentException.class, () -> cellRef("sales").selector(1).clearSelectors().build());
        assertThrows(IllegalArgumentException.class, () -> cellTarget("sales").address(1).clearAddresses().build());
        assertThrows(IllegalArgumentException.class, () -> cellRef("sales").selectors(List.of()).build());
        assertThrows(IllegalArgumentException.class, () -> cellTarget("sales").addresses(List.of()).build());
        assertThrows(IllegalArgumentException.class, () -> cellRef("sales", new Object[0]));
        assertThrows(IllegalArgumentException.class, () -> cellTarget("sales", new Object[0]));
        assertThrows(NullPointerException.class, () -> CellRefExpr.builder(null));
        assertThrows(NullPointerException.class, () -> CellTarget.builder(null));
        assertThrows(NullPointerException.class, () -> cellRef("sales").measure((Identifier) null));
        assertThrows(NullPointerException.class, () -> cellTarget("sales").measure((Identifier) null));
        assertThrows(NullPointerException.class, () -> cellRef("sales").selectors(null));
        assertThrows(NullPointerException.class, () -> cellTarget("sales").addresses(null));
    }
}
