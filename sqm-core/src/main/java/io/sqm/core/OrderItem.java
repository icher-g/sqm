package io.sqm.core;

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
    static OrderItem of(Expression expr) {
        return of(expr, null, null, null, null, null);
    }

    /**
     * Creates an order by item for a provided expr.
     *
     * @param ordinal an ordinal to be used in an ORDER BY clause.
     * @return A newly created instance of an order by item.
     */
    static OrderItem of(int ordinal) {
        return of(null, ordinal, null, null, null, null);
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
    static OrderItem of(Expression expr, Direction direction, Nulls nulls, String collate) {
        return of(expr, null, direction, nulls, collate, null);
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
    static OrderItem of(Integer ordinal, Direction direction, Nulls nulls, String collate) {
        return of(null, ordinal, direction, nulls, collate, null);
    }

    /**
     * Creates an order by item for a provided expr with {@code USING <operator>}.
     *
     * @param expr          an expr to be used in an OrderBy clause.
     * @param usingOperator an operator to be used in {@code ORDER BY ... USING <operator>}.
     * @param nulls         the definition of how NULLs should be treated in the ORDER BY statement.
     * @param collate       collation name; may be null.
     * @return A newly created instance of an order by item.
     */
    static OrderItem of(Expression expr, String usingOperator, Nulls nulls, String collate) {
        return of(expr, null, null, nulls, collate, usingOperator);
    }

    /**
     * Creates an order by item for a provided ordinal with {@code USING <operator>}.
     *
     * @param ordinal       an ordinal to be used in an ORDER BY clause.
     * @param usingOperator an operator to be used in {@code ORDER BY ... USING <operator>}.
     * @param nulls         the definition of how NULLs should be treated in the ORDER BY statement.
     * @param collate       collation name; may be null.
     * @return A newly created instance of an order by item.
     */
    static OrderItem of(Integer ordinal, String usingOperator, Nulls nulls, String collate) {
        return of(null, ordinal, null, nulls, collate, usingOperator);
    }

    /**
     * Creates an order by item with explicit fields.
     *
     * @param expr          an expression to be used in ORDER BY statement. Can be null if ordinal is used.
     * @param ordinal       an ordinal to be used in ORDER BY statement. 1-based; may be null if expression is used.
     * @param direction     an ORDER BY direction: ASC, DESC.
     * @param nulls         the definition of how NULLs should be treated in the ORDER BY statement.
     * @param collate       optional collation name; may be null.
     * @param usingOperator optional {@code USING <operator>} value; may be null.
     * @return A newly created instance of an order by item.
     */
    static OrderItem of(Expression expr, Integer ordinal, Direction direction, Nulls nulls, String collate, String usingOperator) {
        return new Impl(expr, ordinal, direction, nulls, collate, usingOperator);
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
     *
     * @return collate string.
     */
    String collate();

    /**
     * Optional {@code USING <operator>} operator; may be null.
     *
     * @return using operator string.
     */
    String usingOperator();

    /**
     * Adds a {@link Direction#ASC} direction to an order by item.
     *
     * @return A new instance of the order item with the provided direction. All other fields are preserved.
     */
    default OrderItem asc() {
        return of(expr(), ordinal(), Direction.ASC, nulls(), collate(), usingOperator());
    }

    /**
     * Adds a {@link Direction#DESC} direction to an order by item.
     *
     * @return A new instance of the order item with the provided direction. All other fields are preserved.
     */
    default OrderItem desc() {
        return of(expr(), ordinal(), Direction.DESC, nulls(), collate(), usingOperator());
    }

    /**
     * Adds {@link Nulls} to an order by item.
     *
     * @param nulls nulls to be used in OrderBy clause.
     * @return A new instance of the order item with the provided nulls. All other fields are preserved.
     */
    default OrderItem nulls(Nulls nulls) {
        return of(expr(), ordinal(), direction(), nulls, collate(), usingOperator());
    }

    /**
     * Adds {@code NULLS FIRST} to an order by item.
     *
     * @return A new instance of the order item with {@link Nulls#FIRST}. All other fields are preserved.
     */
    default OrderItem nullsFirst() {
        return nulls(Nulls.FIRST);
    }

    /**
     * Adds {@code NULLS LAST} to an order by item.
     *
     * @return A new instance of the order item with {@link Nulls#LAST}. All other fields are preserved.
     */
    default OrderItem nullsLast() {
        return nulls(Nulls.LAST);
    }

    /**
     * Resets nulls handling to dialect default behavior.
     *
     * @return A new instance of the order item with {@link Nulls#DEFAULT}. All other fields are preserved.
     */
    default OrderItem nullsDefault() {
        return nulls(Nulls.DEFAULT);
    }

    /**
     * Adds collate to an order by item.
     *
     * @param collate collate to be used in OrderBy clause.
     * @return A new instance of the order item with the provided collate. All other fields are preserved.
     */
    default OrderItem collate(String collate) {
        return of(expr(), ordinal(), direction(), nulls(), collate, usingOperator());
    }

    /**
     * Adds {@code USING <operator>} to an order by item.
     *
     * @param operator operator to be used in {@code ORDER BY ... USING <operator>}.
     * @return A new instance of the order item with the provided operator. All other fields are preserved.
     */
    default OrderItem using(String operator) {
        return of(expr(), ordinal(), direction(), nulls(), collate(), operator);
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
     *
     * @param expr      an expression to be used in ORDER BY statement. Can be null if ordinal is used.
     * @param ordinal   an ordinal to be used in ORDER BY statement. 1-based; may be null if expression is used.
     * @param direction an ORDER BY direction: ASC, DESC.
     * @param nulls     the definition of how NULLs should be treated in the ORDER BY statement.
     * @param collate       optional collation name; may be null.
     * @param usingOperator optional {@code USING <operator>} value; may be null.
     */
    record Impl(Expression expr, Integer ordinal, Direction direction, Nulls nulls, String collate, String usingOperator) implements OrderItem {
    }
}
