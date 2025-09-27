package io.cherlabs.sqlmodel.core.views;

import io.cherlabs.sqlmodel.core.Filter;
import io.cherlabs.sqlmodel.core.Join;
import io.cherlabs.sqlmodel.core.Table;
import io.cherlabs.sqlmodel.core.traits.HasExpr;
import io.cherlabs.sqlmodel.core.traits.HasJoinOn;
import io.cherlabs.sqlmodel.core.traits.HasJoinType;
import io.cherlabs.sqlmodel.core.traits.HasTable;

import java.util.Optional;

public final class Joins {
    private Joins() {
    }

    public static Optional<String> expr(Join j) {
        if (j instanceof HasExpr h) return Optional.ofNullable(h.expression());
        return Optional.empty();
    }

    public static Optional<Join.JoinType> joinType(Join j) {
        if (j instanceof HasJoinType h) return Optional.ofNullable(h.joinType());
        return Optional.empty();
    }

    public static Optional<Table> table(Join j) {
        if (j instanceof HasTable h) return Optional.ofNullable(h.table());
        return Optional.empty();
    }

    public static Optional<Filter> on(Join j) {
        if (j instanceof HasJoinOn h) return Optional.ofNullable(h.on());
        return Optional.empty();
    }
}
