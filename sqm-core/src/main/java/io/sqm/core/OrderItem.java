package io.sqm.core;

import io.sqm.core.internal.OrderItemImpl;
import io.sqm.core.walk.NodeVisitor;

/**
 * Represents an order by item.
 *
 * <p>Example:</p>
 * <pre>
 *     {@code
 *     ORDER BY products.name
 *     ORDER BY 1, 2
 *     }
 * </pre>
 */
public non-sealed interface OrderItem extends Node {
    /**
     * Creates an order by item for a provided expr.
     *
     * @param expr an expr to be used in an OrderBy clause.
     * @return A newly created instance of an order by item.
     */
    static OrderItem by(Expression expr) {
        return new OrderItemImpl(expr, null, null, null, null);
    }

    /**
     * Creates an order by item for a provided expr.
     *
     * @param ordinal an ordinal to be used in an ORDER BY clause.
     * @return A newly created instance of an order by item.
     */
    static OrderItem by(int ordinal) {
        return new OrderItemImpl(null, ordinal, null, null, null);
    }

    /**
     * Creates an order by item for a provided expr.
     *
     * @param expr      an expr to be used in an OrderBy clause.
     * @param direction an ORDER BY direction: ASC, DESC.
     * @param nulls     the definition of how NULLs should be treated in the ORDER BY statement.
     * @param collate   collation name; may be null.
     * @return A newly created instance of an order by item.
     */
    static OrderItem by(Expression expr, Direction direction, Nulls nulls, String collate) {
        return new OrderItemImpl(expr, null, direction, nulls, collate);
    }

    /**
     * Creates an order by item for a provided expr.
     *
     * @param ordinal   an ordinal to be used in an ORDER BY clause.
     * @param direction an ORDER BY direction: ASC, DESC.
     * @param nulls     the definition of how NULLs should be treated in the ORDER BY statement.
     * @param collate   collation name; may be null.
     * @return A newly created instance of an order by item.
     */
    static OrderItem by(Integer ordinal, Direction direction, Nulls nulls, String collate) {
        return new OrderItemImpl(null, ordinal, direction, nulls, collate);
    }

    /**
     * Gets an expression to be used in ORDER BY statement. Can be null if ordinal is used.
     * <p>Example:</p>
     * <pre>
     *     {@code ORDER BY products.name}
     * </pre>
     *
     * @return an expression.
     */
    Expression expr();

    /**
     * Gets an ordinal to be used in ORDER BY statement. 1-based; may be null if expression is used.
     * <p>Example:</p>
     * <pre>
     *     {@code ORDER BY 1}
     * </pre>
     *
     * @return an ordinal.
     */
    Integer ordinal();

    /**
     * Gets an ORDER BY direction: ASC, DESC.
     *
     * @return a direction.
     */
    Direction direction();

    /**
     * Gets the definition of how NULLs should be treated in the ORDER BY statement.
     *
     * @return a NULLs definition.
     */
    Nulls nulls();

    /**
     * Optional collation name; may be null.
     */
    String collate();

    /**
     * Adds a {@link Direction#ASC} direction to an order by item.
     *
     * @return A new instance of the order item with the provided direction. All other fields are preserved.
     */
    default OrderItem asc() {
        return new OrderItemImpl(expr(), ordinal(), Direction.ASC, nulls(), collate());
    }

    /**
     * Adds a {@link Direction#DESC} direction to an order by item.
     *
     * @return A new instance of the order item with the provided direction. All other fields are preserved.
     */
    default OrderItem desc() {
        return new OrderItemImpl(expr(), ordinal(), Direction.DESC, nulls(), collate());
    }

    /**
     * Adds {@link Nulls} to an order by item.
     *
     * @param nulls nulls to be used in OrderBy clause.
     * @return A new instance of the order item with the provided nulls. All other fields are preserved.
     */
    default OrderItem nulls(Nulls nulls) {
        return new OrderItemImpl(expr(), ordinal(), direction(), nulls, collate());
    }

    /**
     * Adds collate to an order by item.
     *
     * @param collate collate to be used in OrderBy clause.
     * @return A new instance of the order item with the provided collate. All other fields are preserved.
     */
    default OrderItem collate(String collate) {
        return new OrderItemImpl(expr(), ordinal(), direction(), nulls(), collate);
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
        return v.visitOrderItem(this);
    }
}
