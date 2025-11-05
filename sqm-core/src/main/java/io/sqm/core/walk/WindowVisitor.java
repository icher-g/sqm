package io.sqm.core.walk;

import io.sqm.core.*;

/**
 * Visitor interface for SQL window constructs, including {@code WINDOW} definitions
 * and {@code OVER(...)} specifications used by analytic (window) functions.
 * <p>
 * The {@code WindowVisitor} covers the full range of ANSI SQL window elements:
 * <ul>
 *   <li>Named window definitions declared in the {@code WINDOW} clause.</li>
 *   <li>Inline or referenced {@code OVER} specifications that define how a function
 *       is evaluated over a partitioned and ordered set of rows.</li>
 *   <li>Window frame clauses and frame bounds used to restrict the window range.</li>
 * </ul>
 *
 * @param <R> the return type of the visit operations
 */
public interface WindowVisitor<R> {

    /**
     * Visits a {@link WindowDef} node representing a named window defined in the
     * {@code WINDOW} clause of a {@code SELECT} statement.
     * <p>Example SQL:</p>
     * <pre>
     * SELECT RANK() OVER w FROM employees
     * WINDOW w AS (PARTITION BY dept ORDER BY salary DESC)
     * </pre>
     *
     * @param w the window definition
     * @return a result of type {@code R}
     */
    R visitWindowDef(WindowDef w);

    /**
     * Visits an {@link OverSpec.Ref} node representing a reference to a named window,
     * as in {@code OVER w}.
     * <p>Example SQL:</p>
     * <pre>
     * RANK() OVER w
     * </pre>
     *
     * @param r the {@code OVER} reference
     * @return a result of type {@code R}
     */
    R visitOverRef(OverSpec.Ref r);

    /**
     * Visits an {@link OverSpec.Def} node representing an inline {@code OVER(...)}
     * specification attached to a window function.
     * <p>Example SQL:</p>
     * <pre>
     * SUM(amount) OVER (PARTITION BY dept ORDER BY ts)
     * </pre>
     *
     * @param d the {@code OVER} specification
     * @return a result of type {@code R}
     */
    R visitOverDef(OverSpec.Def d);

    /**
     * Visits an {@link PartitionBy} node representing a {@code PARTITION BY}
     * specification attached to a window function.
     * <p>Example SQL:</p>
     * <pre>
     * SUM(amount) OVER (PARTITION BY dept ORDER BY ts)
     * </pre>
     *
     * @param p the {@code PARTITION BY} specification
     * @return a result of type {@code R}
     */
    R visitPartitionBy(PartitionBy p);

    /**
     * Visits a {@link FrameSpec.Single} node representing a single-bound
     * window frame, such as {@code ROWS 5 PRECEDING} or {@code RANGE UNBOUNDED PRECEDING}.
     *
     * @param f the single-bound frame specification
     * @return a result of type {@code R}
     */
    R visitFrameSingle(FrameSpec.Single f);

    /**
     * Visits a {@link FrameSpec.Between} node representing a bounded window
     * frame defined with {@code BETWEEN ... AND ...}, such as:
     * {@code ROWS BETWEEN 2 PRECEDING AND CURRENT ROW}.
     *
     * @param f the between-bound frame specification
     * @return a result of type {@code R}
     */
    R visitFrameBetween(FrameSpec.Between f);

    /**
     * Visits a {@link BoundSpec.UnboundedPreceding} node representing the
     * {@code UNBOUNDED PRECEDING} frame boundary.
     *
     * @param b the frame boundary
     * @return a result of type {@code R}
     */
    R visitBoundUnboundedPreceding(BoundSpec.UnboundedPreceding b);

    /**
     * Visits a {@link BoundSpec.Preceding} node representing a frame boundary
     * defined as {@code n PRECEDING}, where {@code n} is typically a numeric expression.
     *
     * @param b the frame boundary
     * @return a result of type {@code R}
     */
    R visitBoundPreceding(BoundSpec.Preceding b);

    /**
     * Visits a {@link BoundSpec.CurrentRow} node representing the
     * {@code CURRENT ROW} frame boundary.
     *
     * @param b the frame boundary
     * @return a result of type {@code R}
     */
    R visitBoundCurrentRow(BoundSpec.CurrentRow b);

    /**
     * Visits a {@link BoundSpec.Following} node representing a frame boundary
     * defined as {@code n FOLLOWING}, where {@code n} is typically a numeric expression.
     *
     * @param b the frame boundary
     * @return a result of type {@code R}
     */
    R visitBoundFollowing(BoundSpec.Following b);

    /**
     * Visits a {@link BoundSpec.UnboundedFollowing} node representing the
     * {@code UNBOUNDED FOLLOWING} frame boundary.
     *
     * @param b the frame boundary
     * @return a result of type {@code R}
     */
    R visitBoundUnboundedFollowing(BoundSpec.UnboundedFollowing b);
}

