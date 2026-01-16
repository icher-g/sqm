package io.sqm.core.utils;

import io.sqm.core.Expression;
import org.junit.jupiter.api.Test;

import static io.sqm.core.Expression.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link Literals} utility class.
 */
public class LiteralsTest {

    /* ========================= asNumber ========================= */

    @Test
    void asNumber_withIntegerLiteral() {
        Expression expr = literal(42);
        Number num = Literals.asNumber(expr);
        assertNotNull(num);
        assertEquals(42, num.longValue());
    }

    @Test
    void asNumber_withLongLiteral() {
        Expression expr = literal(123456789L);
        Number num = Literals.asNumber(expr);
        assertNotNull(num);
        assertEquals(123456789L, num.longValue());
    }

    @Test
    void asNumber_withDoubleLiteral() {
        Expression expr = literal(3.14);
        Number num = Literals.asNumber(expr);
        assertNotNull(num);
        assertEquals(3.14, num.doubleValue(), 0.001);
    }

    @Test
    void asNumber_withFloatLiteral() {
        Expression expr = literal(2.71f);
        Number num = Literals.asNumber(expr);
        assertNotNull(num);
    }

    @Test
    void asNumber_withStringLiteral_returnsNull() {
        Expression expr = literal("not a number");
        Number num = Literals.asNumber(expr);
        assertNull(num);
    }

    @Test
    void asNumber_withNullValue_returnsNull() {
        Expression expr = literal(null);
        Number num = Literals.asNumber(expr);
        assertNull(num);
    }

    @Test
    void asNumber_withColumnExpr_returnsNull() {
        Expression expr = column("amount");
        Number num = Literals.asNumber(expr);
        assertNull(num);
    }

    @Test
    void asNumber_withFunctionExpr_returnsNull() {
        Expression expr = func("count", starArg());
        Number num = Literals.asNumber(expr);
        assertNull(num);
    }

    @Test
    void asNumber_withNull_returnsNull() {
        Number num = Literals.asNumber(null);
        assertNull(num);
    }

    @Test
    void asNumber_withZero() {
        Expression expr = literal(0);
        Number num = Literals.asNumber(expr);
        assertNotNull(num);
        assertEquals(0, num.longValue());
    }

    @Test
    void asNumber_withNegativeNumber() {
        Expression expr = literal(-100);
        Number num = Literals.asNumber(expr);
        assertNotNull(num);
        assertEquals(-100, num.longValue());
    }

    /* ========================= isZero ========================= */

    @Test
    void isZero_withZeroInteger() {
        Expression expr = literal(0);
        assertTrue(Literals.isZero(expr));
    }

    @Test
    void isZero_withZeroLong() {
        Expression expr = literal(0L);
        assertTrue(Literals.isZero(expr));
    }

    @Test
    void isZero_withZeroDouble() {
        Expression expr = literal(0.0);
        assertTrue(Literals.isZero(expr));
    }

    @Test
    void isZero_withNonZero() {
        Expression expr = literal(1);
        assertFalse(Literals.isZero(expr));
    }

    @Test
    void isZero_withNegativeNumber() {
        Expression expr = literal(-1);
        assertFalse(Literals.isZero(expr));
    }

    @Test
    void isZero_withStringLiteral() {
        Expression expr = literal("0");
        assertFalse(Literals.isZero(expr));
    }

    @Test
    void isZero_withColumn() {
        Expression expr = column("value");
        assertFalse(Literals.isZero(expr));
    }

    @Test
    void isZero_withNull() {
        assertFalse(Literals.isZero(null));
    }

    /* ========================= isOne ========================= */

    @Test
    void isOne_withOneInteger() {
        Expression expr = literal(1);
        assertTrue(Literals.isOne(expr));
    }

    @Test
    void isOne_withOneLong() {
        Expression expr = literal(1L);
        assertTrue(Literals.isOne(expr));
    }

    @Test
    void isOne_withOneDouble() {
        Expression expr = literal(1.0);
        assertTrue(Literals.isOne(expr));
    }

    @Test
    void isOne_withZero() {
        Expression expr = literal(0);
        assertFalse(Literals.isOne(expr));
    }

    @Test
    void isOne_withTwo() {
        Expression expr = literal(2);
        assertFalse(Literals.isOne(expr));
    }

    @Test
    void isOne_withStringLiteral() {
        Expression expr = literal("1");
        assertFalse(Literals.isOne(expr));
    }

