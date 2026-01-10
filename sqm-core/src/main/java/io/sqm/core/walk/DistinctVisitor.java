package io.sqm.core.walk;

import io.sqm.core.DistinctSpec;
import io.sqm.core.SelectQuery;

/**
 * Visitor for {@link DistinctSpec} nodes.
 *
 * <p>This visitor is responsible for handling SELECT-level DISTINCT modifiers,
 * including ANSI {@code DISTINCT} and dialect-specific extensions such as
 * PostgreSQL {@code DISTINCT ON (...)}.</p>
 *
 * <p>The visitor is typically invoked while visiting a {@link SelectQuery},
 * before traversing other SELECT components such as select items, FROM, WHERE,
 * or ORDER BY clauses.</p>
 *
 * <p>Implementations may choose to:</p>
 * <ul>
 *   <li>Handle all {@link DistinctSpec} implementations uniformly
 *       (e.g. render any DISTINCT as ANSI {@code DISTINCT}), or</li>
 *   <li>Dispatch based on the concrete implementation type
 *       (e.g. render {@code DISTINCT ON} for PostgreSQL).</li>
 * </ul>
 *
 * <p>This interface is intentionally minimal. Dialect-specific visitors may
 * extend it to provide strongly typed visit methods for concrete
 * {@link DistinctSpec} implementations.</p>
 *
 * @param <R> return type of the visitor
 */
public interface DistinctVisitor<R> {
    /**
     * Visits a {@link DistinctSpec} instance.
     *
     * @param spec DISTINCT specification to visit
     * @return visitor-specific result
     */
    R visitDistinctSpec(DistinctSpec spec);
}

