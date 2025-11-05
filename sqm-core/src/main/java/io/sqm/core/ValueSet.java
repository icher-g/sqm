package io.sqm.core;

import java.util.Optional;

/**
 * Marker for value-set expressions (expr.g., IN (1,2,3), (a,b) IN ((1,2),(3,4)), IN (SELECT ...)).
 */
public sealed interface ValueSet extends Expression permits RowExpr, QueryExpr, RowListExpr {

    /**
     * Casts current expression to {@link QueryExpr} if possible.
     *
     * @return {@link Optional}<{@link QueryExpr}>.
     */
    default Optional<QueryExpr> asQuery() {
        return this instanceof QueryExpr e ? Optional.of(e) : Optional.empty();
    }

    /**
     * Casts current expression to {@link RowExpr} if possible.
     *
     * @return {@link Optional}<{@link RowExpr}>.
     */
    default Optional<RowExpr> asRow() {
        return this instanceof RowExpr e ? Optional.of(e) : Optional.empty();
    }

    /**
     * Casts current expression to {@link RowListExpr} if possible.
     *
     * @return {@link Optional}<{@link RowListExpr}>.
     */
    default Optional<RowListExpr> asRows() {
        return this instanceof RowListExpr e ? Optional.of(e) : Optional.empty();
    }
}
