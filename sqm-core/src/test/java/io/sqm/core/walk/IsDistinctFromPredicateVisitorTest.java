package io.sqm.core.walk;

import io.sqm.core.*;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for visitor support of {@link IsDistinctFromPredicate}.
 */
class IsDistinctFromPredicateVisitorTest {

    @Test
    void testRecursiveVisitorCallsIsDistinctFromMethod() {
        var pred = IsDistinctFromPredicate.of(col("a"), lit(10), false);
        var visitor = new TestRecursiveVisitor();
        
        pred.accept(visitor);
        
        assertTrue(visitor.visitedIsDistinctFrom);
        assertEquals(pred, visitor.lastIsDistinctFromPredicate);
    }

    @Test
    void testRecursiveVisitorTraversesOperands() {
        var lhs = col("test_col");
        var rhs = lit(42);
        var pred = IsDistinctFromPredicate.of(lhs, rhs, false);
        
        var collector = new NodeCollector();
        pred.accept(collector);
        
        assertTrue(collector.nodes.contains(lhs));
        assertTrue(collector.nodes.contains(rhs));
    }

    @Test
    void testVisitorInCompositeQuery() {
        var pred1 = IsDistinctFromPredicate.of(col("a"), lit(1), false);
        var pred2 = col("b").eq(lit(2));
        var composite = pred1.and(pred2);
        
        var collector = new PredicateTypeCollector();
        composite.accept(collector);
        
        assertTrue(collector.hasIsDistinctFrom);
        assertTrue(collector.hasComparison);
    }

    @Test
    void testVisitorInSelectQuery() {
        var query = select(col("id"))
            .from(tbl("users"))
            .where(IsDistinctFromPredicate.of(col("status"), lit("active"), false));
        
        var collector = new PredicateTypeCollector();
        query.accept(collector);
        
        assertTrue(collector.hasIsDistinctFrom);
    }

    @Test
    void testVisitorCountsPredicates() {
        var pred1 = IsDistinctFromPredicate.of(col("a"), lit(1), false);
        var pred2 = IsDistinctFromPredicate.of(col("b"), lit(2), true);
        var pred3 = col("c").eq(lit(3));
        var composite = pred1.and(pred2).and(pred3);
        
        var counter = new PredicateCounter();
        composite.accept(counter);
        
        assertEquals(2, counter.isDistinctFromCount);
        assertEquals(1, counter.comparisonCount);
    }

    @Test
    void testVisitorWithNestedPredicates() {
        var inner = IsDistinctFromPredicate.of(col("x"), lit(5), false);
        var notPred = inner.not();
        var outer = notPred.or(col("y").eq(lit(10)));
        
        var collector = new PredicateTypeCollector();
        outer.accept(collector);
        
        assertTrue(collector.hasIsDistinctFrom);
        assertTrue(collector.hasComparison);
        assertTrue(collector.hasNot);
    }

    @Test
    void testTransformerCanModifyPredicate() {
        var original = IsDistinctFromPredicate.of(col("old"), lit(1), false);
        var transformer = new ColumnRenameTransformer();
        
        var transformed = (IsDistinctFromPredicate) original.accept(transformer);
        
        assertEquals("new", ((ColumnExpr) transformed.lhs()).name());
        assertNotSame(original, transformed);
    }

    @Test
    void testTransformerLeavesUnchangedWhenNoMatch() {
        var original = IsDistinctFromPredicate.of(col("other"), lit(1), false);
        var transformer = new ColumnRenameTransformer();
        
        var transformed = original.accept(transformer);
        
        assertSame(original, transformed);
    }

    @Test
    void testVisitorInJoinCondition() {
        var join = inner(tbl("orders").as("o"))
            .on(IsDistinctFromPredicate.of(col("u", "id"), col("o", "user_id"), true));
        
        var query = select(col("name"))
            .from(tbl("users").as("u"))
            .join(join);
        
        var collector = new PredicateTypeCollector();
        query.accept(collector);
        
        assertTrue(collector.hasIsDistinctFrom);
    }

    @Test
    void testVisitorDefaultResultReturned() {
        var pred = IsDistinctFromPredicate.of(col("a"), lit(1), false);
        var visitor = new IntegerReturningVisitor();
        
        Integer result = pred.accept(visitor);
        
        assertEquals(99, result);
    }

    // Test helper visitors

    private static class TestRecursiveVisitor extends RecursiveNodeVisitor<Void> {
        boolean visitedIsDistinctFrom = false;
        IsDistinctFromPredicate lastIsDistinctFromPredicate;

        @Override
        protected Void defaultResult() {
            return null;
        }

        @Override
        public Void visitIsDistinctFromPredicate(IsDistinctFromPredicate p) {
            visitedIsDistinctFrom = true;
            lastIsDistinctFromPredicate = p;
            return super.visitIsDistinctFromPredicate(p);
        }
    }

    private static class NodeCollector extends RecursiveNodeVisitor<Void> {
        java.util.List<Node> nodes = new java.util.ArrayList<>();

        @Override
        protected Void defaultResult() {
            return null;
        }

        @Override
        public Void visitColumnExpr(ColumnExpr c) {
            nodes.add(c);
            return super.visitColumnExpr(c);
        }

        @Override
        public Void visitLiteralExpr(LiteralExpr l) {
            nodes.add(l);
            return super.visitLiteralExpr(l);
        }
    }

    private static class PredicateTypeCollector extends RecursiveNodeVisitor<Void> {
        boolean hasIsDistinctFrom = false;
        boolean hasComparison = false;
        boolean hasNot = false;

        @Override
        protected Void defaultResult() {
            return null;
        }

        @Override
        public Void visitIsDistinctFromPredicate(IsDistinctFromPredicate p) {
            hasIsDistinctFrom = true;
            return super.visitIsDistinctFromPredicate(p);
        }

        @Override
        public Void visitComparisonPredicate(ComparisonPredicate p) {
            hasComparison = true;
            return super.visitComparisonPredicate(p);
        }

        @Override
        public Void visitNotPredicate(NotPredicate p) {
            hasNot = true;
            return super.visitNotPredicate(p);
        }
    }

    private static class PredicateCounter extends RecursiveNodeVisitor<Void> {
        int isDistinctFromCount = 0;
        int comparisonCount = 0;

        @Override
        protected Void defaultResult() {
            return null;
        }

        @Override
        public Void visitIsDistinctFromPredicate(IsDistinctFromPredicate p) {
            isDistinctFromCount++;
            return super.visitIsDistinctFromPredicate(p);
        }

        @Override
        public Void visitComparisonPredicate(ComparisonPredicate p) {
            comparisonCount++;
            return super.visitComparisonPredicate(p);
        }
    }

    private static class ColumnRenameTransformer extends io.sqm.core.transform.RecursiveNodeTransformer {
        @Override
        public Node visitColumnExpr(ColumnExpr c) {
            if ("old".equals(c.name())) {
                return ColumnExpr.of("new");
            }
            return c;
        }
    }

    private static class IntegerReturningVisitor extends RecursiveNodeVisitor<Integer> {
        @Override
        protected Integer defaultResult() {
            return 99;
        }
    }
}
