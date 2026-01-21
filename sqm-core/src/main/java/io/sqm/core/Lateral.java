package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * Marks a {@link FromItem} as {@code LATERAL}.
 * <p>
 * {@code LATERAL} allows a FROM item (such as a derived table / subquery or a function table)
 * to reference columns from preceding FROM items in the same FROM clause.
 * <p>
 * This wrapper keeps {@code LATERAL} compositional: any current or future {@link FromItem}
 * can be made lateral without adding flags to multiple node types.
 * <p>
 * Examples:
 * <pre>
 * FROM LATERAL (SELECT ...) t
 * JOIN LATERAL some_function(t.col) f ON ...
 * </pre>
 */
public non-sealed interface Lateral extends TableRef {

    /**
     * Creates a new instance of the LATERAL wrapper.
     *
     * @param inner an item to wrap.
     * @return a new instance of {@link Lateral}.
     */
    static Lateral of(TableRef inner) {
        return new Impl(inner);
    }

    /**
     * The wrapped FROM item.
     */
    TableRef inner();

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
        return v.visitLateral(this);
    }

    /**
     * Default implementation.
     */
    record Impl(TableRef inner) implements Lateral {

        public Impl {
            Objects.requireNonNull(inner, "inner");
        }
    }
}
