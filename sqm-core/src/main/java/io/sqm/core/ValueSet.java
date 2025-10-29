package io.sqm.core;

/**
 * Marker for value-set expressions (e.g., IN (1,2,3), (a,b) IN ((1,2),(3,4)), IN (SELECT ...)).
 */
public sealed interface ValueSet extends Expression permits RowExpr, QueryExpr, RowListExpr {

    /**
     * Casts current expression to {@link QueryExpr}.
     *
     * @return {@link QueryExpr}.
     */
    default QueryExpr asQuery() {
        return this instanceof QueryExpr e ? e : null;
    }

    /**
     * Casts current expression to {@link RowExpr}.
     *
     * @return {@link RowExpr}.
     */
    default RowExpr asRow() {
        return this instanceof RowExpr e ? e : null;
    }

    /**
     * Casts current expression to {@link RowExpr}.
     *
     * @return {@link RowExpr}.
     */
    default RowListExpr asRows() {
        return this instanceof RowListExpr e ? e : null;
    }
}
