package io.sqm.core;

import io.sqm.core.internal.CompositeQueryImpl;
import io.sqm.core.internal.OrderByImpl;
import io.sqm.core.walk.NodeVisitor;

import java.util.List;

/**
 * Represents a composite query.
 * <p>Example:</p>
 * <pre>
 *     {@code
 *     (SELECT * FROM TABLE1)
 *     UNION
 *     (SELECT * FROM TABLE2)
 *     INTERSECT
 *     (SELECT * FROM TABLE3)
 *     }
 * </pre>
 */
public non-sealed interface CompositeQuery extends Query {

    /**
     * Creates a composite query from a list of sub queries and a list of operators.
     *
     * @param terms a list of sub queries. size >= 1.
     * @param ops   a list of operators. size == terms.size()-1.
     * @return A newly created composite query.
     */
    static CompositeQuery of(List<Query> terms, List<SetOperator> ops) {
        return new CompositeQueryImpl(terms, ops, null, null);
    }

    /**
     * Creates a composite query from a list of sub queries and a list of operators.
     *
     * @param terms a list of sub queries. size >= 1.
     * @param ops   a list of operators. size == terms.size()-1.
     * @param orderBy an OrderBy statement. Can be NULL.
     * @param limitOffset a limit and offest definition.
     * @return A newly created composite query.
     */
    static CompositeQuery of(List<Query> terms, List<SetOperator> ops, OrderBy orderBy, LimitOffset limitOffset) {
        return new CompositeQueryImpl(terms, ops, orderBy, limitOffset);
    }

    /**
     * Gets a list of sub queries.
     *
     * @return a list of sub queries.
     */
    List<Query> terms();

    /**
     * Gets a list of operations. size = terms.size() - 1
     *
     * @return a list of operations.
     */
    List<SetOperator> ops();

    /**
     * Gets an OrderBy statement. Can be NULL.
     *
     * @return an OrderBy statement.
     */
    OrderBy orderBy();

    /**
     * Gets a limit and offest definition.
     *
     * @return limit/offset definition or NULL if is not defined.
     */
    LimitOffset limitOffset();

    /**
     * Adds an OrderBy statement to the composite query.
     *
     * @param items a list of items in the OrderBy statement.
     * @return A new instance of the composite query with the provided OrderBy items. All the rest of the fields are preserved.
     */
    default CompositeQuery orderBy(List<OrderItem> items) {
        return new CompositeQueryImpl(terms(), ops(), new OrderByImpl(items), limitOffset());
    }

    /**
     * Adds an OrderBy statement to the composite query.
     *
     * @param items a list of items in the OrderBy statement.
     * @return A new instance of the composite query with the provided OrderBy items. All the rest of the fields are preserved.
     */
    default CompositeQuery orderBy(OrderItem... items) {
        return new CompositeQueryImpl(terms(), ops(), new OrderByImpl(List.of(items)), limitOffset());
    }

    /**
     * Adds a limit to the composite query.
     *
     * @param limit a limit to add to the OrderBy statement.
     * @return A new instance of the composite query with the provided limit. All the rest of the fields are preserved.
     */
    default CompositeQuery limit(Long limit) {
        return new CompositeQueryImpl(terms(), ops(), orderBy(), LimitOffset.of(limit, limitOffset() == null ? null : limitOffset().offset()));
    }

    /**
     * Adds an offset to the composite query.
     *
     * @param offset an offset to add to the OrderBy statement.
     * @return A new instance of the composite query with the provided offset. All the rest of the fields are preserved.
     */
    default CompositeQuery offset(Long offset) {
        return new CompositeQueryImpl(terms(), ops(), orderBy(), LimitOffset.of(limitOffset() == null ? null : limitOffset().limit(), offset));
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
        return v.visitCompositeQuery(this);
    }
}
