package io.sqm.core;

import io.sqm.core.internal.OverSpecDef;
import io.sqm.core.internal.OverSpecRef;
import io.sqm.core.walk.NodeVisitor;

/**
 * Represent an OVER statement in a function call.
 * <p>Example:</p>
 * <pre>
 *     {@code
 *     ROW_NUMBER() OVER (PARTITION BY dept ORDER BY salary)
 *     SUM(amount) OVER (PARTITION BY acct_id ORDER BY ts ROWS BETWEEN 2 PRECEDING AND CURRENT ROW)
 *     AVG(sales) FILTER (WHERE region = 'EU') OVER (PARTITION BY year)
 *     }
 * </pre>
 */
public sealed interface OverSpec extends Node permits OverSpec.Ref, OverSpec.Def {

    /**
     * References a named window from the {@code WINDOW} clause.
     * <p>Example SQL:</p>
     * <pre>
     * SUM(salary) OVER w
     * </pre>
     *
     * @param windowName the name of the referenced window
     * @return an {@link OverSpec.Ref}
     */
    static Ref ref(String windowName) {
        return new OverSpecRef(windowName);
    }

    /**
     * Creates an inline {@code OVER(...)} specification including a frame and an exclusion clause.
     * <p>Example SQL:</p>
     * <pre>
     * RANK() OVER (PARTITION BY grp ORDER BY score DESC GROUPS BETWEEN 1 PRECEDING AND 1 FOLLOWING EXCLUDE TIES)
     * </pre>
     *
     * @param partitionBy the partition-by specification
     * @param orderBy     the order-by specification
     * @param frame       the frame specification
     * @param exclude     the exclusion clause
     * @return an {@link OverSpec.Def}
     */
    static Def def(PartitionBy partitionBy, OrderBy orderBy, FrameSpec frame, Exclude exclude) {
        return new OverSpecDef(null, partitionBy, orderBy, frame, exclude);
    }

    /**
     * Creates an {@code OVER(...)} specification extending a base window with a frame and exclusion.
     * <p>Example SQL:</p>
     * <pre>
     * SUM(amount) OVER (w ROWS BETWEEN 2 PRECEDING AND 2 FOLLOWING EXCLUDE CURRENT ROW)
     * </pre>
     *
     * @param baseWindow the base window name
     * @param orderBy    an optional order-by clause
     * @param frame      the frame specification
     * @param exclude    the exclusion clause
     * @return an {@link OverSpec.Def}
     */
    static Def def(String baseWindow, OrderBy orderBy, FrameSpec frame, Exclude exclude) {
        return new OverSpecDef(baseWindow, null, orderBy, frame, exclude);
    }

    // EXCLUDE { CURRENT ROW | GROUP | TIES | NO OTHERS }
    enum Exclude {CURRENT_ROW, GROUP, TIES, NO_OTHERS}

    /**
     * OVER ( ... ) — inline window specification.
     * <p>Example:</p>
     * <pre>
     *     {@code
     *     SUM(salary) OVER (PARTITION BY dept ORDER BY hire_date)
     *     }
     * </pre>
     */
    non-sealed interface Def extends OverSpec {
        /**
         * Gets the base window name. {@code OVER (w ORDER BY ... ROWS ...)}
         *
         * @return a base window name.
         */
        String baseWindow();

        /**
         * Gets a {@code PARTITION BY e1, e2, ...} statement.
         *
         * @return a partition by statement.
         */
        PartitionBy partitionBy();

        /**
         * Gets an {@code ORDER BY x [ASC|DESC] [NULLS {FIRST|LAST}], ...} statement.
         *
         * @return an order by statement.
         */
        OrderBy orderBy();

        /**
         * Gets a frame definition. {@code ROWS/RANGE/GROUPS frame (bounds + exclusion)}
         *
         * @return a frame definition.
         */
        FrameSpec frame();

        /**
         * Gets an exclusion.
         *
         * @return an exclusion.
         */
        Exclude exclude();

        /**
         * Accepts a {@link NodeVisitor} that performs an operation on this node.
         * <p>
         * Each concrete node class calls back into the visitor with a type-specific
         * {@code visitXxx(...)} method, allowing the visitor to handle each node
         * type appropriately.
         * </p>
         *
         * @param v the visitor instance to accept (must not be {@code null})
         * @return the result of the visitor’s operation on this node,
         * or {@code null} if the visitor’s return type is {@link Void}
         */
        @Override
        default <R> R accept(NodeVisitor<R> v) {
            return v.visitOverDef(this);
        }
    }

    /**
     * OVER w — reference a named window from the SELECT's WINDOW clause
     * <p>Example:</p>
     * <pre>
     *     {@code
     *     AVG(salary) OVER w
     *     }
     * </pre>
     */
    non-sealed interface Ref extends OverSpec {

        /**
         * Gets a window name.
         *
         * @return a window name.
         */
        String windowName();

        /**
         * Accepts a {@link NodeVisitor} that performs an operation on this node.
         * <p>
         * Each concrete node class calls back into the visitor with a type-specific
         * {@code visitXxx(...)} method, allowing the visitor to handle each node
         * type appropriately.
         * </p>
         *
         * @param v the visitor instance to accept (must not be {@code null})
         * @return the result of the visitor’s operation on this node,
         * or {@code null} if the visitor’s return type is {@link Void}
         */
        @Override
        default <R> R accept(NodeVisitor<R> v) {
            return v.visitOverRef(this);
        }
    }
}
