package io.sqm.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static io.sqm.dsl.Dsl.col;

class UnaryPredicateTest {

    @Test
    void of() {
        var expr = Expression.literal(true);
        var predicate = UnaryPredicate.of(expr);
        
        assertNotNull(predicate);
        assertInstanceOf(UnaryPredicate.class, predicate);
        assertEquals(expr, predicate.expr());
    }

    @Test
    void expr() {
        var expr = col("active");
        var predicate = UnaryPredicate.of(expr);
        assertEquals(expr, predicate.expr());
    }

    @Test
    void accept() {
        var predicate = UnaryPredicate.of(Expression.literal(true));
        var visitor = new TestVisitor();
        var result = predicate.accept(visitor);
        assertTrue(result);
    }

    @Test
    void withBooleanLiteral() {
        var predicate = UnaryPredicate.of(Expression.literal(true));
        assertInstanceOf(LiteralExpr.class, predicate.expr());
    }

    @Test
    void withBooleanColumn() {
        var predicate = UnaryPredicate.of(col("isActive"));
        assertInstanceOf(ColumnExpr.class, predicate.expr());
    }

    @Test
    void withFalseLiteral() {
        var predicate = UnaryPredicate.of(Expression.literal(false));
        assertInstanceOf(LiteralExpr.class, predicate.expr());
    }

    static class TestVisitor extends io.sqm.core.walk.RecursiveNodeVisitor<Boolean> {
        @Override
        protected Boolean defaultResult() {
            return false;
        }

        @Override
        public Boolean visitUnaryPredicate(UnaryPredicate node) {
            return true;
        }
    }
}

