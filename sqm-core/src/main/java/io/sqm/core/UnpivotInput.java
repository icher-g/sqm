package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.List;
import java.util.Objects;

/**
 * Input column group and label used by a relational {@link UnpivotTable}.
 */
public non-sealed interface UnpivotInput extends Node {
    /**
     * Creates an unpivot input.
     *
     * @param sourceColumns source columns read for this input branch
     * @param label label emitted into the unpivot name column
     * @return unpivot input
     */
    static UnpivotInput of(List<Identifier> sourceColumns, Expression label) {
        return new Impl(sourceColumns, label);
    }

    /**
     * Creates an unpivot input with one source column.
     *
     * @param sourceColumn source column read for this input branch
     * @param label label emitted into the unpivot name column
     * @return unpivot input
     */
    static UnpivotInput of(Identifier sourceColumn, Expression label) {
        return of(List.of(sourceColumn), label);
    }

    /**
     * Returns source columns read for this input branch.
     *
     * @return immutable source column list
     */
    List<Identifier> sourceColumns();

    /**
     * Returns the label emitted into the unpivot name column.
     *
     * @return label expression
     */
    Expression label();

    /**
     * Accepts a node visitor.
     *
     * @param v visitor
     * @param <R> visitor result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> v) {
        return v.visitUnpivotInput(this);
    }

    /**
     * Immutable unpivot input implementation.
     *
     * @param sourceColumns source columns read for this input branch
     * @param label label emitted into the unpivot name column
     */
    record Impl(List<Identifier> sourceColumns, Expression label) implements UnpivotInput {
        /**
         * Creates an immutable unpivot input.
         */
        public Impl {
            Objects.requireNonNull(sourceColumns, "sourceColumns");
            Objects.requireNonNull(label, "label");
            if (sourceColumns.isEmpty()) {
                throw new IllegalArgumentException("Unpivot input requires at least one source column");
            }
            sourceColumns = List.copyOf(sourceColumns);
        }
    }
}
