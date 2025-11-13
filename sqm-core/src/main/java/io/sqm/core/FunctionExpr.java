package io.sqm.core;

import io.sqm.core.internal.FuncStarArg;
import io.sqm.core.internal.FunctionArgExpr;
import io.sqm.core.internal.FunctionExprImpl;
import io.sqm.core.match.FunctionExprArgMatch;
import io.sqm.core.walk.NodeVisitor;

import java.util.List;
import java.util.Objects;

/**
 * Represents a function call.
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     COUNT(*);
 *     UPPER(products.name)
 *     COUNT(DISTINCT t.id) AS c
 *     }
 * </pre>
 */
public non-sealed interface FunctionExpr extends Expression {
    /**
     * Creates a function call expression.
     *
     * @param name a function name
     * @param args an array of function arguments.
     * @return A newly created instance of a function call expression.
     */
    static FunctionExpr of(String name, FunctionExpr.Arg... args) {
        return new FunctionExprImpl(Objects.requireNonNull(name), List.of(args), null, null, null, null);
    }

    /**
     * Creates a function call expression.
     *
     * @param name        a function name
     * @param args        an array of function arguments.
     * @param distinctArg indicates whether DISTINCT should be added before the list of arguments in the function call. {@code COUNT(DISTINCT t.id) AS c}.
     * @param withinGroup defines an ordered-set aggregates.
     * @param filter      a filter used to filter rows only for aggregates.
     * @param over        an OVER specification.
     * @return A newly created instance of a function call expression.
     */
    static FunctionExpr of(String name, List<FunctionExpr.Arg> args, Boolean distinctArg, OrderBy withinGroup, Predicate filter, OverSpec over) {
        return new FunctionExprImpl(Objects.requireNonNull(name), args, distinctArg, withinGroup, filter, over);
    }

    /**
     * Gets the name of the function.
     *
     * @return the name of the function.
     */
    String name();

    /**
     * Gets a list of arguments. Can be NULL or empty if there are no arguments.
     *
     * @return a list of arguments if exists or NULL otherwise.
     */
    List<Arg> args();

    /**
     * Indicates whether DISTINCT should be added before the list of arguments in the function call.
     * {@code COUNT(DISTINCT t.id) AS c}
     *
     * @return True if DISTINCT needs to be added and False otherwise.
     */
    Boolean distinctArg();

    /**
     * Gets a WITHIN GROUP definition if provided or NULL otherwise.
     * <p>Example:</p>
     * <pre>
     *     {@code
     *     PERCENTILE_CONT(0.9) WITHIN GROUP (ORDER BY salary)
     *     MODE() WITHIN GROUP (ORDER BY value)
     *     RANK(75) WITHIN GROUP (ORDER BY score)
     *     }
     * </pre>
     *
     * @return a WITHIN GROUP definition or NULL>
     */
    OrderBy withinGroup();

    /**
     * Gets a FILTER definition if provided or NULL otherwise.
     * <p>Example:</p>
     * <pre>
     *     {@code
     *     COUNT(*) FILTER (WHERE status = 'active')
     *     SUM(amount) FILTER (WHERE amount > 0)
     *     COUNT(DISTINCT user_id) FILTER (WHERE created_at >= DATE '2025-01-01')
     *     }
     * </pre>
     *
     * @return a FILTER definition or NULL.
     */
    Predicate filter();

    /**
     * Gets an OVER specification if provided or NULL otherwise.
     * <p>Example:</p>
     * <pre>
     *     {@code
     *     ROW_NUMBER() OVER (PARTITION BY dept ORDER BY salary)
     *     SUM(amount) OVER (PARTITION BY acct_id ORDER BY ts ROWS BETWEEN 2 PRECEDING AND CURRENT ROW)
     *     AVG(sales) FILTER (WHERE region = 'EU') OVER (PARTITION BY year)
     *     }
     * </pre>
     *
     * @return an OVER specification or NULL.
     */
    OverSpec over();

    /**
     * Adds DISTINCT indication to an expression.
     *
     * @return A newly created instance of the function call expression with the added indication. All other fields are preserved.
     */
    default FunctionExpr distinct() {
        return new FunctionExprImpl(name(), args(), true, withinGroup(), filter(), over());
    }

    /**
     * Adds WITHIN GROUP statement to the function expression.
     * <p>Example:</p>
     * <pre>
     *     {@code
     *     PERCENTILE_CONT(0.9) WITHIN GROUP (ORDER BY salary)
     *     MODE() WITHIN GROUP (ORDER BY value)
     *     RANK(75) WITHIN GROUP (ORDER BY score)
     *     }
     * </pre>
     *
     * @param withinGroup a statement to add.
     * @return this.
     */
    default FunctionExpr withinGroup(OrderBy withinGroup) {
        return new FunctionExprImpl(name(), args(), distinctArg(), withinGroup, filter(), over());
    }

    /**
     * Adds WITHIN GROUP statement to the function expression.
     * <p>Example:</p>
     * <pre>
     *     {@code
     *     PERCENTILE_CONT(0.9) WITHIN GROUP (ORDER BY salary)
     *     MODE() WITHIN GROUP (ORDER BY value)
     *     RANK(75) WITHIN GROUP (ORDER BY score)
     *     }
     * </pre>
     *
     * @param items a list of ORDER BY items to add.
     * @return this.
     */
    default FunctionExpr withinGroup(OrderItem... items) {
        return new FunctionExprImpl(name(), args(), distinctArg(), OrderBy.of(List.of(items)), filter(), over());
    }

    /**
     * Adds a FILTER statement to the function expression.
     * <p>Example:</p>
     * <pre>
     *     {@code
     *     COUNT(*) FILTER (WHERE status = 'active')
     *     SUM(amount) FILTER (WHERE amount > 0)
     *     COUNT(DISTINCT user_id) FILTER (WHERE created_at >= DATE '2025-01-01')
     *     }
     * </pre>
     *
     * @param filter a statement.
     * @return this.
     */
    default FunctionExpr filter(Predicate filter) {
        return new FunctionExprImpl(name(), args(), distinctArg(), withinGroup(), filter, over());
    }

    /**
     * Adds an OVER statement to the function expression.
     * <p>Example:</p>
     * <pre>
     *     {@code
     *     ROW_NUMBER() OVER (PARTITION BY dept ORDER BY salary)
     *     SUM(amount) OVER (PARTITION BY acct_id ORDER BY ts ROWS BETWEEN 2 PRECEDING AND CURRENT ROW)
     *     AVG(sales) FILTER (WHERE region = 'EU') OVER (PARTITION BY year)
     *     }
     * </pre>
     *
     * @param over a statement.
     * @return this.
     */
    default FunctionExpr over(OverSpec over) {
        return new FunctionExprImpl(name(), args(), distinctArg(), withinGroup(), filter(), over);
    }

    /**
     * References a named window from the {@code WINDOW} clause.
     * <p>Example SQL:</p>
     * <pre>
     * SUM(salary) OVER w
     * </pre>
     *
     * @param windowName the name of the referenced window
     * @return an {@link FunctionExpr}
     */
    default FunctionExpr over(String windowName) {
        return over(OverSpec.ref(windowName));
    }

    /**
     * Adds an inline {@code OVER(...)} specification with {@code PARTITION BY}.
     * <p>Example SQL:</p>
     * <pre>
     * SUM(amount) OVER (PARTITION BY acct)
     * </pre>
     *
     * @param partitionBy the partition-by specification
     * @return an {@link FunctionExpr}
     */
    default FunctionExpr over(PartitionBy partitionBy) {
        return over(OverSpec.def(partitionBy, null, null, null));
    }

    /**
     * Adds an inline {@code OVER(...)} specification with {@code PARTITION BY} and {@code ORDER BY}.
     * <p>Example SQL:</p>
     * <pre>
     * RANK() OVER (PARTITION BY dept ORDER BY salary DESC)
     * </pre>
     *
     * @param partitionBy the partition-by specification
     * @param orderBy     the order-by specification
     * @return an {@link FunctionExpr}
     */
    default FunctionExpr over(PartitionBy partitionBy, OrderBy orderBy) {
        return over(OverSpec.def(partitionBy, orderBy, null, null));
    }

    /**
     * Adds an inline {@code OVER(...)} specification including a window frame.
     * <p>Example SQL:</p>
     * <pre>
     * SUM(amount) OVER (PARTITION BY acct ORDER BY ts ROWS 5 PRECEDING)
     * </pre>
     *
     * @param partitionBy the partition-by specification
     * @param orderBy     the order-by specification
     * @param frame       the frame specification
     * @return an {@link FunctionExpr}
     */
    default FunctionExpr over(PartitionBy partitionBy, OrderBy orderBy, FrameSpec frame) {
        return over(OverSpec.def(partitionBy, orderBy, frame, null));
    }

    /**
     * Adds an inline {@code OVER(...)} specification including a window frame.
     * <p>Example SQL:</p>
     * <pre>
     * SUM(amount) OVER (PARTITION BY acct ORDER BY ts ROWS 5 PRECEDING)
     * </pre>
     *
     * @param partitionBy the partition-by specification
     * @param frame       the frame specification
     * @return an {@link FunctionExpr}
     */
    default FunctionExpr over(PartitionBy partitionBy, FrameSpec frame) {
        return over(OverSpec.def(partitionBy, null, frame, null));
    }

    /**
     * Adds an inline {@code OVER(...)} specification including a frame and an exclusion clause.
     * <p>Example SQL:</p>
     * <pre>
     * RANK() OVER (PARTITION BY grp ORDER BY score DESC GROUPS BETWEEN 1 PRECEDING AND 1 FOLLOWING EXCLUDE TIES)
     * </pre>
     *
     * @param partitionBy the partition-by specification
     * @param orderBy     the order-by specification
     * @param frame       the frame specification
     * @param exclude     the exclusion clause
     * @return an {@link FunctionExpr}
     */
    default FunctionExpr over(PartitionBy partitionBy, OrderBy orderBy, FrameSpec frame, OverSpec.Exclude exclude) {
        return over(OverSpec.def(partitionBy, orderBy, frame, exclude));
    }

    /**
     * Adds an {@code OVER(...)} specification extending a base window name.
     * <p>Example SQL:</p>
     * <pre>
     * SUM(salary) OVER (w ORDER BY ts)
     * </pre>
     *
     * @param baseWindow the referenced base window name
     * @param orderBy    an additional order-by clause
     * @return an {@link FunctionExpr}
     */
    default FunctionExpr over(String baseWindow, OrderBy orderBy) {
        return over(OverSpec.def(baseWindow, orderBy, null, null));
    }

    /**
     * Adds an {@code OVER(...)} specification extending a base window name with a frame.
     * <p>Example SQL:</p>
     * <pre>
     * SUM(amount) OVER (w ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)
     * </pre>
     *
     * @param baseWindow the referenced base window name
     * @param orderBy    an optional order-by clause
     * @param frame      the frame specification
     * @return an {@link FunctionExpr}
     */
    default FunctionExpr over(String baseWindow, OrderBy orderBy, FrameSpec frame) {
        return over(OverSpec.def(baseWindow, orderBy, frame, null));
    }

    /**
     * Adds an {@code OVER(...)} specification extending a base window name with a frame.
     * <p>Example SQL:</p>
     * <pre>
     * SUM(amount) OVER (w ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW)
     * </pre>
     *
     * @param baseWindow the referenced base window name
     * @param frame      the frame specification
     * @return an {@link FunctionExpr}
     */
    default FunctionExpr over(String baseWindow, FrameSpec frame) {
        return over(OverSpec.def(baseWindow, null, frame, null));
    }

    /**
     * Adds an {@code OVER(...)} specification extending a base window with a frame and exclusion.
     * <p>Example SQL:</p>
     * <pre>
     * SUM(amount) OVER (w ROWS BETWEEN 2 PRECEDING AND 2 FOLLOWING EXCLUDE CURRENT ROW)
     * </pre>
     *
     * @param baseWindow the base window name
     * @param orderBy    an optional order-by clause
     * @param frame      the frame specification
     * @param exclude    the exclusion clause
     * @return an {@link FunctionExpr}
     */
    default FunctionExpr over(String baseWindow, OrderBy orderBy, FrameSpec frame, OverSpec.Exclude exclude) {
        return over(OverSpec.def(baseWindow, orderBy, frame, exclude));
    }

    /**
     * Accepts a {@link NodeVisitor} and dispatches control to the
     * visitor method corresponding to the concrete subtype
     *
     * @param v   the visitor instance to accept (must not be {@code null})
     * @param <R> the result type returned by the visitor
     * @return the result produced by the visitor
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitFunctionExpr(this);
    }

    /**
     * Represents a base interface for function argument.
     */
    sealed interface Arg extends Expression permits Arg.ExprArg, Arg.StarArg {
        /**
         * Creates column function argument.
         *
         * @param e a column reference.
         * @return a column argument.
         */
        static ExprArg expr(Expression e) {
            return new FunctionArgExpr(e);
        }

        /**
         * Creates a '*' argument.
         *
         * @return a star argument.
         */
        static StarArg star() {
            return new FuncStarArg();
        }

        /**
         * Accepts a {@link NodeVisitor} and dispatches control to the
         * visitor method corresponding to the concrete subtype
         *
         * @param v   the visitor instance to accept (must not be {@code null})
         * @param <R> the result type returned by the visitor
         * @return the result produced by the visitor
         */
        @Override
        default <R> R accept(NodeVisitor<R> v) {
            return v.visitFunctionArgExpr(this);
        }

        /**
         * Creates a new matcher for the current {@link FunctionExpr.Arg}.
         *
         * @param <R> the result type produced by the match
         * @return a new {@code FunctionExprArgMatch} for current argument.
         */
        default <R> FunctionExprArgMatch<R> matchArg() {
            return FunctionExprArgMatch.match(this);
        }

        /**
         * Column reference argument: t.c or just c.
         */
        non-sealed interface ExprArg extends FunctionExpr.Arg {
            /**
             * Gets an inner expression.
             *
             * @return an inner expression.
             */
            Expression expr();
        }

        /**
         * The '*' argument (expr.g., COUNT(*))
         */
        non-sealed interface StarArg extends FunctionExpr.Arg {
        }
    }
}
