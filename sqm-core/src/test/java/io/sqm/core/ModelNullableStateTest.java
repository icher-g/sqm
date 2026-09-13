package io.sqm.core;

import io.sqm.core.transform.RecursiveNodeTransformer;
import io.sqm.core.walk.RecursiveNodeVisitor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

class ModelNullableStateTest {
    @Test
    void absentOptionsAreNullAndRequiredDefaultsRemainNonNull() {
        var range = cellForRange("year", 2026, 2030, RangeDirection.INCREMENT, 2);
        assertNull(range.likePattern());
        assertEquals(lit(2), range.step());
        assertEquals(range, cellForRange("year", lit(2026), lit(2030), RangeDirection.INCREMENT, lit(2)));
        var main = mainModel().dimension("year").measure("sales")
            .rule(cellTarget("sales", range), lit(100)).build();
        assertNull(main.name());
        assertNull(main.rules().iteration());
        assertNull(main.rules().rules().getFirst().mode());
        assertNull(main.rules().rules().getFirst().orderBy());
        assertEquals(RuleMode.UPSERT, main.rules().defaultMode());
        assertNull(iterationSpec(lit(10)).until());
        assertNull(cellRef("sales", 2026).model());
        assertNull(currentDimension().dimension());
        assertNull(isAny().dimension());
    }

    @Test
    void buildersCopyAndClearOptionalStateWithoutMutatingTheOriginal() {
        var iteration = iterationSpec(lit(10), col("sales").gt(100));
        var rules = modelRules().iteration(iteration).rule(cellTarget("sales", 2026), lit(100)).build();
        var main = mainModel().name("forecast").dimension("year").measure("sales").rules(rules).build();
        assertEquals(main, MainModel.builder(main).build());
        assertEquals(rules, ModelRules.builder(rules).build());
        assertNull(MainModel.builder(main).clearName().build().name());
        assertNull(MainModel.builder(main).name((Identifier) null).build().name());
        assertNull(MainModel.builder(main).name((String) null).build().name());
        assertNull(ModelRules.builder(rules).iteration(null).build().iteration());
        assertEquals(id("forecast"), main.name());
        assertSame(iteration, rules.iteration());
    }

    @Test
    void transformerCanRemoveOptionalChildrenAndPreserveRequiredSiblings() {
        var rewrite = new RecursiveNodeTransformer() {
            @Override public Node visitLiteralExpr(LiteralExpr node) {
                return node.equals(lit("Y%")) ? null : node;
            }
            @Override public Node visitComparisonPredicate(ComparisonPredicate node) { return null; }
            @Override public Node visitOrderBy(OrderBy node) { return null; }
            @Override public Node visitIterationSpec(IterationSpec node) { return null; }
            @Override public Node visitColumnExpr(ColumnExpr node) { return null; }
        };
        var range = cellForRange(id("year"), lit("Y%"), lit(2026), lit(2030), RangeDirection.INCREMENT, lit(1));
        var changedRange = (CellFor.Range) rewrite.transform(range);
        assertNull(changedRange.likePattern());
        assertSame(range.from(), changedRange.from());
        var iteration = iterationSpec(lit(10), col("sales").gt(100));
        var changedIteration = (IterationSpec) new RecursiveNodeTransformer() {
            @Override public Node visitComparisonPredicate(ComparisonPredicate node) { return null; }
        }.transform(iteration);
        assertNull(changedIteration.until());
        assertSame(iteration.limit(), changedIteration.limit());
        var rule = modelRule(RuleMode.UPDATE, cellTarget("sales", 2026), orderBy(col("year").asc()), lit(100));
        var changedRule = (ModelRule) rewrite.transform(rule);
        assertNull(changedRule.orderBy());
        assertSame(rule.target(), changedRule.target());
        var rules = modelRules().iteration(iteration).rule(rule).build();
        assertNull(((ModelRules) rewrite.transform(rules)).iteration());
        assertNull(((IsAnyPredicate) rewrite.transform(isAny("year"))).dimension());
    }

    @Test
    void traversalAndTransformationHandleAbsentAndPresentChildren() {
        var missing = List.<Node>of(
            cellForRange("year", 2026, 2030, RangeDirection.INCREMENT, 1),
            iterationSpec(lit(10)), modelRule(cellTarget("sales", 2026), lit(100)),
            modelRules().rule(cellTarget("sales", 2026), lit(100)).build(),
            currentDimension(), isAny(), cellRef("sales", 2026));
        var present = List.<Node>of(
            cellForRange(id("year"), lit("Y%"), lit(2026), lit(2030), RangeDirection.INCREMENT, lit(1)),
            iterationSpec(lit(10), col("sales").gt(100)),
            modelRule(RuleMode.UPDATE, cellTarget("sales", 2026), orderBy(col("year").asc()), lit(100)),
            modelRules().iteration(iterationSpec(lit(10))).rule(cellTarget("sales", 2026), lit(100)).build(),
            currentDimension("year"), isAny("year"), cellRef(id("forecast"), id("sales"), cellValue(lit(2026))));
        var unchanged = new RecursiveNodeTransformer() {};
        var rewrite = new RecursiveNodeTransformer() {
            @Override public Node visitLiteralExpr(LiteralExpr node) { return lit(42); }
            @Override public Node visitColumnExpr(ColumnExpr node) { return col("rewritten"); }
        };
        for (var nodes : List.of(missing, present)) {
            for (var node : nodes) {
                node.accept(new RecursiveNodeVisitor<Void>() {
                    @Override protected Void defaultResult() { return null; }
                });
                assertSame(node, unchanged.transform(node));
                var updated = rewrite.transform(node);
                assertSame(updated, unchanged.transform(updated));
            }
        }
        var range = (CellFor.Range) rewrite.transform(missing.getFirst());
        assertNull(range.likePattern());
        assertEquals(lit(42), range.from());
        var templateRange = (CellFor.Range) rewrite.transform(present.getFirst());
        assertEquals(lit(42), templateRange.likePattern());
        var iteration = (IterationSpec) rewrite.transform(present.get(1));
        assertEquals(col("rewritten").gt(42), iteration.until());
        var rule = (ModelRule) rewrite.transform(present.get(2));
        assertEquals(RuleMode.UPDATE, rule.mode());
        assertEquals(orderBy(col("rewritten").asc()), rule.orderBy());
        assertNull(((IterationSpec) rewrite.transform(missing.get(1))).until());
        assertNull(((ModelRule) rewrite.transform(missing.get(2))).orderBy());
        assertNull(((ModelRules) rewrite.transform(missing.get(3))).iteration());
    }
}
