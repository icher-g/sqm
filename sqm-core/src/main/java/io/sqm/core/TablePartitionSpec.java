package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.List;
import java.util.Objects;

/**
 * Specification limiting a table reference to named physical partitions.
 */
public non-sealed interface TablePartitionSpec extends Node {
    /**
     * Creates a partition specification.
     *
     * @param names partition names
     * @return partition specification
     */
    static TablePartitionSpec partition(List<Identifier> names) {
        return new Impl(TablePartitionSpecKind.PARTITION, names);
    }

    /**
     * Creates a subpartition specification.
     *
     * @param names subpartition names
     * @return subpartition specification
     */
    static TablePartitionSpec subpartition(List<Identifier> names) {
        return new Impl(TablePartitionSpecKind.SUBPARTITION, names);
    }

    /**
     * Returns the specification kind.
     *
     * @return specification kind
     */
    TablePartitionSpecKind kind();

    /**
     * Returns specified partition or subpartition names.
     *
     * @return immutable partition names
     */
    List<Identifier> names();

    /**
     * Accepts a node visitor.
     *
     * @param v visitor
     * @param <R> visitor result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitTablePartitionSpec(this);
    }

    /**
     * Table partition specification kind.
     */
    enum TablePartitionSpecKind {
        /**
         * Selects table partitions.
         */
        PARTITION,
        /**
         * Selects table subpartitions.
         */
        SUBPARTITION
    }

    /**
     * Immutable table partition specification implementation.
     *
     * @param kind specification kind
     * @param names specified names
     */
    record Impl(TablePartitionSpecKind kind, List<Identifier> names) implements TablePartitionSpec {
        /**
         * Creates an immutable table partition specification.
         */
        public Impl {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(names, "names");
            if (names.isEmpty()) {
                throw new IllegalArgumentException("partition specification requires at least one name");
            }
            names = List.copyOf(names);
        }
    }
}
