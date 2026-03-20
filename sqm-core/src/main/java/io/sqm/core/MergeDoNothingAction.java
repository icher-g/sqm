package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

/**
 * Models a {@code MERGE ... THEN DO NOTHING} action.
 */
public non-sealed interface MergeDoNothingAction extends MergeAction {

    /**
     * Creates an immutable merge-do-nothing action.
     *
     * @return immutable merge-do-nothing action
     */
    static MergeDoNothingAction of() {
        return Impl.INSTANCE;
    }

    /**
     * Accepts a {@link NodeVisitor}.
     *
     * @param visitor visitor instance
     * @param <R> result type
     * @return visitor result
     */
    @Override
    default <R> R accept(NodeVisitor<R> visitor) {
        return visitor.visitMergeDoNothingAction(this);
    }

    /**
     * Default immutable implementation of {@link MergeDoNothingAction}.
     */
    final class Impl implements MergeDoNothingAction {
        private static final Impl INSTANCE = new Impl();

        /**
         * Creates an immutable merge-do-nothing action implementation.
         */
        public Impl() {
        }
    }
}
