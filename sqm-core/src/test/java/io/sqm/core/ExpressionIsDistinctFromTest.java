package io.sqm.core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Expression isDistinctFrom/isNotDistinctFrom Tests")
class ExpressionIsDistinctFromTest {

    @Test
    @DisplayName("isDistinctFrom creates predicate")
    void isDistinctFromCreatesPredicate() {
        var pred = col("a").isDistinctFrom(col("b"));

        assertInstanceOf(IsDistinctFromPredicate.class, pred);
        assertFalse(pred.negated());
        assertEquals("a", ((ColumnExpr) pred.lhs()).name().value());
        assertEquals("b", ((ColumnExpr) pred.rhs()).name().value());
    }

    @Test
    @DisplayName("isNotDistinctFrom creates negated predicate")
    void isNotDistinctFromCreatesNegatedPredicate() {
        var pred = col("a").isNotDistinctFrom(col("b"));

        assertInstanceOf(IsDistinctFromPredicate.class, pred);
        assertTrue(pred.negated());
        assertEquals("a", ((ColumnExpr) pred.lhs()).name().value());
        assertEquals("b", ((ColumnExpr) pred.rhs()).name().value());
    }

    @Test
    @DisplayName("isDistinctFrom with literals")
    void isDistinctFromWithLiterals() {
        var pred = lit(1).isDistinctFrom(lit(2));

        assertNotNull(pred);
        assertEquals(1, ((LiteralExpr) pred.lhs()).value());
        assertEquals(2, ((LiteralExpr) pred.rhs()).value());
    }

    @Test
    @DisplayName("isDistinctFrom with NULL")
    void isDistinctFromWithNull() {
        var pred = col("a").isDistinctFrom(lit(null));

        assertNotNull(pred);
        assertNull(((LiteralExpr) pred.rhs()).value());
    }

    @Test
    @DisplayName("isNotDistinctFrom with qualified columns")
    void isNotDistinctFromWithQualifiedColumns() {
        var pred = col("t1", "id").isNotDistinctFrom(col("t2", "id"));

        assertTrue(pred.negated());
        assertEquals("t1", ((ColumnExpr) pred.lhs()).tableAlias().value());
        assertEquals("t2", ((ColumnExpr) pred.rhs()).tableAlias().value());
    }

    @Test
    @DisplayName("isDistinctFrom in complex expression")
    void isDistinctFromInComplexExpression() {
        var pred = col("a").isDistinctFrom(col("b"))
            .and(col("c").eq(lit(3)));

        assertInstanceOf(AndPredicate.class, pred);
        assertInstanceOf(IsDistinctFromPredicate.class, pred.lhs());
    }

    @Test
    @DisplayName("isNotDistinctFrom with function expressions")
    void isNotDistinctFromWithFunctions() {
        var pred = func("upper", arg(col("a")))
            .isNotDistinctFrom(func("upper", arg(col("b"))));

        assertInstanceOf(FunctionExpr.class, pred.lhs());
        assertInstanceOf(FunctionExpr.class, pred.rhs());
        assertTrue(pred.negated());
    }

    @Test
    @DisplayName("Chaining isDistinctFrom predicates")
    void chainingIsDistinctFromPredicates() {
        var pred = col("a").isDistinctFrom(col("b"))
            .or(col("c").isNotDistinctFrom(col("d")));

        assertInstanceOf(OrPredicate.class, pred);
        assertInstanceOf(IsDistinctFromPredicate.class, pred.lhs());
        assertInstanceOf(IsDistinctFromPredicate.class, pred.rhs());
    }
}
