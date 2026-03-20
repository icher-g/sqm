package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

/**
 * Models a {@code WHEN MATCHED THEN DELETE} MERGE action.
 */
public non-sealed interface MergeDeleteAction extends MergeAction {

    /**
     * Creates an immutable merge-delete action.
     *
     * @return immutable merge-delete action
     */
    static MergeDeleteAction of() {
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
        return visitor.visitMergeDeleteAction(this);
    }

    /**
     * Default immutable implementation of {@link MergeDeleteAction}.
     */
    final class Impl implements MergeDeleteAction {
        private static final Impl INSTANCE = new Impl();

        /**
         * Creates an immutable merge-delete action implementation.
         */
        public Impl() {
        }
    }
}