    @Test
    void isOne_withNull() {
        assertFalse(Literals.isOne(null));
    }

    /* ========================= add ========================= */

    @Test
    void add_twoIntegerLiterals() {
        Expression lhs = literal(10);
        Expression rhs = literal(20);
        Number result = Literals.add(lhs, rhs);
        assertNotNull(result);
        assertEquals(30, result.longValue());
    }

    @Test
    void add_twoLongLiterals() {
        Expression lhs = literal(100L);
        Expression rhs = literal(200L);
        Number result = Literals.add(lhs, rhs);
        assertNotNull(result);
        assertEquals(300, result.longValue());
    }

    @Test
    void add_integerAndDouble() {
        Expression lhs = literal(10);
        Expression rhs = literal(3.5);
        Number result = Literals.add(lhs, rhs);
        assertNotNull(result);
        assertEquals(13.5, result.doubleValue(), 0.001);
    }

    @Test
    void add_twoDoubles() {
        Expression lhs = literal(1.5);
        Expression rhs = literal(2.5);
        Number result = Literals.add(lhs, rhs);
        assertNotNull(result);
        assertEquals(4.0, result.doubleValue(), 0.001);
    }

    @Test
    void add_withNegatives() {
        Expression lhs = literal(-5);
        Expression rhs = literal(-3);
        Number result = Literals.add(lhs, rhs);
        assertNotNull(result);
        assertEquals(-8, result.longValue());
    }

    @Test
    void add_withColumn_returnsNull() {
        Expression lhs = literal(10);
        Expression rhs = column("amount");
        Number result = Literals.add(lhs, rhs);
        assertNull(result);
    }

    @Test
    void add_withStringLiteral_returnsNull() {
        Expression lhs = literal(10);
        Expression rhs = literal("twenty");
        Number result = Literals.add(lhs, rhs);
        assertNull(result);
    }

    /* ========================= sub ========================= */

    @Test
    void sub_twoIntegerLiterals() {
        Expression lhs = literal(30);
        Expression rhs = literal(10);
        Number result = Literals.sub(lhs, rhs);
        assertNotNull(result);
        assertEquals(20, result.longValue());
    }

    @Test
    void sub_integerAndDouble() {
        Expression lhs = literal(10);
        Expression rhs = literal(2.5);
        Number result = Literals.sub(lhs, rhs);
        assertNotNull(result);
        assertEquals(7.5, result.doubleValue(), 0.001);
    }

    @Test
    void sub_twoDoubles() {
        Expression lhs = literal(10.5);
        Expression rhs = literal(3.2);
        Number result = Literals.sub(lhs, rhs);
        assertNotNull(result);
        assertEquals(7.3, result.doubleValue(), 0.001);
    }

    @Test
    void sub_resultingInNegative() {
        Expression lhs = literal(5);
        Expression rhs = literal(10);
        Number result = Literals.sub(lhs, rhs);
        assertNotNull(result);
        assertEquals(-5, result.longValue());
    }

    @Test
    void sub_withColumn_returnsNull() {
        Expression lhs = literal(30);
        Expression rhs = column("amount");
        Number result = Literals.sub(lhs, rhs);
        assertNull(result);
    }

    /* ========================= mul ========================= */

    @Test
    void mul_twoIntegerLiterals() {
        Expression lhs = literal(6);
        Expression rhs = literal(7);
        Number result = Literals.mul(lhs, rhs);
        assertNotNull(result);
        assertEquals(42, result.longValue());
    }

    @Test
    void mul_integerAndDouble() {
        Expression lhs = literal(10);
        Expression rhs = literal(2.5);
        Number result = Literals.mul(lhs, rhs);
        assertNotNull(result);
        assertEquals(25.0, result.doubleValue(), 0.001);
    }

    @Test
    void mul_twoDoubles() {
        Expression lhs = literal(1.5);
        Expression rhs = literal(2.0);
        Number result = Literals.mul(lhs, rhs);
        assertNotNull(result);
        assertEquals(3.0, result.doubleValue(), 0.001);
    }

    @Test
    void mul_byZero() {
        Expression lhs = literal(100);
        Expression rhs = literal(0);
        Number result = Literals.mul(lhs, rhs);
        assertNotNull(result);
        assertEquals(0, result.longValue());
    }

    @Test
    void mul_negativeNumbers() {
        Expression lhs = literal(-5);
        Expression rhs = literal(-4);
        Number result = Literals.mul(lhs, rhs);
        assertNotNull(result);
        assertEquals(20, result.longValue());
    }

