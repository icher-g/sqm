package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

/**
 * Version selector for reading a base table at a specific database time or version range.
 */
public sealed interface TableVersionSpec extends Node permits TableVersionSpec.Impl {
    /**
     * Creates a single-value table version selector.
     *
     * @param kind version selector kind
     * @param value timestamp, SCN, or other single selector expression
     * @return table version specification
     */
    static TableVersionSpec of(TableVersionKind kind, Expression value) {
        return new Impl(kind, value, null, null);
    }

    /**
     * Creates a range table version selector.
     *
     * @param kind range selector kind
     * @param start start expression
     * @param end end expression
     * @return table version specification
     */
    static TableVersionSpec range(TableVersionKind kind, Expression start, Expression end) {
        return new Impl(kind, null, start, end);
    }

    /**
     * Creates an all-versions selector.
     *
     * @return table version specification
     */
    static TableVersionSpec all() {
        return new Impl(TableVersionKind.ALL, null, null, null);
    }

    /**
     * Returns the version selector kind.
     *
     * @return selector kind
     */
    TableVersionKind kind();

    /**
     * Returns the single selector value.
     *
     * @return selector value or {@code null}
     */
    Expression value();

    /**
     * Returns the range start expression.
     *
     * @return start expression or {@code null}
     */
    Expression start();

    /**
     * Returns the range end expression.
     *
     * @return end expression or {@code null}
     */
    Expression end();

    /**
     * Accepts a node visitor.
     *
     * @param v visitor
     * @param <R> visitor result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitTableVersionSpec(this);
    }

    /**
     * Table version selector kind.
     */
    enum TableVersionKind {
        /**
         * Read table rows as of a timestamp expression.
         */
        AS_OF_TIMESTAMP,
        /**
         * Read table rows as of a system change number or equivalent database version.
         */
        AS_OF_SCN,
        /**
         * Read table rows from one version expression to another.
         */
        FROM_TO,
        /**
         * Read table rows between two version expressions.
         */
        BETWEEN,
        /**
         * Read table rows contained in a version interval.
         */
        CONTAINED_IN,
        /**
         * Read all available table versions.
         */
        ALL
    }

    /**
     * Immutable table version selector implementation.
     *
     * @param kind selector kind
     * @param value single selector expression
     * @param start range start expression
     * @param end range end expression
     */
    record Impl(TableVersionKind kind, Expression value, Expression start, Expression end) implements TableVersionSpec {
        /**
         * Creates an immutable table version selector.
         */
        public Impl {
            java.util.Objects.requireNonNull(kind, "kind");
            switch (kind) {
                case AS_OF_TIMESTAMP, AS_OF_SCN -> java.util.Objects.requireNonNull(value, "value");
                case FROM_TO, BETWEEN, CONTAINED_IN -> {
                    java.util.Objects.requireNonNull(start, "start");
                    java.util.Objects.requireNonNull(end, "end");
                }
                case ALL -> {
                    if (value != null || start != null || end != null) {
                        throw new IllegalArgumentException("ALL table version selector cannot have expressions");
                    }
                }
            }
        }
    }
}
