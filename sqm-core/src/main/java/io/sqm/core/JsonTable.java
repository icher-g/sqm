package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.List;
import java.util.Objects;

/**
 * Table reference produced by a SQL/JSON {@code JSON_TABLE} expression.
 */
public non-sealed interface JsonTable extends TableRef {
    /**
     * Creates a JSON table reference without an alias.
     *
     * @param json JSON expression that supplies the context item
     * @param rootPath row-pattern JSON path
     * @param columns output column definitions
     * @return JSON table reference
     */
    static JsonTable of(Expression json, JsonPathSpec rootPath, List<JsonTableColumn> columns) {
        return of(json, rootPath, columns, null);
    }

    /**
     * Creates a JSON table reference.
     *
     * @param json JSON expression that supplies the context item
     * @param rootPath row-pattern JSON path
     * @param columns output column definitions
     * @param alias optional table alias
     * @return JSON table reference
     */
    static JsonTable of(Expression json, JsonPathSpec rootPath, List<JsonTableColumn> columns, Identifier alias) {
        return new Impl(json, rootPath, columns, alias);
    }

    /**
     * Returns the JSON context expression.
     *
     * @return JSON context expression
     */
    Expression json();

    /**
     * Returns the row-pattern JSON path.
     *
     * @return row-pattern JSON path
     */
    JsonPathSpec rootPath();

    /**
     * Returns output column definitions.
     *
     * @return immutable output column definitions
     */
    List<JsonTableColumn> columns();

    /**
     * Returns the table alias.
     *
     * @return alias, or {@code null}
     */
    Identifier alias();

    /**
     * Creates a copy with the provided table alias.
     *
     * @param alias table alias
     * @return JSON table reference with alias
     */
    default JsonTable as(String alias) {
        return as(alias == null ? null : Identifier.of(alias));
    }

    /**
     * Creates a copy with the provided table alias.
     *
     * @param alias table alias
     * @return JSON table reference with alias
     */
    default JsonTable as(Identifier alias) {
        return of(json(), rootPath(), columns(), alias);
    }

    /**
     * Accepts a node visitor.
     *
     * @param v visitor
     * @param <R> visitor result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitJsonTableRef(this);
    }

    /**
     * Immutable JSON table reference implementation.
     *
     * @param json JSON expression that supplies the context item
     * @param rootPath row-pattern JSON path
     * @param columns output column definitions
     * @param alias optional table alias
     */
    record Impl(Expression json, JsonPathSpec rootPath, List<JsonTableColumn> columns, Identifier alias) implements JsonTable {
        /**
         * Creates an immutable JSON table reference.
         */
        public Impl {
            Objects.requireNonNull(json, "json");
            Objects.requireNonNull(rootPath, "rootPath");
            Objects.requireNonNull(columns, "columns");
            if (columns.isEmpty()) {
                throw new IllegalArgumentException("JSON_TABLE requires at least one column");
            }
            columns = List.copyOf(columns);
        }
    }
}
