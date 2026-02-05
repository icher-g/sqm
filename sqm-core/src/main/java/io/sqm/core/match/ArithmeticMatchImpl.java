package io.sqm.core.match;

import io.sqm.core.*;
import io.sqm.core.walk.NodeVisitor;

import java.util.function.Function;

/**
 * A typed match implementation for {@link ArithmeticExpr} nodes.
 *
 * <p>This matcher provides pattern-based dispatch for all arithmetic
 * expression variants, including:</p>
 *
 * <ul>
 *   <li>{@link AddArithmeticExpr}</li>
 *   <li>{@link SubArithmeticExpr}</li>
 *   <li>{@link MulArithmeticExpr}</li>
 *   <li>{@link DivArithmeticExpr}</li>
 *   <li>{@link ModArithmeticExpr}</li>
 *   <li>{@link NegativeArithmeticExpr}</li>
 * </ul>
 *
 * <p>Each method accepts a function that handles a specific arithmetic
 * node type. When a match operation is executed, the first matching
 * branch (in declaration order) is applied. If no branch matches, the
 * {@link #otherwise(Function)} method from {@link Match} is used as a
 * fallback.</p>
 *
 * <p>This interface does <strong>not</strong> perform recursive traversal.
 * To walk expression subtrees, use a {@link NodeVisitor} or transformer.</p>
 *
 * @param <R> the result type returned by the match operation
 */
public class ArithmeticMatchImpl<R> implements ArithmeticMatch<R> {

    private final ArithmeticExpr expr;
    private boolean matched = false;
    private R result;

    /**
     * Initializes {@link ArithmeticMatchImpl}.
     *
     * @param expr an expression to match.
     */
    public ArithmeticMatchImpl(ArithmeticExpr expr) {
        this.expr = expr;
    }

    /**
     * Matches a {@link AddArithmeticExpr} (binary addition) node.
     *
     * @param f function applied when the expression is an addition node,
     *          must not be {@code null}
     * @return this matcher instance for chaining
     */
    @Override
    public ArithmeticMatch<R> add(Function<AddArithmeticExpr, R> f) {
        if (!matched && expr instanceof AddArithmeticExpr e) {
            result = f.apply(e);
            matched = true;
        }
        return this;
    }

    /**
     * Matches a {@link SubArithmeticExpr} (binary subtraction) node.
     *
     * @param f function applied when the expression is a subtraction node,
     *          must not be {@code null}
     * @return this matcher instance for chaining
     */
    @Override
    public ArithmeticMatch<R> sub(Function<SubArithmeticExpr, R> f) {
        if (!matched && expr instanceof SubArithmeticExpr e) {
            result = f.apply(e);
            matched = true;
        }
        return this;
    }

    /**
     * Matches a {@link MulArithmeticExpr} (binary multiplication) node.
     *
     * @param f function applied when the expression is a multiplication node,
     *          must not be {@code null}
     * @return this matcher instance for chaining
     */
    @Override
    public ArithmeticMatch<R> mul(Function<MulArithmeticExpr, R> f) {
        if (!matched && expr instanceof MulArithmeticExpr e) {
            result = f.apply(e);
            matched = true;
        }
        return this;
    }

    /**
     * Matches a {@link DivArithmeticExpr} (binary division) node.
     *
     * @param f function applied when the expression is a division node,
     *          must not be {@code null}
     * @return this matcher instance for chaining
     */
    @Override
    public ArithmeticMatch<R> div(Function<DivArithmeticExpr, R> f) {
        if (!matched && expr instanceof DivArithmeticExpr e) {
            result = f.apply(e);
            matched = true;
        }
        return this;
    }

    /**
     * Matches a {@link ModArithmeticExpr} (binary modulo) node.
     *
     * <p>Note that SQL dialects may render modulo differently (e.g.
     * {@code lhs % rhs} or {@code MOD(lhs, rhs)}), but this does not
     * affect matching.</p>
     *
     * @param f function applied when the expression is a modulo node,
     *          must not be {@code null}
     * @return this matcher instance for chaining
     */
    @Override
    public ArithmeticMatch<R> mod(Function<ModArithmeticExpr, R> f) {
        if (!matched && expr instanceof ModArithmeticExpr e) {
            result = f.apply(e);
            matched = true;
        }
        return this;
    }

    /**
     * Matches a {@link NegativeArithmeticExpr} (unary negation) node.
     *
     * @param f function applied when the expression is a negation node,
     *          must not be {@code null}
     * @return this matcher instance for chaining
     */
    @Override
    public ArithmeticMatch<R> neg(Function<NegativeArithmeticExpr, R> f) {
        if (!matched && expr instanceof NegativeArithmeticExpr e) {
            result = f.apply(e);
            matched = true;
        }
        return this;
    }

    /**
     * Registers a handler for {@link PowerArithmeticExpr} expressions.
     * <p>
     * This matcher is invoked for exponentiation expressions using the
     * PostgreSQL {@code ^} operator, which has higher precedence than
     * multiplicative and additive arithmetic operators.
     *
     * @param f a function that handles {@link PowerArithmeticExpr} instances
     * @return this matcher for fluent chaining
     */
    @Override
    public ArithmeticMatch<R> pow(Function<PowerArithmeticExpr, R> f) {
        if (!matched && expr instanceof PowerArithmeticExpr e) {
            result = f.apply(e);
            matched = true;
        }
        return this;
    }

    /**
     * Terminal operation for this match chain.
     * <p>
     * Executes the first matching branch that was previously registered.
     * If none of the registered type handlers matched the input object,
     * the given fallback function will be applied.
     *
     * @param f a function providing a fallback value if no match occurred
     * @return the computed result, never {@code null} unless produced by the handler
     */
    @Override
    public R otherwise(Function<ArithmeticExpr, R> f) {
        return matched ? result : f.apply(expr);
    }
}
