package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.ArrayList;
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
     * Creates a table with quote-aware identifiers and typed table hints.
     *
     * @param schema      optional schema identifier
     * @param name        table name identifier (unqualified)
     * @param alias       optional table alias identifier
     * @param inheritance inheritance behavior
     * @param hints       optional table hints
     * @return a newly created table instance
     */
    static Table of(Identifier schema, Identifier name, Identifier alias, Inheritance inheritance, List<? extends TableHint> hints) {
        return new Impl(
            schema,
            Objects.requireNonNull(name),
            alias,
            inheritance,
            copyHints(hints)
        );
    }

    private static TableHint indexHint(String name, String... indexes) {
        return TableHint.of(
            name,
            toIdentifiers(indexes).stream()
                .map(HintArg::identifier)
                .toArray(HintArg[]::new)
        );
    }

    private static List<Identifier> toIdentifiers(String... indexes) {
        Objects.requireNonNull(indexes, "indexes");
        return java.util.Arrays.stream(indexes)
            .map(Identifier::of)
            .toList();
    }

    private static List<TableHint> copyHints(List<? extends TableHint> hints) {
        if (hints == null || hints.isEmpty()) {
            return List.of();
        }
        return List.copyOf(hints);
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
     * Returns table hints attached to this table reference.
     *
     * @return immutable list of table hints.
     */
    default List<TableHint> hints() {
        return List.of();
    }

    /**
     * Replaces all table hints.
     *
     * @param hints table hints.
     * @return a new table with replaced hints.
     */
    default Table withHints(List<? extends TableHint> hints) {
        return new Impl(schema(), name(), alias(), inheritance(), copyHints(hints));
    }

    /**
     * Appends a typed table hint.
     *
     * @param hint table hint to append.
     * @return a new table with appended hint.
     */
    default Table hint(TableHint hint) {
        Objects.requireNonNull(hint, "hint");
        var updated = new ArrayList<>(hints());
        updated.add(hint);
        return withHints(updated);
    }

    /**
     * Appends a typed table hint.
     *
     * @param name hint name
     * @param args hint arguments
     * @return a typed table hint
     */
    default Table hint(String name, Object... args) {
        return hint(TableHint.of(name, args));
    }

    /**
     * Appends a typed table hint.
     *
     * @param name hint name
     * @param args hint arguments
     * @return a typed table hint
     */
    default Table hint(Identifier name, Object... args) {
        return hint(TableHint.of(name, args));
    }

    /**
     * Adds a {@code USE INDEX (...)} hint.
     *
     * @param indexes index identifiers.
     * @return a new table with appended hint.
     */
    default Table useIndex(String... indexes) {
        return hint(indexHint("USE_INDEX", indexes));
    }

    /**
     * Adds a {@code IGNORE INDEX (...)} hint.
     *
     * @param indexes index identifiers.
     * @return a new table with appended hint.
     */
    default Table ignoreIndex(String... indexes) {
        return hint(indexHint("IGNORE_INDEX", indexes));
    }

    /**
     * Adds a {@code FORCE INDEX (...)} hint.
     *
     * @param indexes index identifiers.
     * @return a new table with appended hint.
     */
    default Table forceIndex(String... indexes) {
        return hint(indexHint("FORCE_INDEX", indexes));
    }

    /**
     * Adds alias to the table.
     *
     * @param alias an alias to add.
     * @return A newly created table with the provide alias. All other fields are preserved.
     */
    default Table as(String alias) {
        return new Impl(schema(), name(), alias == null ? null : Identifier.of(alias), inheritance(), hints());
    }

    /**
     * Adds alias to the table.
     *
     * @param alias an alias identifier to add.
     * @return a newly created table with the provided alias.
     */
    default Table as(Identifier alias) {
        return new Impl(schema(), name(), alias, inheritance(), hints());
    }

    /**
     * Adds a schema to the table.
     *
     * @param schema a schema to add.
     * @return A newly created table with the provided schema. All other fields are preserved.
     */
    default Table inSchema(String schema) {
        return new Impl(schema == null ? null : Identifier.of(schema), name(), alias(), inheritance(), hints());
    }

    /**
     * Adds a schema to the table.
     *
     * @param schema a schema identifier to add.
     * @return a newly created table with the provided schema.
     */
    default Table inSchema(Identifier schema) {
        return new Impl(schema, name(), alias(), inheritance(), hints());
    }

    /**
     * Marks the table as {@code ONLY} (PostgreSQL).
     *
     * @return A new instance with ONLY inheritance.
     */
    default Table only() {
        return new Impl(schema(), name(), alias(), Inheritance.ONLY, hints());
    }

    /**
     * Marks the table as explicitly including descendants (PostgreSQL).
     *
     * @return A new instance with descendants included.
     */
    default Table includingDescendants() {
        return new Impl(schema(), name(), alias(), Inheritance.INCLUDE_DESCENDANTS, hints());
    }

    /**
     * Adds a {@code WITH (NOLOCK)} hint.
     *
     * @return a new table with appended hint.
     */
    default Table withNoLock() {
        return hint(TableHint.of("NOLOCK"));
    }

    /**
     * Adds a {@code WITH (UPDLOCK)} hint.
     *
     * @return a new table with appended hint.
     */
    default Table withUpdLock() {
        return hint(TableHint.of("UPDLOCK"));
    }

    /**
     * Adds a {@code WITH (HOLDLOCK)} hint.
     *
     * @return a new table with appended hint.
     */
    default Table withHoldLock() {
        return hint(TableHint.of("HOLDLOCK"));
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
     * @param hints       optional table hints.
     */
    record Impl(Identifier schema,
                Identifier name,
                Identifier alias,
                Inheritance inheritance,
                List<TableHint> hints) implements Table {
        /**
         * Creates a table implementation.
         *
         * @param schema      table schema identifier
         * @param name        table name identifier
         * @param alias       table alias identifier
         * @param inheritance table inheritance behavior
         * @param hints       table hints.
         */
        public Impl {
            Objects.requireNonNull(name, "name");
            if (inheritance == null) {
                inheritance = Inheritance.DEFAULT;
            }
            hints = copyHints(hints);
        }

        /**
         * Creates a table implementation without hints.
         *
         * @param schema      table schema identifier
         * @param name        table name identifier
         * @param alias       table alias identifier
         * @param inheritance table inheritance behavior
         */
        public Impl(Identifier schema, Identifier name, Identifier alias, Inheritance inheritance) {
            this(schema, name, alias, inheritance, List.of());
        }
    }
}
