package io.sqm.core.walk;

import io.sqm.core.CompositeQuery;
import io.sqm.core.Query;
import io.sqm.core.SelectQuery;
import io.sqm.core.WithQuery;

/**
 * Visitor for traversing the different kinds of {@link Query} nodes.
 * <p>
 * This interface defines the entry points for the {@code SELECT},
 * composite set operation (e.g. {@code UNION}, {@code INTERSECT}),
 * and {@code WITH} query forms.
 * <p>
 * Implementations can perform read-only analysis, validation, or transformation
 * by providing concrete behavior for the desired query variants.
 *
 * @param <R> the result type produced by the visitor
 */
public interface QueryVisitor<R> {

    /**
     * Visits a simple {@link SelectQuery}, the basic {@code SELECT ... FROM ...} form.
     *
     * @param q the select query being visited
     * @return a result produced by the visitor
     */
    R visitSelectQuery(SelectQuery q);

    /**
     * Visits a {@link CompositeQuery}, representing a set operation such as
     * {@code UNION}, {@code INTERSECT}, or {@code EXCEPT}.
     *
     * @param q the composite query being visited
     * @return a result produced by the visitor
     */
    R visitCompositeQuery(CompositeQuery q);

    /**
     * Visits a {@link WithQuery}, representing a common table expression (CTE)
     * introduced with the {@code WITH} clause.
     *
     * @param q the with query being visited
     * @return a result produced by the visitor
     */
    R visitWithQuery(WithQuery q);
}

