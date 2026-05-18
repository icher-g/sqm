package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * Existence-test output column inside a {@code JSON_TABLE} column list.
 */
public non-sealed interface JsonTableExistsColumn extends JsonTableColumn {
    /**
     * Creates an existence-test JSON table column.
     *
     * @param name column name
     * @param type SQL result type
     * @param path JSON path
     * @return existence-test JSON table column
     */
    static JsonTableExistsColumn of(
        Identifier name,
        TypeName type,
        JsonPathSpec path
    ) {
        return new Impl(name, type, path, null);
    }

    /**
     * Creates an existence-test JSON table column.
     *
     * @param name    column name
     * @param type    SQL result type
     * @param path    JSON path
     * @param onError behavior for errors, or {@code null} for dialect default
     * @return existence-test JSON table column
     */
    static JsonTableExistsColumn of(
        Identifier name,
        TypeName type,
        JsonPathSpec path,
        JsonTableBehavior onError
    ) {
        return new Impl(name, type, path, onError);
    }

    /**
     * Returns the column name.
     *
     * @return column name
     */
    Identifier name();

    /**
     * Returns the SQL result type.
     *
     * @return SQL type
     */
    TypeName type();

    /**
     * Returns the JSON path evaluated relative to the current row pattern.
     *
     * @return JSON path
     */
    JsonPathSpec path();

    /**
     * Returns the error handling behavior.
     *
     * @return error behavior or {@code null}
     */
    JsonTableBehavior onError();

    /**
     * Accepts a node visitor.
     *
     * @param v   visitor
     * @param <R> visitor result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitJsonTableExistsColumn(this);
    }

    /**
     * Immutable JSON_TABLE EXISTS column implementation.
     *
     * @param name    column name
     * @param type    SQL result type
     * @param path    JSON path evaluated relative to the current row pattern
     * @param onError behavior for errors, or {@code null} for dialect default
     */
    record Impl(
        Identifier name,
        TypeName type,
        JsonPathSpec path,
        JsonTableBehavior onError
    ) implements JsonTableExistsColumn {
        /**
         * Creates an immutable JSON_TABLE EXISTS column.
         */
        public Impl {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(path, "path");
        }
    }
}