package io.sqm.core.views;

import io.sqm.core.Filter;
import io.sqm.core.Join;
import io.sqm.core.Table;
import io.sqm.core.traits.HasExpr;
import io.sqm.core.traits.HasJoinOn;
import io.sqm.core.traits.HasJoinType;
import io.sqm.core.traits.HasTable;

import java.util.Optional;

/**
 * A view that provides access to {@link Join} properties.
 */
public final class Joins {
    private Joins() {
    }

    /**
     * Gets an expr from the join implemented by one of the derived types.
     * @param j a join.
     * @return {@link Optional} with the expr if presented or an {@link Optional#empty()}.
     */
    public static Optional<String> expr(Join j) {
        if (j instanceof HasExpr h) return Optional.ofNullable(h.expr());
        return Optional.empty();
    }

    /**
     * Gets a join type from the join implemented by one of the derived types.
     * @param j a join.
     * @return {@link Optional} with the join type if presented or an {@link Optional#empty()}.
     */
    public static Optional<Join.JoinType> joinType(Join j) {
        if (j instanceof HasJoinType h) return Optional.ofNullable(h.joinType());
        return Optional.empty();
    }

    /**
     * Gets a table from the join implemented by one of the derived types.
     * @param j a join.
     * @return {@link Optional} with the table if presented or an {@link Optional#empty()}.
     */
    public static Optional<Table> table(Join j) {
        if (j instanceof HasTable h) return Optional.ofNullable(h.table());
        return Optional.empty();
    }

    /**
     * Gets an on filter from the join implemented by one of the derived types.
     * @param j a join.
     * @return {@link Optional} with the on filter if presented or an {@link Optional#empty()}.
     */
    public static Optional<Filter> on(Join j) {
        if (j instanceof HasJoinOn h) return Optional.ofNullable(h.on());
        return Optional.empty();
    }
}
