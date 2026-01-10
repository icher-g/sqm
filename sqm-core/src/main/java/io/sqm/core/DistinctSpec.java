package io.sqm.core;

import io.sqm.core.internal.DistinctSpecImpl;
import io.sqm.core.walk.NodeVisitor;

/**
 * Select DISTINCT modifier.
 *
 * <p>ANSI renderer may treat any non-null implementation as plain {@code DISTINCT}.
 * Dialect renderers may render more specific forms (e.g. PostgreSQL DISTINCT ON).</p>
 */
public non-sealed interface DistinctSpec extends Node {

    /**
     * Marker constant representing presence of a DISTINCT specification.
     *
     * <p>This constant corresponds to an ANSI {@code DISTINCT} modifier
     * without any dialect-specific extensions.</p>
     */
    DistinctSpec TRUE = new DistinctSpecImpl();

    /**
     * Marker constant representing absence of a DISTINCT specification.
     *
     * <p>A {@code null} value indicates that the SELECT query does not apply
     * any DISTINCT semantics and behaves as a regular {@code SELECT}.</p>
     */
    DistinctSpec FALSE = null;

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
        return v.visitDistinctSpec(this);
    }
}
