package io.sqm.core;

import io.sqm.core.internal.CteDefImpl;
import io.sqm.core.internal.QueryTableImpl;
import io.sqm.core.internal.WithQueryImpl;

import java.util.List;

/**
 * Represents a base interface for all queries.
 */
public sealed interface Query extends Node permits CompositeQuery, SelectQuery, WithQuery {

    /**
     * Wraps a query as a table for use in FROM statement.
     *
     * @param query a query to wrap.
     * @return A newly created instance of a wrapped query.
     */
    static QueryTable table(Query query) {
        return new QueryTableImpl(query, null);
    }

    /**
     * Creates SELECT query with a list of items.
     *
     * @param items a list of items.
     * @return A newly created instance of SELECT query.
     */
    static SelectQuery select(SelectItem... items) {
        return SelectQuery.of().select(items);
    }

    /**
     * Creates SELECT query with a list of expressions.
     *
     * @param expressions an array of expressions.
     * @return this.
     */
    static SelectQuery select(Expression... expressions) {
        return SelectQuery.of().select(expressions);
    }

    /**
     * Creates a composite query from a list of sub queries and a list of operators.
     *
     * @param terms a list of sub queries. size >= 1.
     * @param ops   a list of operators. size == terms.size()-1.
     * @return A newly created composite query.
     */
    static CompositeQuery compose(List<Query> terms, List<SetOperator> ops) {
        return CompositeQuery.of(terms, ops);
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
    static CompositeQuery compose(List<Query> terms, List<SetOperator> ops, OrderBy orderBy, LimitOffset limitOffset) {
        return CompositeQuery.of(terms, ops, orderBy, limitOffset);
    }

    /**
     * Creates a CTE definition with the provided name.
     *
     * @param name the CTE name.
     * @return A newly created CTE definition.
     */
    static CteDef cte(String name) {
        return CteDef.of(name);
    }

    /**
     * Creates a CTE definition with the provided name.
     *
     * @param name the CTE name.
     * @param body a sub query wrapped by the CTE.
     * @return A newly created CTE definition.
     */
    static CteDef cte(String name, Query body) {
        return CteDef.of(name, body);
    }

    /**
     * Creates a CTE definition with the provided name.
     *
     * @param name the CTE name.
     * @param body a sub query wrapped by the CTE.
     * @param columnAliases a list of column aliases.
     * @return A newly created CTE definition.
     */
    static CteDef cte(String name, Query body, List<String> columnAliases) {
        return new CteDefImpl(name, body, columnAliases);
    }

    /**
     * Creates a WITH query statement with the list of CTE sub queries and a body.
     *
     * @param ctes a list of CTE sub queries.
     * @param body a body.
     * @return A newly created WITH query.
     */
    static WithQuery with(List<CteDef> ctes, Query body) {
        return WithQuery.of(ctes, body);
    }

    /**
     * Creates a WITH query statement with the list of CTE sub queries and a body.
     *
     * @param ctes a list of CTE sub queries.
     * @param body a body.
     * @param recursive indicates whether the WITH statement supports recursive calls within the CTE queries.
     * @return A newly created WITH query.
     */
    static WithQuery with(List<CteDef> ctes, Query body, boolean recursive) {
        return new WithQueryImpl(ctes, body, recursive);
    }

    /**
     * Creates EXISTS predicate.
     * <p>For example:</p>
     * <pre>
     *     {@code
     *     SELECT *
     *     FROM customers c
     *     WHERE EXISTS (
     *         SELECT 1
     *         FROM orders o
     *         WHERE o.customer_id = c.id
     *     );
     *     }
     * </pre>
     *
     * @param subquery a sub query which resul to check.
     * @return A newly created EXISTS predicate.
     */
    static ExistsPredicate exists(Query subquery) {
        return ExistsPredicate.of(subquery, false);
    }

    /**
     * Creates NOT EXISTS predicate.
     * <p>For example:</p>
     * <pre>
     *     {@code
     *     SELECT *
     *     FROM customers c
     *     WHERE NOT EXISTS (
     *         SELECT 1
     *         FROM orders o
     *         WHERE o.customer_id = c.id
     *     );
     *     }
     * </pre>
     *
     * @param subquery a sub query which resul to check.
     * @return A newly created NOT EXISTS predicate.
     */
    static ExistsPredicate notExists(Query subquery) {
        return ExistsPredicate.of(subquery, true);
    }

    /**
     * Creates a UNION composite query based on the current query and the provided other query.
     *
     * @param other the other query.
     * @return A newly created instance of the composite query consisting of two queries and UNION operator.
     */
    default CompositeQuery union(Query other) {
        return CompositeQuery.of(List.of(this, other), List.of(SetOperator.UNION));
    }

    /**
     * Creates a UNION ALL composite query based on the current query and the provided other query.
     *
     * @param other the other query.
     * @return A newly created instance of the composite query consisting of two queries and UNION ALL operator.
     */
    default CompositeQuery unionAll(Query other) {
        return CompositeQuery.of(List.of(this, other), List.of(SetOperator.UNION_ALL));
    }

    /**
     * Creates a INTERSECT composite query based on the current query and the provided other query.
     *
     * @param other the other query.
     * @return A newly created instance of the composite query consisting of two queries and INTERSECT operator.
     */
    default CompositeQuery intersect(Query other) {
        return CompositeQuery.of(List.of(this, other), List.of(SetOperator.INTERSECT));
    }

    /**
     * Creates a INTERSECT ALL composite query based on the current query and the provided other query.
     *
     * @param other the other query.
     * @return A newly created instance of the composite query consisting of two queries and INTERSECT ALL operator.
     */
    default CompositeQuery intersectAll(Query other) {
        return CompositeQuery.of(List.of(this, other), List.of(SetOperator.INTERSECT_ALL));
    }

    /**
     * Creates a EXCEPT composite query based on the current query and the provided other query.
     *
     * @param other the other query.
     * @return A newly created instance of the composite query consisting of two queries and EXCEPT operator.
     */
    default CompositeQuery except(Query other) {
        return CompositeQuery.of(List.of(this, other), List.of(SetOperator.EXCEPT));
    }

    /**
     * Creates a EXCEPT ALL composite query based on the current query and the provided other query.
     *
     * @param other the other query.
     * @return A newly created instance of the composite query consisting of two queries and EXCEPT ALL operator.
     */
    default CompositeQuery exceptAll(Query other) {
        return CompositeQuery.of(List.of(this, other), List.of(SetOperator.EXCEPT_ALL));
    }
}
