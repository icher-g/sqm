package io.sqm.core.walk;

import io.sqm.core.HierarchicalQueryClause;

/**
 * Visitor for hierarchical query clauses.
 *
 * @param <R> the result type produced by the visitor
 */
public interface HierarchicalQueryVisitor<R> {

    /**
     * Visits a hierarchical query clause.
     *
     * @param clause hierarchical query clause
     * @return visitor result
     */
    R visitHierarchicalQueryClause(HierarchicalQueryClause clause);
}
