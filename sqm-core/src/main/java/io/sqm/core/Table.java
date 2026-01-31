package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * A catalog/schema-qualified named table: expr.g. schema.table
 */
public non-sealed interface Table extends TableRef {
    /**
     * Table inheritance handling for PostgreSQL.
     */
    enum Inheritance {
        /**
         * Default behavior (dialect-defined).
         */
        DEFAULT,
        /**
         * Use ONLY to exclude child tables (PostgreSQL).
         */
        ONLY,
        /**
         * Explicitly include child tables via {@code table *} (PostgreSQL).
         */
        INCLUDE_DESCENDANTS
    }
    /**
     * Creates a table with the provided name. All other fields are set to NULL.
     *
     * @param name the name of the table. This is not qualified name.
     * @return A newly created instance of the table.
     */
    static Table of(String name) {
        return of(null, Objects.requireNonNull(name), null, Inheritance.DEFAULT);
    }

    /**
     * Creates a table with the provided name. All other fields are set to NULL.
     *
     * @param name   the name of the table. This is not qualified name.
     * @param schema a table schema.
     * @return A newly created instance of the table.
     */
    static Table of(String schema, String name) {
        return of(schema, Objects.requireNonNull(name), null, Inheritance.DEFAULT);
    }

    /**
     * Creates a table with the provided name.
     *
     * @param schema      a table schema.
     * @param name        the table name (unqualified).
     * @param alias       optional table alias.
     * @param inheritance inheritance behavior.
     * @return A newly created instance of the table.
     */
    static Table of(String schema, String name, String alias, Inheritance inheritance) {
        return new Impl(schema, Objects.requireNonNull(name), alias, inheritance);
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
     * Gets table inheritance behavior.
     *
     * @return inheritance behavior.
     */
    Inheritance inheritance();

    /**
     * Adds alias to the table.
     *
     * @param alias an alias to add.
     * @return A newly created table with the provide alias. All other fields are preserved.
     */
    default Table as(String alias) {
        return of(schema(), name(), alias, inheritance());
    }

    /**
     * Adds a schema to the table.
     *
     * @param schema a schema to add.
     * @return A newly created table with the provided schema. All other fields are preserved.
     */
    default Table inSchema(String schema) {
        return of(schema, name(), alias(), inheritance());
    }

    /**
     * Marks the table as {@code ONLY} (PostgreSQL).
     *
     * @return A new instance with ONLY inheritance.
     */
    default Table only() {
        return of(schema(), name(), alias(), Inheritance.ONLY);
    }

    /**
     * Marks the table as explicitly including descendants (PostgreSQL).
     *
     * @return A new instance with descendants included.
     */
    default Table includingDescendants() {
        return of(schema(), name(), alias(), Inheritance.INCLUDE_DESCENDANTS);
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
     * @param name        the name of the table. This is not qualified name.
     * @param schema      a table schema.
     * @param alias       a table alias.
     * @param inheritance table inheritance behavior.
     */
    record Impl(String schema, String name, String alias, Inheritance inheritance) implements Table {
        public Impl {
            if (inheritance == null) {
                inheritance = Inheritance.DEFAULT;
            }
        }
    }
}
