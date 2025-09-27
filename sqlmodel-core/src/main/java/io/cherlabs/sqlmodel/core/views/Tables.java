package io.cherlabs.sqlmodel.core.views;

import io.cherlabs.sqlmodel.core.Query;
import io.cherlabs.sqlmodel.core.Table;
import io.cherlabs.sqlmodel.core.traits.HasAlias;
import io.cherlabs.sqlmodel.core.traits.HasName;
import io.cherlabs.sqlmodel.core.traits.HasQuery;
import io.cherlabs.sqlmodel.core.traits.HasSchema;

import java.util.Optional;

public final class Tables {
    private Tables() {
    }

    public static Optional<String> name(Table t) {
        if (t instanceof HasName h) return Optional.ofNullable(h.name());
        return Optional.empty();
    }

    public static Optional<String> alias(Table t) {
        if (t instanceof HasAlias h) return Optional.ofNullable(h.alias());
        return Optional.empty();
    }

    public static Optional<String> schema(Table t) {
        if (t instanceof HasSchema h) return Optional.ofNullable(h.schema());
        return Optional.empty();
    }

    public static Optional<Query> query(Table t) {
        if (t instanceof HasQuery h) return Optional.ofNullable(h.query());
        return Optional.empty();
    }
}
