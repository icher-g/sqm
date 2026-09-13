package io.sqm.core;

import io.sqm.core.match.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class ModelMatchTest {
    @Test
    void matchesEveryCellSelectorVariantAndPreservesFirstMatch() {
        for (CellSelector node : List.of(cellValue(lit(2026)), cellCondition(col("year").eq(2026)))) {
            assertSame(node, node.<CellSelector>matchCellSelector()
                .value(value -> value).condition(value -> value)
                .otherwise(value -> fail("Unmatched selector")));
            assertSame(node, node.<CellSelector>matchCellSelector().otherwise(value -> value));
        }
        assertNull(cellValue(lit(2026)).<String>matchCellSelector()
            .value(value -> null).value(value -> fail("First match must win"))
            .otherwise(value -> fail("A null result still matches")));
        assertNull(cellCondition(col("year").eq(2026)).<String>matchCellSelector()
            .condition(value -> null).condition(value -> fail("First match must win"))
            .otherwise(value -> fail("A null result still matches")));
        assertThrows(NullPointerException.class, () -> CellSelectorMatch.match(null));
    }

    @Test
    void matchesEveryCellForVariantAndPreservesFirstMatch() {
        for (CellFor node : List.<CellFor>of(
            cellForValues(List.of(id("year"), id("product")), rows(row(2026, "Bike"))),
            cellForValues(List.of(id("year")), select(lit(2026)).build()),
            cellForRange(id("year"), lit("Y%"), lit(2020), lit(2026), RangeDirection.INCREMENT, lit(1)))) {
            assertSame(node, node.<CellFor>matchCellFor()
                .values(value -> value)
                .range(value -> value)
                .otherwise(value -> fail("Unmatched variant: " + value)));
            assertSame(node, node.<CellFor>matchCellFor().otherwise(value -> value));
        }
        assertNull((cellForValues(List.of(id("year"), id("product")), rows(row(2026, "Bike")))).<String>matchCellFor()
            .values(value -> null).values(value -> fail("First match must win"))
            .otherwise(value -> fail("A null result still matches")));
        assertNull((cellForValues(List.of(id("year")), select(lit(2026)).build())).<String>matchCellFor()
            .values(value -> null).values(value -> fail("First match must win"))
            .otherwise(value -> fail("A null result still matches")));
        assertNull((cellForRange(id("year"), lit("Y%"), lit(2020), lit(2026), RangeDirection.INCREMENT, lit(1))).<String>matchCellFor()
            .range(value -> null).range(value -> fail("First match must win"))
            .otherwise(value -> fail("A null result still matches")));
        assertThrows(NullPointerException.class, () -> CellForMatch.match(null));
    }

    @Test
    void matchesSelectorAndGeneratorAddresses() {
        for (CellAddress address : List.of(cellAny(), cellForValues("year", 2026))) {
            assertSame(address, address.<CellAddress>matchCellAddress().selector(value -> value)
                .generator(value -> value).otherwise(value -> fail("Unmatched address")));
            assertSame(address, address.<CellAddress>matchCellAddress().otherwise(value -> value));
        }
        assertNull(cellAny().<String>matchCellAddress().selector(value -> null)
            .selector(value -> fail("First match must win")).otherwise(value -> fail()));
        assertNull(cellForValues("year", 2026).<String>matchCellAddress().generator(value -> null)
            .generator(value -> fail("First match must win")).otherwise(value -> fail()));
        assertThrows(NullPointerException.class, () -> CellAddressMatch.match(null));
    }

    @Test
    void matchesModelExpressionsAndPresencePredicates() {
        assertEquals("matched", (cellRef("sales", 2026)).<String>matchExpression()
            .cellRefExpr(value -> "matched").cellRefExpr(value -> fail("First match must win"))
            .otherwise(value -> fail("Model expression not matched")));
        assertEquals("fallback", lit(0).<String>matchExpression()
            .cellRefExpr(value -> fail("Wrong variant")).otherwise(value -> "fallback"));
        assertEquals("matched", (modelAggregateExpr(func("SUM", col("sales")), List.of(cellAny()))).<String>matchExpression()
            .modelAggregateExpr(value -> "matched").modelAggregateExpr(value -> fail("First match must win"))
            .otherwise(value -> fail("Model expression not matched")));
        assertEquals("fallback", lit(0).<String>matchExpression()
            .modelAggregateExpr(value -> fail("Wrong variant")).otherwise(value -> "fallback"));
        assertEquals("matched", (currentDimension("year")).<String>matchExpression()
            .currentDimensionExpr(value -> "matched").currentDimensionExpr(value -> fail("First match must win"))
            .otherwise(value -> fail("Model expression not matched")));
        assertEquals("fallback", lit(0).<String>matchExpression()
            .currentDimensionExpr(value -> fail("Wrong variant")).otherwise(value -> "fallback"));
        assertEquals("matched", (iterationNumber()).<String>matchExpression()
            .iterationNumberExpr(value -> "matched").iterationNumberExpr(value -> fail("First match must win"))
            .otherwise(value -> fail("Model expression not matched")));
        assertEquals("fallback", lit(0).<String>matchExpression()
            .iterationNumberExpr(value -> fail("Wrong variant")).otherwise(value -> "fallback"));
        assertEquals("matched", (previousModelValueExpr(cellRef("sales", 2026))).<String>matchExpression()
            .previousModelValueExpr(value -> "matched").previousModelValueExpr(value -> fail("First match must win"))
            .otherwise(value -> fail("Model expression not matched")));
        assertEquals("fallback", lit(0).<String>matchExpression()
            .previousModelValueExpr(value -> fail("Wrong variant")).otherwise(value -> "fallback"));
        assertEquals("matched", (presenceValue(cellRef("sales", 2026), lit(10), lit(0))).<String>matchExpression()
            .presenceValueExpr(value -> "matched").presenceValueExpr(value -> fail("First match must win"))
            .otherwise(value -> fail("Model expression not matched")));
        assertEquals("fallback", lit(0).<String>matchExpression()
            .presenceValueExpr(value -> fail("Wrong variant")).otherwise(value -> "fallback"));
        assertEquals("matched", (cellIsPresent(cellRef("sales", 2026))).<String>matchPredicate()
            .cellPresentPredicate(value -> "matched").cellPresentPredicate(value -> fail("First match must win"))
            .otherwise(value -> fail("Model expression not matched")));
        assertEquals("fallback", col("x").isNull().<String>matchPredicate()
            .cellPresentPredicate(value -> fail("Wrong variant")).otherwise(value -> "fallback"));
    }
}
