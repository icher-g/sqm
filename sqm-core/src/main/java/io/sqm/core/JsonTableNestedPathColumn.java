package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.List;
import java.util.Objects;

/**
 * Nested path column group inside a {@code JSON_TABLE} column list.
 */
public non-sealed interface JsonTableNestedPathColumn extends JsonTableColumn {
    /**
     * Creates a nested-path JSON table column group.
     *
     * @param path    nested JSON path
     * @param columns nested output columns
     * @return nested-path JSON table column group
     */
    static JsonTableNestedPathColumn of(JsonPathSpec path, List<JsonTableColumn> columns) {
        return new Impl(path, columns);
    }

    /**
     * Returns the nested JSON path.
     *
     * @return nested JSON path
     */
    JsonPathSpec path();

    /**
     * Returns the nested output columns.
     *
     * @return nested output columns
     */
    List<JsonTableColumn> columns();

    /**
     * Nested path groups do not define a direct visible column name.
     *
     * @return {@code null}
     */
    @Override
    default Identifier name() {
        return null;
    }

    /**
     * Accepts a node visitor.
     *
     * @param v   visitor
     * @param <R> visitor result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitJsonTableNestedPathColumn(this);
    }

    /**
     * Immutable nested-path JSON_TABLE column group implementation.
     *
     * @param path    nested JSON path
     * @param columns nested output columns
     */
    record Impl(JsonPathSpec path, List<JsonTableColumn> columns) implements JsonTableNestedPathColumn {
        /**
         * Creates an immutable nested-path JSON_TABLE column group.
         */
        public Impl {
            Objects.requireNonNull(path, "path");
            Objects.requireNonNull(columns, "columns");

            if (columns.isEmpty()) {
                throw new IllegalArgumentException(
                    "Nested JSON_TABLE path requires at least one column"
                );
            }

            columns = List.copyOf(columns);
        }
    }
}