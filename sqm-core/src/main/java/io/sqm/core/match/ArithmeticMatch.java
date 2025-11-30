package io.sqm.core.match;

import io.sqm.core.*;
import io.sqm.core.walk.NodeVisitor;

import java.util.function.Function;

/**
 * A typed match interface for {@link ArithmeticExpr} nodes.
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
public interface ArithmeticMatch<R> extends Match<ArithmeticExpr, R> {
    /**
     * Creates a new matcher for the given {@link ArithmeticExpr}.
     *
     * @param e   the expression to match on (maybe any concrete {@code ArithmeticExpr} subtype)
     * @param <R> the result type produced by the match
     * @return a new {@code ArithmeticMatch} for {@code e}
     */
    static <R> ArithmeticMatch<R> match(ArithmeticExpr e) {
        return new ArithmeticMatchImpl<>(e);
    }

    /**
     * Matches a {@link AddArithmeticExpr} (binary addition) node.
     *
     * @param f function applied when the expression is an addition node,
     *          must not be {@code null}
     * @return this matcher instance for chaining
     */
    ArithmeticMatch<R> add(Function<AddArithmeticExpr, R> f);

    /**
     * Matches a {@link SubArithmeticExpr} (binary subtraction) node.
     *
     * @param f function applied when the expression is a subtraction node,
     *          must not be {@code null}
     * @return this matcher instance for chaining
     */
    ArithmeticMatch<R> sub(Function<SubArithmeticExpr, R> f);

    /**
     * Matches a {@link MulArithmeticExpr} (binary multiplication) node.
     *
     * @param f function applied when the expression is a multiplication node,
     *          must not be {@code null}
     * @return this matcher instance for chaining
     */
    ArithmeticMatch<R> mul(Function<MulArithmeticExpr, R> f);

    /**
     * Matches a {@link DivArithmeticExpr} (binary division) node.
     *
     * @param f function applied when the expression is a division node,
     *          must not be {@code null}
     * @return this matcher instance for chaining
     */
    ArithmeticMatch<R> div(Function<DivArithmeticExpr, R> f);

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
    ArithmeticMatch<R> mod(Function<ModArithmeticExpr, R> f);

    /**
     * Matches a {@link NegativeArithmeticExpr} (unary negation) node.
     *
     * @param f function applied when the expression is a negation node,
     *          must not be {@code null}
     * @return this matcher instance for chaining
     */
    ArithmeticMatch<R> neg(Function<NegativeArithmeticExpr, R> f);
}

