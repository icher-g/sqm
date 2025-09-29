package io.cherlabs.sqlmodel.core.views;

import io.cherlabs.sqlmodel.core.Query;
import io.cherlabs.sqlmodel.core.Table;
import io.cherlabs.sqlmodel.core.traits.HasAlias;
import io.cherlabs.sqlmodel.core.traits.HasName;
import io.cherlabs.sqlmodel.core.traits.HasQuery;
import io.cherlabs.sqlmodel.core.traits.HasSchema;

import java.util.Optional;

/**
 * A view that provides access to {@link Table} properties.
 */
public final class Tables {
    private Tables() {
    }

    /**
     * Gets the table name implemented by one of the derived types if presented.
     * @param t a table
     * @return {@link Optional} with the table name if presented or an {@link Optional#empty()}.
     */
    public static Optional<String> name(Table t) {
        if (t instanceof HasName h) return Optional.ofNullable(h.name());
        return Optional.empty();
    }

    /**
     * Gets the table alias implemented by one of the derived types if presented.
     * @param t a table
     * @return {@link Optional} with the table alias if presented or an {@link Optional#empty()}.
     */
    public static Optional<String> alias(Table t) {
        if (t instanceof HasAlias h) return Optional.ofNullable(h.alias());
        return Optional.empty();
    }

    /**
     * Gets the table schema implemented by one of the derived types if presented.
     * @param t a table
     * @return {@link Optional} with the table schema if presented or an {@link Optional#empty()}.
     */
    public static Optional<String> schema(Table t) {
        if (t instanceof HasSchema h) return Optional.ofNullable(h.schema());
        return Optional.empty();
    }

    /**
     * Gets the sub query implemented by one of the derived types if presented.
     * @param t a table
     * @return {@link Optional} with the sub query if presented or an {@link Optional#empty()}.
     */
    public static Optional<Query<?>> query(Table t) {
        if (t instanceof HasQuery h) return Optional.ofNullable(h.query());
        return Optional.empty();
    }
}
