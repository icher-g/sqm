package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.List;
import java.util.Objects;

/**
 * Models a {@code WHEN MATCHED THEN UPDATE SET ...} MERGE action.
 */
public non-sealed interface MergeUpdateAction extends MergeAction {

    /**
     * Creates an immutable merge-update action.
     *
     * @param assignments update assignments
     * @return immutable merge-update action
     */
    static MergeUpdateAction of(List<Assignment> assignments) {
        return new Impl(assignments);
    }

    /**
     * Returns assignments applied by this action.
     *
     * @return immutable assignment list
     */
    List<Assignment> assignments();

    /**
     * Accepts a {@link NodeVisitor}.
     *
     * @param visitor visitor instance
     * @param <R> result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitMergeUpdateAction(this);
    }

    /**
     * Default immutable implementation of {@link MergeUpdateAction}.
     *
     * @param assignments update assignments
     */
    record Impl(List<Assignment> assignments) implements MergeUpdateAction {
        /**
         * Creates an immutable merge-update action implementation.
         */
        public Impl {
            assignments = List.copyOf(Objects.requireNonNull(assignments, "assignments"));
            if (assignments.isEmpty()) {
                throw new IllegalArgumentException("assignments must not be empty");
            }
        }
    }
}