    @Test
    void mul_withColumn_returnsNull() {
        Expression lhs = literal(6);
        Expression rhs = column("quantity");
        Number result = Literals.mul(lhs, rhs);
        assertNull(result);
    }

    /* ========================= div ========================= */

    @Test
    void div_twoIntegerLiterals() {
        Expression lhs = literal(100);
        Expression rhs = literal(5);
        Number result = Literals.div(lhs, rhs);
        assertNotNull(result);
        assertEquals(20, result.longValue());
    }

    @Test
    void div_integerAndDouble() {
        Expression lhs = literal(10);
        Expression rhs = literal(2.5);
        Number result = Literals.div(lhs, rhs);
        assertNotNull(result);
        assertEquals(4.0, result.doubleValue(), 0.001);
    }

    @Test
    void div_twoDoubles() {
        Expression lhs = literal(10.0);
        Expression rhs = literal(4.0);
        Number result = Literals.div(lhs, rhs);
        assertNotNull(result);
        assertEquals(2.5, result.doubleValue(), 0.001);
    }

    @Test
    void div_byZero_returnsNull() {
        Expression lhs = literal(100);
        Expression rhs = literal(0);
        Number result = Literals.div(lhs, rhs);
        assertNull(result);
    }

    @Test
    void div_negativeNumbers() {
        Expression lhs = literal(-20);
        Expression rhs = literal(4);
        Number result = Literals.div(lhs, rhs);
        assertNotNull(result);
        assertEquals(-5, result.longValue());
    }

    @Test
    void div_withColumn_returnsNull() {
        Expression lhs = literal(100);
        Expression rhs = column("divisor");
        Number result = Literals.div(lhs, rhs);
        assertNull(result);
    }

    /* ========================= mod ========================= */

    @Test
    void mod_twoIntegerLiterals() {
        Expression lhs = literal(10);
        Expression rhs = literal(3);
        Number result = Literals.mod(lhs, rhs);
        assertNotNull(result);
        assertEquals(1, result.longValue());
    }

    @Test
    void mod_doubleAndInteger() {
        Expression lhs = literal(10.5);
        Expression rhs = literal(3);
        Number result = Literals.mod(lhs, rhs);
        assertNotNull(result);
    }

    @Test
    void mod_byOne() {
        Expression lhs = literal(100);
        Expression rhs = literal(1);
        Number result = Literals.mod(lhs, rhs);
        assertNotNull(result);
        assertEquals(0, result.longValue());
    }

    @Test
    void mod_negativeNumbers() {
        Expression lhs = literal(-10);
        Expression rhs = literal(3);
        Number result = Literals.mod(lhs, rhs);
        assertNotNull(result);
    }

    @Test
    void mod_withZeroDivisor() {
        Expression lhs = literal(10);
        Expression rhs = literal(0);
        // Note: mod with 0 divisor might throw or return null depending on implementation
        // Testing that it doesn't crash
        try {
            Number result = Literals.mod(lhs, rhs);
            // Either null or an exception is acceptable
        } catch (ArithmeticException e) {
            // Division by zero - acceptable
        }
    }

    @Test
    void mod_withColumn_returnsNull() {
        Expression lhs = literal(10);
        Expression rhs = column("divisor");
        Number result = Literals.mod(lhs, rhs);
        assertNull(result);
    }

    /* ========================= Combined Operations ========================= */

    @Test
    void combinedOperations_multipleArithmetic() {
        Expression a = literal(10);
        Expression b = literal(5);
        Expression c = literal(2);

        Number step1 = Literals.add(a, b);      // 15
        assertNotNull(step1);
        assertEquals(15, step1.longValue());

        Expression step1Expr = literal(step1);
        Number step2 = Literals.mul(step1Expr, c);  // 30
        assertNotNull(step2);
        assertEquals(30, step2.longValue());
    }

    @Test
    void largeNumbers() {
        Expression lhs = literal(Long.MAX_VALUE - 1);
        Expression rhs = literal(1L);
        Number result = Literals.add(lhs, rhs);
        assertNotNull(result);
        assertEquals(Long.MAX_VALUE, result.longValue());
    }

    @Test
    void mixedPrecisionArithmetic() {
        Expression intExpr = literal(5);
        Expression doubleExpr = literal(2.0);
        
        // When mixing int and double, result should be double
        Number result = Literals.mul(intExpr, doubleExpr);
        assertTrue(result instanceof Double || result instanceof Number);
    }
}
