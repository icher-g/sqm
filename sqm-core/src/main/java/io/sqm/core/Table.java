package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.Objects;

/**
 * A catalog/schema-qualified named table: expr.g. schema.table
 */
public non-sealed interface Table extends TableRef {
    /**
     * Creates a table with quote-aware identifier.
     *
     * @param name table name identifier (unqualified)
     * @return a newly created table instance
     */
    static Table of(Identifier name) {
        return new Impl(null, Objects.requireNonNull(name), null, Inheritance.DEFAULT);
    }

    /**
     * Creates a table with quote-aware identifiers.
     *
     * @param schema      optional schema identifier
     * @param name        table name identifier (unqualified)
     * @param alias       optional table alias identifier
     * @param inheritance inheritance behavior
     * @return a newly created table instance
     */
    static Table of(Identifier schema, Identifier name, Identifier alias, Inheritance inheritance) {
        return new Impl(schema, Objects.requireNonNull(name), alias, inheritance);
    }

    /**
     * Gets a table schema.
     *
     * @return a table schema.
     */
    Identifier schema();  // may be null

    /**
     * Gets the table name identifier with quote metadata.
     *
     * @return table name identifier
     */
    Identifier name();

    /**
     * Gets the table alias identifier with quote metadata.
     *
     * @return alias identifier or {@code null}
     */
    Identifier alias();

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
        return of(schema(), name(), alias == null ? null : Identifier.of(alias), inheritance());
    }

    /**
     * Adds alias to the table.
     *
     * @param alias an alias identifier to add.
     * @return a newly created table with the provided alias.
     */
    default Table as(Identifier alias) {
        return of(schema(), name(), alias, inheritance());
    }

    /**
     * Adds a schema to the table.
     *
     * @param schema a schema to add.
     * @return A newly created table with the provided schema. All other fields are preserved.
     */
    default Table inSchema(String schema) {
        return of(schema == null ? null : Identifier.of(schema), name(), alias(), inheritance());
    }

    /**
     * Adds a schema to the table.
     *
     * @param schema a schema identifier to add.
     * @return a newly created table with the provided schema.
     */
    default Table inSchema(Identifier schema) {
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
     * An implementation class of the {@link Table}l
     *
     * @param schema      a table schema identifier.
     * @param name        the name of the table. This is not qualified name.
     * @param alias       a table alias identifier.
     * @param inheritance table inheritance behavior.
     */
    record Impl(Identifier schema, Identifier name, Identifier alias, Inheritance inheritance) implements Table {
        /**
         * Creates a table implementation.
         *
         * @param schema      table schema identifier
         * @param name        table name identifier
         * @param alias       table alias identifier
         * @param inheritance table inheritance behavior
         */
        public Impl {
            Objects.requireNonNull(name, "name");
            if (inheritance == null) {
                inheritance = Inheritance.DEFAULT;
            }
        }
    }
}
