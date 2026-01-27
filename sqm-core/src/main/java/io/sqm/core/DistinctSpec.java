package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.List;

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
    DistinctSpec TRUE = new Impl(List.of());
    /**
     * Marker constant representing absence of a DISTINCT specification.
     *
     * <p>A {@code null} value indicates that the SELECT query does not apply
     * any DISTINCT semantics and behaves as a regular {@code SELECT}.</p>
     */
    DistinctSpec FALSE = null;

    /**
     * Creates new instance of {@link DistinctSpec} with a list of expressions used in DISTINCT ON (a, b) clause.
     *
     * @param items a list of expressions.
     * @return new instance of {@link DistinctSpec}.
     */
    static DistinctSpec on(List<Expression> items) {
        return new Impl(items);
    }

    /**
     * Used by DISTINCT ON clause.
     * <p>
     * DISTINCT ON keeps the first row for each unique combination
     * of the provided expressions, where "first" is defined by ORDER BY.
     * <p>
     * Example:
     * SELECT DISTINCT ON (a, b) a, b, c
     * FROM t
     * ORDER BY a, b, c DESC;
     */
    List<Expression> items();

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

    /**
     * A default implementation of the {@link DistinctSpec} interface.
     *
     * @param items a list of expressions to be used in DISTINCT ON clause. Can be empty.
     */
    record Impl(List<Expression> items) implements DistinctSpec {
    }
}
