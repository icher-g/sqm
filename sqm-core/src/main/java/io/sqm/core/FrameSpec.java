package io.sqm.core;

import io.sqm.core.internal.FrameSpecBetween;
import io.sqm.core.internal.FrameSpecSingle;
import io.sqm.core.match.FrameSpecMatch;
import io.sqm.core.walk.NodeVisitor;

/**
 * An interface to define the frame specification in an OVER statement.
 * <p>Example:</p>
 * <pre>
 *     {@code
 *     SUM(salary) OVER (w1 ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS running_sum
 *     }
 * </pre>
 */
public sealed interface FrameSpec extends Node permits FrameSpec.Single, FrameSpec.Between {
    /**
     * Creates a single frame.
     * <p>Example:</p>
     * <pre>
     *     {@code
     *     ROWS 5 PRECEDING
     *     RANGE UNBOUNDED PRECEDING
     *     }
     * </pre>
     *
     * @param unit  a frame unit.
     * @param bound a frame bound.
     * @return a new instance of {@link Single} frame specification.
     */
    static Single single(Unit unit, BoundSpec bound) {
        return new FrameSpecSingle(unit, bound);
    }

    /**
     * Creates a between frame.
     * <p>Example:</p>
     * <pre>
     *     {@code
     *     SUM(salary) OVER (w1 ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS running_sum
     *     }
     * </pre>
     *
     * @param unit  a frame unit.
     * @param start the start of the frame.
     * @param end   the end of the frame.
     * @return a new instance of {@link Between} frame specification.
     */
    static Between between(Unit unit, BoundSpec start, BoundSpec end) {
        return new FrameSpecBetween(unit, start, end);
    }

    /**
     * Creates a new matcher for the current {@link FrameSpec}.
     *
     * @param <R> the result type
     * @return a new {@code FrameSpecMatch}.
     */
    default <R> FrameSpecMatch<R> matchFrameSpec() {
        return FrameSpecMatch.match(this);
    }

    /**
     * Gets a {@link Unit} used in the current frame specification.
     *
     * @return a unit.
     */
    Unit unit();

    /**
     * Defines the unit type.
     */
    enum Unit {
        /**
         * Rows
         */
        ROWS,
        /**
         * Range
         */
        RANGE,
        /**
         * Groups
         */
        GROUPS
    }

    /**
     * ROWS bound  (shorthand for BETWEEN bound AND CURRENT ROW in many engines is NOT standard)
     */
    non-sealed interface Single extends FrameSpec {

        /**
         * Gets a frame unit.
         *
         * @return a frame unit.
         */
        Unit unit();

        /**
         * Gets a frame bound.
         *
         * @return a frame bound.
         */
        BoundSpec bound();

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
            return v.visitFrameSingle(this);
        }
    }

    /**
     * ROWS BETWEEN start AND end
     */
    non-sealed interface Between extends FrameSpec {

        /**
         * Gets a frame unit.
         *
         * @return a frame unit.
         */
        Unit unit();

        /**
         * Gets a start bound.
         *
         * @return a start bound.
         */
        BoundSpec start();

        /**
         * Gets an end bound.
         *
         * @return an end bound.
         */
        BoundSpec end();

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
            return v.visitFrameBetween(this);
        }
    }
}
