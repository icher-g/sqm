package io.sqm.core.walk;

import io.sqm.core.Node;

/**
 * Visitor for traversing all {@link Node} nodes in the SQM model.
 * <p>
 * This interface provides type-specific callbacks for every concrete expression
 * variant. It enables type-safe processing of SQL expression trees without
 * explicit {@code instanceof} checks.
 *
 * @param <R> the result type produced by the visitor
 */
public interface NodeVisitor<R> extends
    ExpressionVisitor<R>,
    PredicateVisitor<R>,
    FromVisitor<R>,
    SelectItemVisitor<R>,
    GroupVisitor<R>,
    OrderVisitor<R>,
    PaginationVisitor<R>,
    StatementVisitor<R>,
    QueryVisitor<R> {
}
