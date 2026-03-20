package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.List;
import java.util.Objects;

/**
 * Models a {@code WHEN NOT MATCHED THEN INSERT ... VALUES (...)} MERGE action.
 */
public non-sealed interface MergeInsertAction extends MergeAction {

    /**
     * Creates an immutable merge-insert action.
     *
     * @param columns target columns, or empty when omitted
     * @param values inserted values row
     * @return immutable merge-insert action
     */
    static MergeInsertAction of(List<Identifier> columns, RowExpr values) {
        return new Impl(columns, values);
    }

    /**
     * Returns explicit target columns, or an empty list when omitted.
     *
     * @return immutable target column list
     */
    List<Identifier> columns();

    /**
     * Returns inserted value expressions.
     *
     * @return inserted values row
     */
    RowExpr values();

    /**
     * Accepts a {@link NodeVisitor}.
     *
     * @param visitor visitor instance
     * @param <R> result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitMergeInsertAction(this);
    }

    /**
     * Default immutable implementation of {@link MergeInsertAction}.
     *
     * @param columns target columns
     * @param values inserted values row
     */
    record Impl(List<Identifier> columns, RowExpr values) implements MergeInsertAction {
        /**
         * Creates an immutable merge-insert action implementation.
         */
        public Impl {
            columns = columns == null ? List.of() : List.copyOf(columns);
            Objects.requireNonNull(values, "values");
        }
    }
}
