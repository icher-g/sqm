package io.sqm.core;

import io.sqm.core.transform.RecursiveNodeTransformer;
import io.sqm.core.walk.RecursiveNodeVisitor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class ModelPredicateSelectorTest {
    @Test
    void existingPredicatesAreWrappedAsConditionsWithoutCopying() {
        var year = col("sales_year");
        for (Predicate predicate : List.of(year.eq(2026), year.between(2023, 2026),
            year.in(2025, 2026), year.isNull(), year.isNotNull(),
            year.gt(2020).and(year.lt(2027)), year.eq(2025).or(year.eq(2026)),
            not(year.eq(2024)), year.between(2020, 2021).negated(true),
            year.notIn(2020, 2021), year.isAny(), isAny())) {
            var read = cellRef("sales", "Bike", predicate);
            var target = cellTarget("sales", "Bike", predicate);
            var readSelector = assertInstanceOf(CellSelector.Condition.class, read.selectors().get(1));
            var targetSelector = assertInstanceOf(CellSelector.Condition.class, target.addresses().get(1));
            assertSame(predicate, readSelector.predicate());
            assertSame(predicate, targetSelector.predicate());
            assertEquals(cellCondition(predicate), readSelector);
        }
    }

    @Test
    void positionalAndSymbolicNullSelectorsRemainDifferent() {
        var positional = cellRef("sales", (Object) null).selectors().getFirst();
        var symbolic = cellRef("sales", col("year").eq(lit(null))).selectors().getFirst();
        assertInstanceOf(CellSelector.Value.class, positional);
        assertInstanceOf(CellSelector.Condition.class, symbolic);
        assertNotEquals(positional, symbolic);
    }

    @Test
    void wildcardHelpersPreserveImplicitAndExplicitDimensionInformation() {
        assertNull(isAny().dimension());
        assertEquals(col("product"), isAny("product").dimension());
        var quoted = col(id("Product Name", QuoteStyle.DOUBLE_QUOTE));
        assertSame(quoted, isAny(quoted).dimension());
        assertEquals(isAny(quoted), quoted.isAny());
        assertEquals(cellCondition(isAny()), cellAny());
        assertThrows(NullPointerException.class, () -> isAny((Expression) null));
        assertThrows(NullPointerException.class, () -> isAny((String) null));
        assertThrows(NullPointerException.class, () -> cellCondition(null));
    }

    @Test
    void wildcardMatcherIsDifferentFromQuantifiedAny() {
        var wildcard = isAny("product");
        var quantified = col("price").eqAny(select(col("price")).from(tbl("offers")).build());
        assertEquals("wildcard", wildcard.<String>matchPredicate()
            .anyAll(p -> fail("A wildcard is not a quantified comparison"))
            .isAny(p -> "wildcard").isAny(p -> fail("First match must win"))
            .otherwise(p -> fail("Missing wildcard handler")));
        assertEquals("quantified", quantified.<String>matchPredicate()
            .isAny(p -> fail("A quantified comparison is not a wildcard"))
            .anyAll(p -> "quantified").otherwise(p -> fail("Missing quantified handler")));
        assertNull(wildcard.<String>matchPredicate().isAny(p -> null)
            .isAny(p -> fail("A null result still matches")).otherwise(p -> fail()));
    }

    @Test
    void visitorsAndTransformersReachPredicateOperandsAndWildcardDimensions() {
        var query = select(col("sales")).model(model().dimension("year").measure("sales")
            .rule(cellTarget("sales", col("year").between(2023, 2026)),
                cellRef("sales", col("year").isAny())).build()).build();
        var seen = new ArrayList<ColumnExpr>();
        query.accept(new RecursiveNodeVisitor<Void>() {
            @Override protected Void defaultResult() { return null; }
            @Override public Void visitColumnExpr(ColumnExpr column) {
                seen.add(column);
                return null;
            }
        });
        assertEquals(3, seen.stream().filter(c -> c.name().equals(id("year"))).count());
        assertSame(query, new RecursiveNodeTransformer() {}.transform(query));
        var rewrite = new RecursiveNodeTransformer() {
            @Override public Node visitColumnExpr(ColumnExpr column) {
                return column.name().equals(id("year")) ? col("renamed_year") : column;
            }
        };
        var changed = (SelectQuery) rewrite.transform(query);
        var rule = changed.model().main().rules().rules().getFirst();
        var target = (CellSelector.Condition) rule.target().addresses().getFirst();
        assertEquals(col("renamed_year"), ((BetweenPredicate) target.predicate()).value());
        var read = (CellRefExpr) rule.value();
        var selector = (CellSelector.Condition) read.selectors().getFirst();
        assertEquals(col("renamed_year"), ((IsAnyPredicate) selector.predicate()).dimension());
        assertSame(query.items().getFirst(), changed.items().getFirst());
        assertEquals(col("year"), query.model().main().dimensions().getFirst().expression());
        // Aliases do not change implicitly when their input expressions change.
        assertEquals(id("year"), changed.model().main().dimensions().getFirst().name());
        var bare = isAny();
        assertSame(bare, rewrite.transform(bare));
    }
}
