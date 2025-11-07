package io.sqm.core;

import io.sqm.core.internal.*;
import io.sqm.core.walk.NodeVisitor;

import java.util.Optional;

/**
 * Determines which rows are visible to the function for each current row.
 * Frame bounds like UNBOUNDED PRECEDING, 5 PRECEDING, CURRENT ROW, etc.
 */
public sealed interface BoundSpec extends Node permits BoundSpec.UnboundedPreceding, BoundSpec.Preceding, BoundSpec.CurrentRow, BoundSpec.Following, BoundSpec.UnboundedFollowing {

    /**
     * Creates UNBOUNDED PRECEDING bound.
     *
     * @return a new instance of {@link UnboundedPreceding}.
     */
    static UnboundedPreceding unboundedPreceding() {
        return new BoundSpecUnboundedPreceding();
    }

    /**
     * Creates PRECEDING bound with the provided expression.
     *
     * @param e an expression to use in a bound.
     * @return a new instance of {@link Preceding}.
     */
    static Preceding preceding(Expression e) {
        return new BoundSpecPreceding(e);
    }

    /**
     * Creates CURRENT ROW bound.
     *
     * @return a new instance of {@link CurrentRow}.
     */
    static CurrentRow currentRow() {
        return new BoundSpecCurrentRow();
    }

    /**
     * Creates FOLLOWING bound with the provided expression.
     *
     * @param e an expression to use in a bound.
     * @return a new instance of {@link Following}.
     */
    static Following following(Expression e) {
        return new BoundSpecFollowing(e);
    }

    /**
     * Creates UNBOUNDED FOLLOWING bound.
     *
     * @return a new instance of {@link UnboundedFollowing}.
     */
    static UnboundedFollowing unboundedFollowing() {
        return new BoundSpecUnboundedFollowing();
    }

    /**
     * Casts this to {@link BoundSpec.Preceding}.
     *
     * @return an {@link Optional}<{@link BoundSpec.Preceding}> if the cast is successful or {@link Optional#empty()} otherwise.
     */
    default Optional<BoundSpec.Preceding> asPreceding() {
        return this instanceof BoundSpec.Preceding b ? Optional.of(b) : Optional.empty();
    }

    /**
     * Casts this to {@link BoundSpec.Following}.
     *
     * @return an {@link Optional}<{@link BoundSpec.Following}> if the cast is successful or {@link Optional#empty()} otherwise.
     */
    default Optional<BoundSpec.Following> asFollowing() {
        return this instanceof BoundSpec.Following b ? Optional.of(b) : Optional.empty();
    }

    /**
     * Casts this to {@link BoundSpec.CurrentRow}.
     *
     * @return an {@link Optional}<{@link BoundSpec.CurrentRow}> if the cast is successful or {@link Optional#empty()} otherwise.
     */
    default Optional<BoundSpec.CurrentRow> asCurrentRow() {
        return this instanceof BoundSpec.CurrentRow b ? Optional.of(b) : Optional.empty();
    }

    /**
     * Casts this to {@link BoundSpec.UnboundedPreceding}.
     *
     * @return an {@link Optional}<{@link BoundSpec.UnboundedPreceding}> if the cast is successful or {@link Optional#empty()} otherwise.
     */
    default Optional<BoundSpec.UnboundedPreceding> asUnboundedPreceding() {
        return this instanceof BoundSpec.UnboundedPreceding b ? Optional.of(b) : Optional.empty();
    }

    /**
     * Casts this to {@link BoundSpec.UnboundedFollowing}.
     *
     * @return an {@link Optional}<{@link BoundSpec.UnboundedFollowing}> if the cast is successful or {@link Optional#empty()} otherwise.
     */
    default Optional<BoundSpec.UnboundedFollowing> asUnboundedFollowing() {
        return this instanceof BoundSpec.UnboundedFollowing b ? Optional.of(b) : Optional.empty();
    }

    /**
     * Represents an UNBOUNDED PRECEDING bound.
     * <p>For example:</p>
     * <pre>
     *     {@code
     *     -- All rows up to current
     *     SUM(salary) OVER (PARTITION BY dept ORDER BY salary DESC RANGE UNBOUNDED PRECEDING)
     *     }
     * </pre>
     */
    non-sealed interface UnboundedPreceding extends BoundSpec {
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
            return v.visitBoundUnboundedPreceding(this);
        }
    }

    /**
     * Represents a PRECEDING bound.
     * <p>For example:</p>
     * <pre>
     *     {@code
     *     -- Last 5 rows
     *     SUM(salary) OVER (PARTITION BY dept ORDER BY salary DESC ROWS 5 PRECEDING)
     *     }
     * </pre>
     */
    non-sealed interface Preceding extends BoundSpec {

        /**
         * Gets an expression used in a bound.
         *
         * @return an expression.
         */
        Expression expr();

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
            return v.visitBoundPreceding(this);
        }
    }        // usually integer literal; keep Expression for flexibility

    /**
     * Represents a CURRENT ROW bound.
     * <p>For example:</p>
     * <pre>
     *     {@code
     *     -- Only current row
     *     AVG(salary) OVER (PARTITION BY dept ORDER BY salary ROWS CURRENT ROW)
     *     }
     * </pre>
     */
    non-sealed interface CurrentRow extends BoundSpec {
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
            return v.visitBoundCurrentRow(this);
        }
    }

    /**
     * Represents a FOLLOWING bound.
     * <p>For example:</p>
     * <pre>
     *     {@code
     *     -- Next 3 rows
     *     AVG(salary) OVER (PARTITION BY dept ORDER BY salary ROWS 3 FOLLOWING)
     *     }
     * </pre>
     */
    non-sealed interface Following extends BoundSpec {

        /**
         * Gets an expression used in a bound.
         *
         * @return an expression.
         */
        Expression expr();

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
            return v.visitBoundFollowing(this);
        }
    }

    /**
     * Represents an UNBOUNDED FOLLOWING bound.
     * <p>For example:</p>
     * <pre>
     *     {@code
     *     -- All future rows
     *     MAX(value) OVER (ORDER BY ts RANGE UNBOUNDED FOLLOWING)
     *     }
     * </pre>
     */
    non-sealed interface UnboundedFollowing extends BoundSpec {
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
            return v.visitBoundUnboundedFollowing(this);
        }
    }
}
