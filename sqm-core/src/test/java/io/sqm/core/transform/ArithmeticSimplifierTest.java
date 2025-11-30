package io.sqm.core.transform;


import io.sqm.core.ColumnExpr;
import io.sqm.core.Expression;
import io.sqm.core.LiteralExpr;
import io.sqm.core.MulArithmeticExpr;
import org.junit.jupiter.api.Test;

import static io.sqm.dsl.Dsl.col;
import static io.sqm.dsl.Dsl.lit;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ArithmeticSimplifier}.
 *
 * <p>These tests describe the expected behavior of the arithmetic
 * transformer:
 * <ul>
 *     <li>Remove neutral elements (e.g. {@code x + 0}, {@code x * 1}).</li>
 *     <li>Simplify annihilating elements (e.g. {@code x * 0} → {@code 0}).</li>
 *     <li>Eliminate double negation (e.g. {@code -(-x)} → {@code x}).</li>
 *     <li>Apply the same rules recursively inside nested expressions.</li>
 * </ul>
 * The concrete implementation of {@link ArithmeticSimplifier} must
 * satisfy these tests.
 */
class ArithmeticSimplifierTest {

    private static Expression transform(Expression expr) {
        return (Expression) expr.accept(new ArithmeticSimplifier());
    }

    // -------------------------------------------------------------------------
    // Neutral element: x + 0 and 0 + x
    // -------------------------------------------------------------------------

    @Test
    void add_zero_on_right_is_removed() {
        Expression original = col("a").add(lit(0));
        Expression simplified = transform(original);

        // Expect: a
        assertInstanceOf(ColumnExpr.class, simplified);
        assertEquals("a", ((ColumnExpr) simplified).name());
    }

    @Test
    void add_zero_on_left_is_removed() {
        Expression original = lit(0).add(col("a"));
        Expression simplified = transform(original);

        // Expect: a
        assertInstanceOf(ColumnExpr.class, simplified);
        assertEquals("a", ((ColumnExpr) simplified).name());
    }

    @Test
    void add_two_numbers() {
        Expression original = lit(2).add(lit(3));
        Expression simplified = transform(original);

        // Expect: 5
        assertEquals(5L, ((LiteralExpr)simplified).value());
    }

    // -------------------------------------------------------------------------
    // Neutral element: x - 0
    // -------------------------------------------------------------------------

    @Test
    void sub_zero_on_right_is_removed() {
        Expression original = col("a").sub(lit(0));
        Expression simplified = transform(original);

        // Expect: a
        assertInstanceOf(ColumnExpr.class, simplified);
        assertEquals("a", ((ColumnExpr) simplified).name());
    }

    @Test
    void sub_two_numbers() {
        Expression original = lit(8).sub(lit(3));
        Expression simplified = transform(original);

        // Expect: 5
        assertEquals(5L, ((LiteralExpr)simplified).value());
    }

    // -------------------------------------------------------------------------
    // Neutral element: x * 1 and 1 * x
    // -------------------------------------------------------------------------

    @Test
    void mul_one_on_right_is_removed() {
        Expression original = col("a").mul(lit(1));
        Expression simplified = transform(original);

        // Expect: a
        assertInstanceOf(ColumnExpr.class, simplified);
        assertEquals("a", ((ColumnExpr) simplified).name());
    }

    @Test
    void mul_one_on_left_is_removed() {
        Expression original = lit(1).mul(col("a"));
        Expression simplified = transform(original);

        // Expect: a
        assertInstanceOf(ColumnExpr.class, simplified);
        assertEquals("a", ((ColumnExpr) simplified).name());
    }

    @Test
    void mul_two_numbers() {
        Expression original = lit(2).mul(lit(3));
        Expression simplified = transform(original);

        // Expect: 5
        assertEquals(6L, ((LiteralExpr)simplified).value());
    }

    @Test
    void div_two_numbers() {
        Expression original = lit(15).div(lit(3));
        Expression simplified = transform(original);

        // Expect: 5
        assertEquals(5L, ((LiteralExpr)simplified).value());
    }

    // -------------------------------------------------------------------------
    // Annihilating element: x * 0 and 0 * x
    // -------------------------------------------------------------------------

    @Test
    void mul_zero_on_right_becomes_zero_literal() {
        Expression original = col("a").mul(lit(0));
        Expression simplified = transform(original);

        // Expect: 0
        assertInstanceOf(LiteralExpr.class, simplified);
        assertEquals(0, ((LiteralExpr) simplified).value());
    }

    @Test
    void mul_zero_on_left_becomes_zero_literal() {
        Expression original = lit(0).mul(col("a"));
        Expression simplified = transform(original);

        // Expect: 0
        assertInstanceOf(LiteralExpr.class, simplified);
        assertEquals(0, ((LiteralExpr) simplified).value());
    }

    @Test
    void mod_two_numbers() {
        Expression original = lit(8).mod(lit(3));
        Expression simplified = transform(original);

        // Expect: 5
        assertEquals(2L, ((LiteralExpr)simplified).value());
    }

    // -------------------------------------------------------------------------
    // Double negation: -(-x) → x
    // -------------------------------------------------------------------------

    @Test
    void double_negative_is_eliminated() {
        Expression inner = col("a");
        Expression original = inner.neg().neg(); // -(-a)

        Expression simplified = transform(original);

        // Expect: a
        assertInstanceOf(ColumnExpr.class, simplified);
        assertEquals("a", ((ColumnExpr) simplified).name());
    }

    // -------------------------------------------------------------------------
    // Recursive simplification: nested arithmetic
    // -------------------------------------------------------------------------

    @Test
    void simplifies_nested_arithmetic_expressions_recursively() {
        // Original: (a + 0) * (1 * (b - 0))
        Expression left = col("a").add(lit(0));
        Expression right = lit(1).mul(col("b").sub(lit(0)));
        Expression original = left.mul(right);

        Expression simplified = transform(original);

        // Expected simplified structure: a * b
        assertInstanceOf(MulArithmeticExpr.class, simplified);
        MulArithmeticExpr mul = (MulArithmeticExpr) simplified;

        assertInstanceOf(ColumnExpr.class, mul.lhs());
        assertEquals("a", ((ColumnExpr) mul.lhs()).name());

        assertInstanceOf(ColumnExpr.class, mul.rhs());
        assertEquals("b", ((ColumnExpr) mul.rhs()).name());
    }

    // -------------------------------------------------------------------------
    // Non-arithmetic expressions: should remain unchanged
    // -------------------------------------------------------------------------

    @Test
    void non_arithmetic_expression_is_unchanged() {
        Expression original = col("name"); // ColumnExpr, no arithmetic

        Expression simplified = transform(original);

        // We expect the transformer to leave non-arithmetic expressions as-is.
        assertSame(original, simplified,
            "Non-arithmetic expressions should be returned unchanged");
    }

    // -------------------------------------------------------------------------
    // Idempotence: applying simplifier twice is stable
    // -------------------------------------------------------------------------

    @Test
    void simplifier_is_idempotent() {
        // Expression that should simplify once to a normal form.
        Expression original = col("a").add(lit(0)).mul(lit(1)); // (a + 0) * 1
        ArithmeticSimplifier transformer = new ArithmeticSimplifier();

        Expression once = (Expression) original.accept(transformer);
        Expression twice = (Expression) once.accept(transformer);

        // After first simplification: a
        assertInstanceOf(ColumnExpr.class, once);
        assertEquals("a", ((ColumnExpr) once).name());

        // Second run should not change the expression further.
        assertSame(once, twice, "Running the simplifier twice should not change the result");
    }
}

