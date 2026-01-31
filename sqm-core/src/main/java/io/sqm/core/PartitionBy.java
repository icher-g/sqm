package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.List;

/**
 * Represents a PARTITION BY statement used in OVER();
 * <p>For example:</p>
 * <pre>
 *     {@code
 *     SUM(salary) OVER (PARTITION BY dept ORDER BY salary DESC RANGE UNBOUNDED PRECEDING)
 *     AVG(salary) OVER (PARTITION BY dept ORDER BY salary ROWS CURRENT ROW)
 *     }
 * </pre>
 */
public non-sealed interface PartitionBy extends Node {

    /**
     * Creates a PARTITION BY statement with the provided list of expressions.
     *
     * @param items a list of expressions.
     * @return a new instance of {@link PartitionBy}.
     */
    static PartitionBy of(Expression... items) {
        return new Impl(List.of(items));
    }

    /**
     * Creates a PARTITION BY statement with the provided list of expressions.
     *
     * @param items a list of expressions.
     * @return a new instance of {@link PartitionBy}.
     */
    static PartitionBy of(List<Expression> items) {
        return new Impl(items);
    }

    /**
     * Gets a list of expressions used in PARTITION BY statement.
     *
     * @return a list of expressions.
     */
    List<Expression> items();

    /**
     * Accepts a {@link NodeVisitor} that performs an operation on this node.
     * <p>
     * Each concrete node class calls back into the visitor with a type-specific
     * {@code visitXxx(...)} method, allowing the visitor to handle each node
     * type appropriately.
     * </p>
     *
     * @param v   the visitor instance to accept (must not be {@code null})
     * @param <R> the result type produced by the visitor
     * @return the result of the visitor’s operation on this node,
     * or {@code null} if the visitor’s return type is {@link Void}
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitPartitionBy(this);
    }

    /**
     * Implements a PARTITION BY statement used in OVER();
     * <p>For example:</p>
     * <pre>
     *     {@code
     *     SUM(salary) OVER (PARTITION BY dept ORDER BY salary DESC RANGE UNBOUNDED PRECEDING)
     *     AVG(salary) OVER (PARTITION BY dept ORDER BY salary ROWS CURRENT ROW)
     *     }
     * </pre>
     *
     * @param items a list of expressions used in PARTITION BY statement.
     */
    record Impl(List<Expression> items) implements PartitionBy {

        /**
         * Creates a partition by implementation.
         *
         * @param items partition expressions
         */
        public Impl {
            items = List.copyOf(items);
        }
    }
}
