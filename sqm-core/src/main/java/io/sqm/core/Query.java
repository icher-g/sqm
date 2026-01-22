package io.sqm.core;

import io.sqm.core.match.QueryMatch;

import java.util.List;

/**
 * Represents a base interface for all queries.
 */
public sealed interface Query extends Node permits CompositeQuery, DialectQuery, SelectQuery, WithQuery {
    /**
     * Creates SELECT query with a list of expressions.
     *
     * @param nodes an array of expressions.
     * @return this.
     */
    static SelectQuery select(Node... nodes) {
        return SelectQuery.of().select(nodes);
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
     * @param terms       a list of sub queries. size >= 1.
     * @param ops         a list of operators. size == terms.size()-1.
     * @param orderBy     an OrderBy statement. Can be NULL.
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
     * @param name          the CTE name.
     * @param body          a sub query wrapped by the CTE.
     * @param columnAliases a list of column aliases.
     * @return A newly created CTE definition.
     */
    static CteDef cte(String name, Query body, List<String> columnAliases) {
        return CteDef.of(name, body, columnAliases);
    }

    /**
     * Creates a WITH query statement with the list of CTE sub queries and a body.
     *
     * @param ctes a list of CTE sub queries.
     * @return A newly created WITH query.
     */
    static WithQuery with(CteDef... ctes) {
        return WithQuery.of(ctes);
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
     * @param ctes      a list of CTE sub queries.
     * @param body      a body.
     * @param recursive indicates whether the WITH statement supports recursive calls within the CTE queries.
     * @return A newly created WITH query.
     */
    static WithQuery with(List<CteDef> ctes, Query body, boolean recursive) {
        return WithQuery.of(ctes, body, recursive);
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

    /**
     * Creates a new matcher for the current {@link Query}.
     *
     * @param <R> the result type
     * @return a new {@code QueryMatch}.
     */
    default <R> QueryMatch<R> matchQuery() {
        return QueryMatch.match(this);
    }
}
