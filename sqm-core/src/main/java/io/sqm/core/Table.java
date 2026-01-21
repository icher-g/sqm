package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * A catalog/schema-qualified named table: expr.g. schema.table
 */
public non-sealed interface Table extends TableRef {
    /**
     * Creates a table with the provided name. All other fields are set to NULL.
     *
     * @param name the name of the table. This is not qualified name.
     * @return A newly created instance of the table.
     */
    static Table of(String name) {
        return new Impl(null, Objects.requireNonNull(name), null);
    }

    /**
     * Creates a table with the provided name. All other fields are set to NULL.
     *
     * @param name   the name of the table. This is not qualified name.
     * @param schema a table schema.
     * @return A newly created instance of the table.
     */
    static Table of(String schema, String name) {
        return new Impl(schema, Objects.requireNonNull(name), null);
    }

    /**
     * Gets a table schema.
     *
     * @return a table schema.
     */
    String schema();  // may be null

    /**
     * Gets a table name.
     *
     * @return a table name.
     */
    String name();

    /**
     * Optional table alias.
     */
    String alias();

    /**
     * Adds alias to the table.
     *
     * @param alias an alias to add.
     * @return A newly created table with the provide alias. All other fields are preserved.
     */
    default Table as(String alias) {
        return new Impl(schema(), name(), alias);
    }

    /**
     * Adds a schema to the table.
     *
     * @param schema a schema to add.
     * @return A newly created table with the provided schema. All other fields are preserved.
     */
    default Table inSchema(String schema) {
        return new Impl(schema, name(), alias());
    }

    /**
     * Accepts a {@link NodeVisitor} and dispatches control to the
     * visitor method corresponding to the concrete subtype
     *
     * @param v   the visitor instance to accept (must not be {@code null})
     * @param <R> the result type returned by the visitor
     * @return the result produced by the visitor
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitTable(this);
    }

    /**
     * An implementation class of the {@link Table}l
     *
     * @param name   the name of the table. This is not qualified name.
     * @param schema a table schema.
     * @param alias  a table alias.
     */
    record Impl(String schema, String name, String alias) implements Table {
    }
}
