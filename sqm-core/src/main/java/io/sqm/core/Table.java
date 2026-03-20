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
        return new Impl(
            schema,
            Objects.requireNonNull(name),
            alias,
            inheritance,
            copyHints(indexHints)
        );
    }

    /**
     * Creates a table with quote-aware identifiers, index hints, and lock hints.
     *
     * @param schema      optional schema identifier
     * @param name        table name identifier (unqualified)
     * @param alias       optional table alias identifier
     * @param inheritance inheritance behavior
     * @param indexHints  optional index hints
     * @param lockHints   optional lock hints
     * @return a newly created table instance
     */
    static Table of(
        Identifier schema,
        Identifier name,
        Identifier alias,
        Inheritance inheritance,
        List<IndexHint> indexHints,
        List<LockHint> lockHints) {
        return new Impl(
            schema,
            Objects.requireNonNull(name),
            alias,
            inheritance,
            mergeHints(indexHints, lockHints)
        );
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
     * Returns index hints attached to this table reference.
     *
     * @return immutable list of index hints.
     */
    default List<IndexHint> indexHints() {
        return filterHints(hints(), IndexHint.class);
    }

    /**
     * Returns lock hints attached to this table reference.
     *
     * @return immutable list of lock hints.
     */
    default List<LockHint> lockHints() {
        return filterHints(hints(), LockHint.class);
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
     * Replaces index hints while preserving all non-index hints.
     *
     * @param hints index hints.
     * @return a new table with replaced index hints.
     */
    default Table withIndexHints(List<IndexHint> hints) {
        var merged = new java.util.ArrayList<>(copyHints(hints));
        merged.addAll(lockHints());
        return withHints(merged);
    }

    /**
     * Replaces lock hints while preserving all non-lock hints.
     *
     * @param hints lock hints.
     * @return a new table with replaced lock hints.
     */
    default Table withLockHints(List<LockHint> hints) {
        var merged = new java.util.ArrayList<TableHint>(indexHints());
        merged.addAll(copyHints(hints));
        return withHints(merged);
    }

    /**
     * Appends an index hint.
     *
     * @param hint index hint to append.
     * @return a new table with appended index hint.
     */
    default Table addIndexHint(IndexHint hint) {
        Objects.requireNonNull(hint, "hint");
        var updated = new java.util.ArrayList<>(hints());
        updated.add(hint);
        return withHints(updated);
    }

    /**
     * Appends a lock hint.
     *
     * @param hint lock hint to append.
     * @return a new table with appended lock hint.
     */
    default Table addLockHint(LockHint hint) {
        Objects.requireNonNull(hint, "hint");
        var updated = new java.util.ArrayList<>(hints());
        updated.add(hint);
        return withHints(updated);
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
     * Adds a {@code IGNORE INDEX (...)} hint.
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
        return addLockHint(LockHint.nolock());
    }

    /**
     * Adds a {@code WITH (UPDLOCK)} hint.
     *
     * @return a new table with appended hint.
     */
    default Table withUpdLock() {
        return addLockHint(LockHint.updlock());
    }

    /**
     * Adds a {@code WITH (HOLDLOCK)} hint.
     *
     * @return a new table with appended hint.
     */
    default Table withHoldLock() {
        return addLockHint(LockHint.holdlock());
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

    private static List<TableHint> mergeHints(List<? extends TableHint> first, List<? extends TableHint> second) {
        var merged = new java.util.ArrayList<TableHint>();
        merged.addAll(copyHints(first));
        merged.addAll(copyHints(second));
        return List.copyOf(merged);
    }

    private static List<TableHint> copyHints(List<? extends TableHint> hints) {
        if (hints == null || hints.isEmpty()) {
            return List.of();
        }
        return List.copyOf(hints);
    }

    private static <T extends TableHint> List<T> filterHints(List<TableHint> hints, Class<T> type) {
        return hints.stream()
            .filter(type::isInstance)
            .map(type::cast)
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
     * Marker interface for table hints.
     */
    sealed interface TableHint permits IndexHint, LockHint {
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
    record IndexHint(IndexHintType type, IndexHintScope scope, List<Identifier> indexes) implements TableHint {
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
         *
         * @param scope hint scope.
         * @param indexes target index identifiers.
         * @return index hint.
         */
        public static IndexHint use(IndexHintScope scope, List<Identifier> indexes) {
            return new IndexHint(IndexHintType.USE, scope, indexes);
        }

        /**
         * Creates an IGNORE INDEX hint.
         *
         * @param scope hint scope.
         * @param indexes target index identifiers.
         * @return index hint.
         */
        public static IndexHint ignore(IndexHintScope scope, List<Identifier> indexes) {
            return new IndexHint(IndexHintType.IGNORE, scope, indexes);
        }

        /**
         * Creates a FORCE INDEX hint.
         *
         * @param scope hint scope.
         * @param indexes target index identifiers.
         * @return index hint.
         */
        public static IndexHint force(IndexHintScope scope, List<Identifier> indexes) {
            return new IndexHint(IndexHintType.FORCE, scope, indexes);
        }
    }

    /**
     * Lock hint kind.
     */
    enum LockHintKind {
        /**
         * {@code NOLOCK} hint.
         */
        NOLOCK,
        /**
         * {@code UPDLOCK} hint.
         */
        UPDLOCK,
        /**
         * {@code HOLDLOCK} hint.
         */
        HOLDLOCK
    }

    /**
     * Lock hint attached to a table reference.
     *
     * @param kind hint kind.
     */
    record LockHint(LockHintKind kind) implements TableHint {
        /**
         * Creates a lock hint.
         *
         * @param kind hint kind.
         */
        public LockHint {
            Objects.requireNonNull(kind, "kind");
        }

        /**
         * Creates a {@code NOLOCK} hint.
         *
         * @return lock hint.
         */
        public static LockHint nolock() {
            return new LockHint(LockHintKind.NOLOCK);
        }

        /**
         * Creates an {@code UPDLOCK} hint.
         *
         * @return lock hint.
         */
        public static LockHint updlock() {
            return new LockHint(LockHintKind.UPDLOCK);
        }

        /**
         * Creates a {@code HOLDLOCK} hint.
         *
         * @return lock hint.
         */
        public static LockHint holdlock() {
            return new LockHint(LockHintKind.HOLDLOCK);
        }
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
         * @param schema table schema identifier
         * @param name table name identifier
         * @param alias table alias identifier
         * @param inheritance table inheritance behavior
         */
        public Impl(Identifier schema, Identifier name, Identifier alias, Inheritance inheritance) {
            this(schema, name, alias, inheritance, List.of());
        }
    }
}
