package io.sqm.core.walk;

import io.sqm.core.*;

/**
 * Visitor for traversing elements of the {@code FROM} clause, including tables and joins.
 * <p>
 * Implementations can analyze, validate, or transform the logical source tree
 * of a query by providing behavior for specific {@link FromItem} subtypes.
 *
 * @param <R> the result type produced by the visitor
 */
public interface FromVisitor<R> {

    /**
     * Visits a base {@link Table} reference.
     *
     * @param t the table being visited
     * @return a result produced by the visitor
     */
    R visitTable(Table t);

    /**
     * Visits a {@link QueryTable}, representing a derived table
     * or subquery used in the {@code FROM} clause.
     *
     * @param t the query table being visited
     * @return a result produced by the visitor
     */
    R visitQueryTable(QueryTable t);

    /**
     * Visits a {@link ValuesTable}, representing an inline {@code VALUES} construct.
     *
     * @param t the values table being visited
     * @return a result produced by the visitor
     */
    R visitValuesTable(ValuesTable t);

    /**
     * Visits an {@link OnJoin}, a join with an {@code ON} predicate and a specific join kind
     * (INNER, LEFT, RIGHT, or FULL).
     *
     * @param j the join being visited
     * @return a result produced by the visitor
     */
    R visitOnJoin(OnJoin j);

    /**
     * Visits a {@link CrossJoin}, representing a {@code CROSS JOIN} between two sources.
     *
     * @param j the cross join being visited
     * @return a result produced by the visitor
     */
    R visitCrossJoin(CrossJoin j);

    /**
     * Visits a {@link NaturalJoin}, representing a {@code NATURAL JOIN}.
     *
     * @param j the natural join being visited
     * @return a result produced by the visitor
     */
    R visitNaturalJoin(NaturalJoin j);

    /**
     * Visits a {@link UsingJoin}, representing a join with a {@code USING(...)} clause.
     *
     * @param j the using join being visited
     * @return a result produced by the visitor
     */
    R visitUsingJoin(UsingJoin j);
}

