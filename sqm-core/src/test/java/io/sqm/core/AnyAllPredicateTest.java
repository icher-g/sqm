package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AnyAllPredicateTest {

    @Test
    void of() {
        var lhs = Expression.column("age");
        var subquery = Query.select(Expression.literal(1));
        var predicate = AnyAllPredicate.of(lhs, ComparisonOperator.EQ, subquery, Quantifier.ANY);
        
        assertNotNull(predicate);
        assertInstanceOf(AnyAllPredicate.class, predicate);
        assertEquals(lhs, predicate.lhs());
        assertEquals(ComparisonOperator.EQ, predicate.operator());
        assertEquals(subquery, predicate.subquery());
        assertEquals(Quantifier.ANY, predicate.quantifier());
    }

    @Test
    void anyQuantifier() {
        var predicate = AnyAllPredicate.of(
            Expression.column("age"),
            ComparisonOperator.LT,
            Query.select(Expression.column("age")).from(TableRef.table("users")),
            Quantifier.ANY
        );
        
        assertEquals(Quantifier.ANY, predicate.quantifier());
    }

    @Test
    void allQuantifier() {
        var predicate = AnyAllPredicate.of(
            Expression.column("age"),
            ComparisonOperator.GT,
            Query.select(Expression.column("age")).from(TableRef.table("users")),
            Quantifier.ALL
        );
        
        assertEquals(Quantifier.ALL, predicate.quantifier());
    }

    @Test
    void differentOperators() {
        var operators = new ComparisonOperator[]{
            ComparisonOperator.EQ, ComparisonOperator.NE,
            ComparisonOperator.LT, ComparisonOperator.LTE,
            ComparisonOperator.GT, ComparisonOperator.GTE
        };
        
        for (var op : operators) {
            var predicate = AnyAllPredicate.of(
                Expression.column("x"),
                op,
                Query.select(Expression.literal(1)),
                Quantifier.ANY
            );
            assertEquals(op, predicate.operator());
        }
    }

    @Test
    void accept() {
        var predicate = AnyAllPredicate.of(
            Expression.column("age"),
            ComparisonOperator.EQ,
            Query.select(Expression.literal(1)),
            Quantifier.ANY
        );
        
        var visitor = new TestVisitor();
        var result = predicate.accept(visitor);
        assertTrue(result);
    }

    @Test
    void lhs() {
        var lhs = Expression.column("price");
        var predicate = AnyAllPredicate.of(
            lhs,
            ComparisonOperator.EQ,
            Query.select(Expression.literal(1)),
            Quantifier.ANY
        );
        
        assertEquals(lhs, predicate.lhs());
    }

    @Test
    void subquery() {
        var subquery = Query.select(Expression.column("id")).from(TableRef.table("orders"));
        var predicate = AnyAllPredicate.of(
            Expression.column("orderId"),
            ComparisonOperator.EQ,
            subquery,
            Quantifier.ANY
        );
        
        assertEquals(subquery, predicate.subquery());
    }

    static class TestVisitor extends io.sqm.core.walk.RecursiveNodeVisitor<Boolean> {
        @Override
        protected Boolean defaultResult() {
            return false;
        }

        @Override
        public Boolean visitAnyAllPredicate(AnyAllPredicate node) {
            return true;
        }
    }
}
