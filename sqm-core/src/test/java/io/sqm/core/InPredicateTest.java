package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InPredicateTest {

    @Test
    void of() {
        var lhs = Expression.column("status");
        var rhs = Expression.row("active", "pending");
        var predicate = InPredicate.of(lhs, rhs, false);
        
        assertNotNull(predicate);
        assertInstanceOf(InPredicate.class, predicate);
        assertEquals(lhs, predicate.lhs());
        assertEquals(rhs, predicate.rhs());
        assertFalse(predicate.negated());
    }

    @Test
    void in() {
        var predicate = InPredicate.of(
            Expression.column("id"),
            Expression.row(1, 2),
            false
        );
        assertFalse(predicate.negated());
    }

    @Test
    void notIn() {
        var predicate = InPredicate.of(
            Expression.column("id"),
            Expression.row(1, 2),
            true
        );
        assertTrue(predicate.negated());
    }

    @Test
    void lhs() {
        var lhs = Expression.column("category");
        var predicate = InPredicate.of(
            lhs,
            Expression.row("A"),
            false
        );
        assertEquals(lhs, predicate.lhs());
    }

    @Test
    void rhs() {
        var rhs = Expression.row(1, 2, 3);
        var predicate = InPredicate.of(
            Expression.column("id"),
            rhs,
            false
        );
        assertEquals(rhs, predicate.rhs());
    }

    @Test
    void accept() {
        var predicate = InPredicate.of(
            Expression.column("x"),
            Expression.row(1),
            false
        );
        var visitor = new TestVisitor();
        var result = predicate.accept(visitor);
        assertTrue(result);
    }

    @Test
    void withQueryExpr() {
        var subquery = Query.select(Expression.column("id")).from(TableRef.table("orders"));
        var rhs = Expression.subquery(subquery);
        var predicate = InPredicate.of(
            Expression.column("orderId"),
            rhs,
            false
        );
        assertInstanceOf(QueryExpr.class, predicate.rhs());
    }

    @Test
    void withRowListExpr() {
        var row1 = RowExpr.of(java.util.List.of(Expression.literal(1), Expression.literal(2)));
        var row2 = RowExpr.of(java.util.List.of(Expression.literal(3), Expression.literal(4)));
        var rhs = Expression.rows(row1, row2);
        var predicate = InPredicate.of(
            Expression.column("x"),
            rhs,
            false
        );
        assertInstanceOf(RowListExpr.class, predicate.rhs());
    }

    static class TestVisitor extends io.sqm.core.walk.RecursiveNodeVisitor<Boolean> {
        @Override
        protected Boolean defaultResult() {
            return false;
        }

        @Override
        public Boolean visitInPredicate(InPredicate node) {
            return true;
        }
    }
}
