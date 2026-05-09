package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * Represents a hierarchical query clause such as Oracle {@code START WITH ... CONNECT BY ...}.
 */
public non-sealed interface HierarchicalQueryClause extends Node {

    /**
     * Creates a hierarchical query clause.
     *
     * @param startWith optional root-row predicate
     * @param connectBy parent-child relationship predicate
     * @param noCycle whether cycle detection is requested
     * @param orderSiblingsBy optional sibling ordering clause
     * @return hierarchical query clause
     */
    static HierarchicalQueryClause of(Predicate startWith, Predicate connectBy, boolean noCycle, OrderBy orderSiblingsBy) {
        return new Impl(startWith, connectBy, noCycle, orderSiblingsBy);
    }

    /**
     * Returns the optional root-row predicate.
     *
     * @return root-row predicate, or {@code null}
     */
    Predicate startWith();

    /**
     * Returns the parent-child relationship predicate.
     *
     * @return connect-by predicate
     */
    Predicate connectBy();

    /**
     * Returns whether cycle-safe traversal is requested.
     *
     * @return {@code true} when {@code NOCYCLE} is present
     */
    boolean noCycle();

    /**
     * Returns the optional sibling ordering clause.
     *
     * @return sibling ordering clause, or {@code null}
     */
    OrderBy orderSiblingsBy();

    /**
     * Accepts a visitor.
     *
     * @param v visitor instance
     * @param <R> visitor result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitHierarchicalQueryClause(this);
    }

    /**
     * Default immutable implementation.
     *
     * @param startWith optional root-row predicate
     * @param connectBy parent-child relationship predicate
     * @param noCycle whether cycle detection is requested
     * @param orderSiblingsBy optional sibling ordering clause
     */
    record Impl(Predicate startWith, Predicate connectBy, boolean noCycle, OrderBy orderSiblingsBy)
        implements HierarchicalQueryClause {

        /**
         * Creates a hierarchical query clause implementation.
         *
         * @param startWith optional root-row predicate
         * @param connectBy parent-child relationship predicate
         * @param noCycle whether cycle detection is requested
         * @param orderSiblingsBy optional sibling ordering clause
         */
        public Impl {
            Objects.requireNonNull(connectBy, "connectBy");
        }
    }
}
