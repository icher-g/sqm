package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.List;
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
        return new Impl(null, Objects.requireNonNull(name), null, Inheritance.DEFAULT, List.of());
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
        return new Impl(schema, Objects.requireNonNull(name), alias, inheritance, List.of());
    }

    /**
     * Creates a table with quote-aware identifiers and index hints.
     *
     * @param schema      optional schema identifier
     * @param name        table name identifier (unqualified)
     * @param alias       optional table alias identifier
     * @param inheritance inheritance behavior
     * @param indexHints  optional index hints
     * @return a newly created table instance
     */
    static Table of(Identifier schema, Identifier name, Identifier alias, Inheritance inheritance, List<IndexHint> indexHints) {
        return new Impl(schema, Objects.requireNonNull(name), alias, inheritance, indexHints == null ? List.of() : List.copyOf(indexHints));
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
     * Returns index hints attached to this table reference.
     *
     * @return immutable list of index hints.
     */
    default List<IndexHint> indexHints() {
        return List.of();
    }

    /**
     * Replaces index hints.
     *
     * @param hints index hints.
     * @return a new table with replaced index hints.
     */
    default Table withIndexHints(List<IndexHint> hints) {
        return of(schema(), name(), alias(), inheritance(), hints);
    }

    /**
     * Appends an index hint.
     *
     * @param hint index hint to append.
     * @return a new table with appended index hint.
     */
    default Table addIndexHint(IndexHint hint) {
        Objects.requireNonNull(hint, "hint");
        var hints = new java.util.ArrayList<>(indexHints());
        hints.add(hint);
        return withIndexHints(hints);
    }

    /**
     * Adds a {@code USE INDEX (...)} hint.
     *
     * @param indexes index identifiers.
     * @return a new table with appended hint.
     */
    default Table useIndex(String... indexes) {
        return addIndexHint(IndexHint.use(IndexHintScope.DEFAULT, toIdentifiers(indexes)));
    }

    /**
     * Adds an {@code IGNORE INDEX (...)} hint.
     *
     * @param indexes index identifiers.
     * @return a new table with appended hint.
     */
    default Table ignoreIndex(String... indexes) {
        return addIndexHint(IndexHint.ignore(IndexHintScope.DEFAULT, toIdentifiers(indexes)));
    }

    /**
     * Adds a {@code FORCE INDEX (...)} hint.
     *
     * @param indexes index identifiers.
     * @return a new table with appended hint.
     */
    default Table forceIndex(String... indexes) {
        return addIndexHint(IndexHint.force(IndexHintScope.DEFAULT, toIdentifiers(indexes)));
    }

    /**
     * Adds alias to the table.
     *
     * @param alias an alias to add.
     * @return A newly created table with the provide alias. All other fields are preserved.
     */
    default Table as(String alias) {
        return of(schema(), name(), alias == null ? null : Identifier.of(alias), inheritance(), indexHints());
    }

    /**
     * Adds alias to the table.
     *
     * @param alias an alias identifier to add.
     * @return a newly created table with the provided alias.
     */
    default Table as(Identifier alias) {
        return of(schema(), name(), alias, inheritance(), indexHints());
    }

    /**
     * Adds a schema to the table.
     *
     * @param schema a schema to add.
     * @return A newly created table with the provided schema. All other fields are preserved.
     */
    default Table inSchema(String schema) {
        return of(schema == null ? null : Identifier.of(schema), name(), alias(), inheritance(), indexHints());
    }

    /**
     * Adds a schema to the table.
     *
     * @param schema a schema identifier to add.
     * @return a newly created table with the provided schema.
     */
    default Table inSchema(Identifier schema) {
        return of(schema, name(), alias(), inheritance(), indexHints());
    }

    /**
     * Marks the table as {@code ONLY} (PostgreSQL).
     *
     * @return A new instance with ONLY inheritance.
     */
    default Table only() {
        return of(schema(), name(), alias(), Inheritance.ONLY, indexHints());
    }

    /**
     * Marks the table as explicitly including descendants (PostgreSQL).
     *
     * @return A new instance with descendants included.
     */
    default Table includingDescendants() {
        return of(schema(), name(), alias(), Inheritance.INCLUDE_DESCENDANTS, indexHints());
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

    private static List<Identifier> toIdentifiers(String... indexes) {
        Objects.requireNonNull(indexes, "indexes");
        return java.util.Arrays.stream(indexes)
            .map(Identifier::of)
            .toList();
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
     * MySQL-compatible index hint type.
     */
    enum IndexHintType {
        /**
         * USE INDEX hint.
         */
        USE,
        /**
         * IGNORE INDEX hint.
         */
        IGNORE,
        /**
         * FORCE INDEX hint.
         */
        FORCE
    }

    /**
     * Optional scope for index hints.
     */
    enum IndexHintScope {
        /**
         * No explicit scope.
         */
        DEFAULT,
        /**
         * Applies to JOIN access.
         */
        JOIN,
        /**
         * Applies to ORDER BY.
         */
        ORDER_BY,
        /**
         * Applies to GROUP BY.
         */
        GROUP_BY
    }

    /**
     * Index hint attached to a table reference.
     *
     * @param type    hint type.
     * @param scope   optional hint scope.
     * @param indexes target index identifiers.
     */
    record IndexHint(IndexHintType type, IndexHintScope scope, List<Identifier> indexes) {
        /**
         * Creates an index hint.
         */
        public IndexHint {
            Objects.requireNonNull(type, "type");
            if (scope == null) {
                scope = IndexHintScope.DEFAULT;
            }
            indexes = indexes == null ? List.of() : List.copyOf(indexes);
            if (indexes.isEmpty()) {
                throw new IllegalArgumentException("indexes cannot be empty");
            }
        }

        /**
         * Creates a USE INDEX hint.
         */
        public static IndexHint use(IndexHintScope scope, List<Identifier> indexes) {
            return new IndexHint(IndexHintType.USE, scope, indexes);
        }

        /**
         * Creates an IGNORE INDEX hint.
         */
        public static IndexHint ignore(IndexHintScope scope, List<Identifier> indexes) {
            return new IndexHint(IndexHintType.IGNORE, scope, indexes);
        }

        /**
         * Creates a FORCE INDEX hint.
         */
        public static IndexHint force(IndexHintScope scope, List<Identifier> indexes) {
            return new IndexHint(IndexHintType.FORCE, scope, indexes);
        }
    }

    /**
     * An implementation class of the {@link Table}l
     *
     * @param schema      a table schema identifier.
     * @param name        the name of the table. This is not qualified name.
     * @param alias       a table alias identifier.
     * @param inheritance table inheritance behavior.
     * @param indexHints  optional index hints.
     */
    record Impl(Identifier schema,
                Identifier name,
                Identifier alias,
                Inheritance inheritance,
                List<IndexHint> indexHints) implements Table {
        /**
         * Creates a table implementation.
         *
         * @param schema      table schema identifier
         * @param name        table name identifier
         * @param alias       table alias identifier
         * @param inheritance table inheritance behavior
         * @param indexHints  index hints.
         */
        public Impl {
            Objects.requireNonNull(name, "name");
            if (inheritance == null) {
                inheritance = Inheritance.DEFAULT;
            }
            indexHints = indexHints == null ? List.of() : List.copyOf(indexHints);
        }

        /**
         * Creates a table implementation without index hints.
         */
        public Impl(Identifier schema, Identifier name, Identifier alias, Inheritance inheritance) {
            this(schema, name, alias, inheritance, List.of());
        }
    }
}
