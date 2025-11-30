package io.sqm.core.utils;

import io.sqm.core.Expression;
import io.sqm.core.LiteralExpr;

/**
 * Utility methods for working with literal numeric values inside SQM
 * {@link Expression} nodes.
 *
 * <p>This class provides helper functions for:</p>
 *
 * <ul>
 *   <li>Extracting a {@link Number} value from a {@link LiteralExpr}</li>
 *   <li>Evaluating simple binary arithmetic operations (add, sub, mul, div, mod)
 *       when <em>both</em> operands are numeric literal expressions</li>
 * </ul>
 *
 * <p>The primary purpose of this utility is to support constant folding inside
 * arithmetic transformers and optimizers. All methods return {@code null} when
 * one of the operands is not a literal number, or when an operation cannot be
 * safely evaluated (for example division by zero).</p>
 *
 * <p>This class is not intended to perform arbitrary expression evaluation and
 * does not recurse into nested expression structures.</p>
 */
public class Literals {

    private Literals() {
    }

    /**
     * Extracts the numeric value from a literal expression.
     *
     * <p>If {@code expr} is a {@link LiteralExpr} whose value is an instance
     * of {@link Number}, the numeric value is returned. Otherwise {@code null}
     * is returned.</p>
     *
     * @param expr the expression to inspect, may be {@code null}
     * @return the numeric value contained in the literal expression,
     * or {@code null} if not applicable
     */
    public static Number asNumber(Expression expr) {
        if (expr instanceof LiteralExpr l) {
            if (l.value() instanceof Number n) {
                return n;
            }
        }
        return null;
    }

    /**
     * Checks if the expression is literal and equals to 0.
     *
     * @param expr an expression to inspect.
     * @return {@code true} if the expression is literal and its value is 0 and {@code false} otherwise.
     */
    public static boolean isZero(Expression expr) {
        if (expr instanceof LiteralExpr l) {
            if (l.value() instanceof Number n) {
                return n.longValue() == 0L;
            }
        }
        return false;
    }

    /**
     * Checks if the expression is literal and equals to 1.
     *
     * @param expr an expression to inspect.
     * @return {@code true} if the expression is literal and its value is 1 and {@code false} otherwise.
     */
    public static boolean isOne(Expression expr) {
        if (expr instanceof LiteralExpr l) {
            if (l.value() instanceof Number n) {
                return n.longValue() == 1L;
            }
        }
        return false;
    }

    /**
     * Evaluates the addition of two literal numeric expressions.
     *
     * <p>If both {@code lhs} and {@code rhs} resolve to numeric literals via
     * {@link #asNumber(Expression)}, their sum is returned. If either operand
     * is non-numeric, {@code null} is returned.</p>
     *
     * <p>If any operand is a {@link Double}, the result is computed using
     * {@code double} arithmetic. Otherwise, the result is computed using
     * {@code long} arithmetic.</p>
     *
     * @param lhs the left-hand operand
     * @param rhs the right-hand operand
     * @return the computed sum, or {@code null} if not applicable
     */
    public static Number add(Expression lhs, Expression rhs) {
        var l = asNumber(lhs);
        var r = asNumber(rhs);

        if (l != null && r != null) {
            if (l instanceof Double || r instanceof Double) {
                return l.doubleValue() + r.doubleValue();
            }
            return l.longValue() + r.longValue();
        }
        return null;
    }

    /**
     * Evaluates the subtraction of two literal numeric expressions.
     *
     * <p>If both operands resolve to numbers, the difference {@code lhs - rhs}
     * is returned. If either operand is non-numeric, {@code null} is returned.</p>
     *
     * <p>Uses {@code double} arithmetic if any operand is a {@link Double},
     * otherwise uses {@code long} arithmetic.</p>
     *
     * @param lhs the left-hand operand
     * @param rhs the right-hand operand
     * @return the computed difference, or {@code null} if not applicable
     */
    public static Number sub(Expression lhs, Expression rhs) {
        var l = asNumber(lhs);
        var r = asNumber(rhs);

        if (l != null && r != null) {
            if (l instanceof Double || r instanceof Double) {
                return l.doubleValue() - r.doubleValue();
            }
            return l.longValue() - r.longValue();
        }
        return null;
    }

    /**
     * Evaluates the multiplication of two literal numeric expressions.
     *
     * <p>If both operands resolve to numbers, their product is returned.
     * If either operand is non-numeric, {@code null} is returned.</p>
     *
     * <p>Uses {@code double} arithmetic when applicable, otherwise uses
     * {@code long} arithmetic.</p>
     *
     * @param lhs the left-hand operand
     * @param rhs the right-hand operand
     * @return the computed product, or {@code null} if not applicable
     */
    public static Number mul(Expression lhs, Expression rhs) {
        var l = asNumber(lhs);
        var r = asNumber(rhs);

        if (l != null && r != null) {
            if (l instanceof Double || r instanceof Double) {
                return l.doubleValue() * r.doubleValue();
            }
            return l.longValue() * r.longValue();
        }
        return null;
    }

    /**
     * Evaluates the division of two literal numeric expressions.
     *
     * <p>If both operands are numeric literals and the right-hand operand
     * is non-zero, returns {@code lhs / rhs}. If division cannot be performed
     * safely (e.g. division by zero or non-numeric operands), {@code null}
     * is returned.</p>
     *
     * <p>Uses {@code double} arithmetic when applicable, otherwise uses
     * {@code long} division.</p>
     *
     * @param lhs the left-hand operand
     * @param rhs the right-hand operand
     * @return the computed quotient, or {@code null} if not applicable
     */
    public static Number div(Expression lhs, Expression rhs) {
        var l = asNumber(lhs);
        var r = asNumber(rhs);

        if (l != null && r != null && r.longValue() != 0) {
            if (l instanceof Double || r instanceof Double) {
                return l.doubleValue() / r.doubleValue();
            }
            return l.longValue() / r.longValue();
        }
        return null;
    }

    /**
     * Evaluates the modulo operation between two literal numeric expressions.
     *
     * <p>If both operands resolve to numbers, returns {@code lhs % rhs}.
     * If either operand is non-numeric, {@code null} is returned.</p>
     *
     * <p>The modulo operator always uses integer-based arithmetic on the
     * right operand. If the left operand is a {@link Double}, the Java
     * {@code %} operator is applied to its {@code double} value.</p>
     *
     * @param lhs the left-hand operand
     * @param rhs the right-hand operand
     * @return the computed modulo value, or {@code null} if not applicable
     */
    public static Number mod(Expression lhs, Expression rhs) {
        var l = asNumber(lhs);
        var r = asNumber(rhs);

        if (l != null && r != null) {
            if (l instanceof Double) {
                return l.doubleValue() % r.longValue();
            }
            return l.longValue() % r.longValue();
        }
        return null;
    }
}
