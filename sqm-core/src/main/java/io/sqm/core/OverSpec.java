package io.sqm.core;

import io.sqm.core.match.OverSpecMatch;
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
public sealed interface OverSpec extends Node permits DialectOverSpec, OverSpec.Def, OverSpec.Ref {

    /**
     * References a named window from the {@code WINDOW} clause.
     *
     * @param windowName the name identifier of the referenced window
     * @return an {@link OverSpec.Ref}
     */
    static Ref ref(Identifier windowName) {
        return new Ref.Impl(windowName);
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
        return new Def.Impl(null, partitionBy, orderBy, frame, exclude);
    }

    /**
     * Creates an {@code OVER(...)} specification extending a base window with a frame and exclusion.
     *
     * @param baseWindow the base window identifier
     * @param orderBy    an optional order-by clause
     * @param frame      the frame specification
     * @param exclude    the exclusion clause
     * @return an {@link OverSpec.Def}
     */
    static Def def(Identifier baseWindow, OrderBy orderBy, FrameSpec frame, Exclude exclude) {
        return new Def.Impl(baseWindow, null, orderBy, frame, exclude);
    }

    /**
     * Creates a new matcher for the current {@link OverSpec}.
     *
     * @param <R> the result type
     * @return a new {@code OverSpecMatch}.
     */
    default <R> OverSpecMatch<R> matchOverSpec() {
        return OverSpecMatch.match(this);
    }

    /**
     * EXCLUDE { CURRENT ROW | GROUP | TIES | NO OTHERS }
     */
    enum Exclude {
        /**
         * EXCLUDE CURRENT ROW
         */
        CURRENT_ROW,
        /**
         * EXCLUDE GROUP
         */
        GROUP,
        /**
         * EXCLUDE TIES
         */
        TIES,
        /**
         * EXCLUDE NO OTHERS
         */
        NO_OTHERS
    }

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
        Identifier baseWindow();

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

        /**
         * OVER ( ... ) — inline window specification.
         * <p>Example:</p>
         * <pre>
         *     {@code
         *     SUM(salary) OVER (PARTITION BY dept ORDER BY hire_date)
         *     }
         * </pre>
         *
         * @param baseWindow  the base window name. {@code OVER (w ORDER BY ... ROWS ...)}
         * @param partitionBy a {@code PARTITION BY e1, e2, ...} statement.
         * @param orderBy     an {@code ORDER BY x [ASC|DESC] [NULLS {FIRST|LAST}], ...} statement.
         * @param frame       a frame definition. {@code ROWS/RANGE/GROUPS frame (bounds + exclusion)}
         * @param exclude     an exclusion.
         */
        record Impl(Identifier baseWindow, PartitionBy partitionBy, OrderBy orderBy, FrameSpec frame, OverSpec.Exclude exclude) implements OverSpec.Def {
            /**
             * Creates an inline window definition.
             *
             * @param baseWindow  a base window identifier
             * @param partitionBy a PARTITION BY statement
             * @param orderBy     an ORDER BY statement
             * @param frame       a frame definition
             * @param exclude     an exclusion
             */
            public Impl {
            }
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
        Identifier windowName();

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

        /**
         * OVER w — reference a named window from the SELECT's WINDOW clause
         * <p>Example:</p>
         * <pre>
         *     {@code
         *     AVG(salary) OVER w
         *     }
         * </pre>
         *
         * @param windowName a window name.
         */
        record Impl(Identifier windowName) implements OverSpec.Ref {
            /**
             * Creates a named window reference.
             *
             * @param windowName a window name identifier
             */
            public Impl {
                java.util.Objects.requireNonNull(windowName, "windowName");
            }
        }
    }
}
