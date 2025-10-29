package io.sqm.core;

import io.sqm.core.internal.OnJoinImpl;

/**
 * Represents a regular join: INNER/LEFT/RIGHT/FULL.
 */
public non-sealed interface OnJoin extends Join {

    /**
     * Creates a JOIN predicate.
     *
     * @param right the table reference to join.
     * @param kind  a join kind.
     * @param on    a predicate used in a JOIN statement.
     * @return a new JOIN predicate instance.
     */
    static OnJoin of(TableRef right, JoinKind kind, Predicate on) {
        return new OnJoinImpl(right, kind, on);
    }

    /**
     * Gets the join type.
     *
     * @return the join type.
     */
    JoinKind kind();

    /**
     * ON predicate;
     */
    Predicate on();

    /**
     * Adds a predicate to the current join instance.
     *
     * @param predicate a predicate to add.
     * @return A newly created instance with the provided predicate.
     */
    default Join on(Predicate predicate) {
        return new OnJoinImpl(right(), kind(), predicate);
    }
}
