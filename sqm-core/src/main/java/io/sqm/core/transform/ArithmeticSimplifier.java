package io.sqm.core.transform;

import io.sqm.core.*;
import io.sqm.core.utils.Literals;

/**
 * A simple arithmetic transformer that normalizes and simplifies
 * {@link ArithmeticExpr} trees.
 *
 * <p>The transformer applies a set of safe, local rewrite rules, such as:</p>
 *
 * <ul>
 *     <li>{@code x + 0} → {@code x}</li>
 *     <li>{@code 0 + x} → {@code x}</li>
 *     <li>{@code x - 0} → {@code x}</li>
 *     <li>{@code x * 1} → {@code x}</li>
 *     <li>{@code 1 * x} → {@code x}</li>
 *     <li>{@code x * 0} → {@code 0}</li>
 *     <li>{@code 0 * x} → {@code 0}</li>
 *     <li>{@code -(-x)} → {@code x}</li>
 * </ul>
 *
 * <p>The transformer currently focuses on arithmetic expressions only: it
 * does not attempt to rewrite non-arithmetic expression types or
 * traverse into their children. Non-arithmetic expressions are returned
 * unchanged.</p>
 *
 * <p>The transformation is idempotent: applying it repeatedly to the same
 * expression tree yields the same result instance after the first run,
 * assuming the tree is not modified externally.</p>
 */
public class ArithmeticSimplifier extends RecursiveNodeTransformer {

    /**
     * Visits an {@link AddArithmeticExpr} node.
     *
     * <p>This method is invoked when the visitor encounters a binary addition
     * expression of the form {@code lhs + rhs}. Implementations may perform
     * processing, transformation, or traversal of the node and its operands.</p>
     *
     * @param expr the addition expression being visited, never {@code null}
     * @return a visitor-defined result
     */
    @Override
    public Node visitAddArithmeticExpr(AddArithmeticExpr expr) {
        var lhs = apply(expr.lhs());
        var rhs = apply(expr.rhs());

        // x + 0 → x
        if (Literals.isZero(rhs)) {
            return lhs;
        }
        // 0 + x → x
        if (Literals.isZero(lhs)) {
            return rhs;
        }

        // try to add
        Number n = Literals.add(lhs, rhs);
        if (n != null) {
            return LiteralExpr.of(n);
        }

        // No rule applied; if children are unchanged, return original instance.
        if (lhs == expr.lhs() && rhs == expr.rhs()) {
            return expr;
        }

        return AddArithmeticExpr.of(lhs, rhs);
    }

    /**
     * Visits a {@link SubArithmeticExpr} node.
     *
     * <p>This method is invoked when the visitor encounters a binary subtraction
     * expression of the form {@code lhs - rhs}. Implementations may perform
     * processing, transformation, or traversal of the node and its operands.</p>
     *
     * @param expr the subtraction expression being visited, never {@code null}
     * @return a visitor-defined result
     */
    @Override
    public Node visitSubArithmeticExpr(SubArithmeticExpr expr) {
        var lhs = apply(expr.lhs());
        var rhs = apply(expr.rhs());

        // x - 0 → x
        if (Literals.isZero(rhs)) {
            return lhs;
        }

        // try to subtract
        Number n = Literals.sub(lhs, rhs);
        if (n != null) {
            return LiteralExpr.of(n);
        }

        if (lhs == expr.lhs() && rhs == expr.rhs()) {
            return expr;
        }

        return SubArithmeticExpr.of(lhs, rhs);
    }

    /**
     * Visits a {@link MulArithmeticExpr} node.
     *
     * <p>This method is invoked when the visitor encounters a binary multiplication
     * expression of the form {@code lhs * rhs}. Implementations may perform
     * processing, transformation, or traversal of the node and its operands.</p>
     *
     * @param expr the multiplication expression being visited, never {@code null}
     * @return a visitor-defined result
     */
    @Override
    public Node visitMulArithmeticExpr(MulArithmeticExpr expr) {
        var lhs = apply(expr.lhs());
        var rhs = apply(expr.rhs());

        // x * 0 → 0
        if (Literals.isZero(lhs)) {
            return lhs;
        }
        if (Literals.isZero(rhs)) {
            return rhs;
        }

        // x * 1 → x
        if (Literals.isOne(rhs)) {
            return lhs;
        }
        // 1 * x → x
        if (Literals.isOne(lhs)) {
            return rhs;
        }

        // try to multiply
        Number n = Literals.mul(lhs, rhs);
        if (n != null) {
            return LiteralExpr.of(n);
        }

        if (lhs == expr.lhs() && rhs == expr.rhs()) {
            return expr;
        }
        return MulArithmeticExpr.of(lhs, rhs);
    }

    /**
     * Visits a {@link DivArithmeticExpr} node.
     *
     * <p>This method is invoked when the visitor encounters a binary division
     * expression of the form {@code lhs / rhs}. Implementations may perform
     * processing, transformation, or traversal of the node and its operands.</p>
     *
     * @param expr the division expression being visited, never {@code null}
     * @return a visitor-defined result
     */
    @Override
    public Node visitDivArithmeticExpr(DivArithmeticExpr expr) {
        var lhs = apply(expr.lhs());
        var rhs = apply(expr.rhs());

        // x / 1 → x (we do NOT simplify 0 / x or x / 0 here)
        if (Literals.isOne(rhs)) {
            return lhs;
        }

        // try to divide
        Number n = Literals.div(lhs, rhs);
        if (n != null) {
            return LiteralExpr.of(n);
        }

        if (lhs == expr.lhs() && rhs == expr.rhs()) {
            return expr;
        }
        return DivArithmeticExpr.of(lhs, rhs);
    }

    /**
     * Visits a {@link ModArithmeticExpr} node.
     *
     * <p>This method is invoked when the visitor encounters a binary modulo
     * expression of the form {@code lhs % rhs}. Implementations may perform
     * processing, transformation, or traversal of the node and its operands.
     * Note that the rendered SQL form of the modulo operator may vary by dialect.</p>
     *
     * @param expr the modulo expression being visited, never {@code null}
     * @return a visitor-defined result
     */
    @Override
    public Node visitModArithmeticExpr(ModArithmeticExpr expr) {
        var lhs = apply(expr.lhs());
        var rhs = apply(expr.rhs());

        // try to mod
        Number n = Literals.mod(lhs, rhs);
        if (n != null) {
            return LiteralExpr.of(n);
        }

        if (lhs == expr.lhs() && rhs == expr.rhs()) {
            return expr;
        }
        return ModArithmeticExpr.of(lhs, rhs);
    }

    /**
     * Visits a {@link NegativeArithmeticExpr} node.
     *
     * <p>This method is invoked when the visitor encounters a unary negation
     * expression of the form {@code -expr}. Implementations may perform
     * processing, transformation, or traversal of the negated operand.</p>
     *
     * @param expr the negation expression being visited, never {@code null}
     * @return a visitor-defined result
     */
    @Override
    public Node visitNegativeArithmeticExpr(NegativeArithmeticExpr expr) {
        var inner = apply(expr.expr());

        // -(-x) → x
        if (inner instanceof NegativeArithmeticExpr innerNeg) {
            return innerNeg.expr();
        }

        if (inner == expr.expr()) {
            return expr;
        }
        return NegativeArithmeticExpr.of(inner);
    }
}
