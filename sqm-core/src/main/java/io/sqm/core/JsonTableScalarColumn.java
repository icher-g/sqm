package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * Scalar output column inside a {@code JSON_TABLE} column list.
 */
public non-sealed interface JsonTableScalarColumn extends JsonTableColumn {
    /**
     * Creates a scalar JSON table column.
     *
     * @param name column name
     * @param type SQL result type
     * @param path JSON path
     * @return scalar JSON table column
     */
    static JsonTableScalarColumn of(
        Identifier name,
        TypeName type,
        JsonPathSpec path
    ) {
        return new Impl(name, type, path, Wrapper.DEFAULT, null, null);
    }

    /**
     * Creates a scalar JSON table column.
     *
     * @param name    column name
     * @param type    SQL result type
     * @param path    JSON path evaluated relative to the current row pattern
     * @param wrapper wrapper behavior
     * @param onEmpty behavior for empty results, or {@code null} for dialect default
     * @param onError behavior for errors, or {@code null} for dialect default
     * @return scalar JSON table column
     */
    static JsonTableScalarColumn of(
        Identifier name,
        TypeName type,
        JsonPathSpec path,
        Wrapper wrapper,
        JsonTableBehavior onEmpty,
        JsonTableBehavior onError
    ) {
        return new Impl(name, type, path, wrapper, onEmpty, onError);
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
     * Returns the wrapper behavior.
     *
     * @return wrapper behavior
     */
    Wrapper wrapper();

    /**
     * Returns the empty-result behavior.
     *
     * @return empty-result behavior or {@code null}
     */
    JsonTableBehavior onEmpty();

    /**
     * Returns the error behavior.
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
        return v.visitJsonTableScalarColumn(this);
    }

    /**
     * JSON array wrapper behavior for scalar JSON table columns.
     */
    enum Wrapper {
        /**
         * Dialect default wrapper behavior.
         */
        DEFAULT,

        /**
         * Explicit {@code WITHOUT WRAPPER}.
         */
        WITHOUT,

        /**
         * Explicit unconditional {@code WITH WRAPPER}.
         */
        WITH,

        /**
         * Explicit {@code WITH CONDITIONAL WRAPPER}.
         */
        CONDITIONAL
    }

    /**
     * Immutable JSON_TABLE scalar column implementation.
     *
     * @param name    column name
     * @param type    SQL result type
     * @param path    JSON path evaluated relative to the current row pattern
     * @param wrapper wrapper behavior
     * @param onEmpty behavior for empty results, or {@code null} for dialect default
     * @param onError behavior for errors, or {@code null} for dialect default
     */
    record Impl(
        Identifier name,
        TypeName type,
        JsonPathSpec path,
        Wrapper wrapper,
        JsonTableBehavior onEmpty,
        JsonTableBehavior onError
    ) implements JsonTableScalarColumn {
        /**
         * Creates an immutable JSON_TABLE scalar column.
         */
        public Impl {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(path, "path");
            wrapper = wrapper == null ? Wrapper.DEFAULT : wrapper;
        }
    }
}