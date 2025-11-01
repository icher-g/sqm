package io.sqm.core;

import io.sqm.core.internal.GroupItemImpl;
import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * Either expression-based grouping or ordinal reference. Exactly one is non-null.
 */
public non-sealed interface GroupItem extends Node {
    /**
     * Creates a group by item from column.
     *
     * @param expr a colum to group by
     * @return A newly created instance of a group item.
     */
    static GroupItem by(Expression expr) {
        return new GroupItemImpl(Objects.requireNonNull(expr), null);
    }

    /**
     * Creates a group by item from ordinal.
     *
     * @param ordinal an ordinal to group by.
     * @return A newly created instance of a group item.
     */
    static GroupItem by(int ordinal) {
        return new GroupItemImpl(null, ordinal);
    }

    Expression expr(); // may be null if ordinal is used

    Integer ordinal();       // 1-based; may be null if expression is used

    /**
     * Indicates if the {@link GroupItem} is represented by ordinal.
     *
     * @return True if the group is represented by ordinal or False otherwise.
     */
    default boolean isOrdinal() {
        return ordinal() != null;
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
        return v.visitGroupItem(this);
    }
}
