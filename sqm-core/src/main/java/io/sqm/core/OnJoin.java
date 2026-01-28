package io.sqm.core;

import io.sqm.core.walk.NodeVisitor;

import java.util.List;

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
        return new Impl(right, kind, on);
    }

    /**
     * Gets the join type.
     *
     * @return the join type.
     */
    JoinKind kind();

    /**
     * ON predicate;
     *
     * @return the ON predicate.
     */
    Predicate on();

    /**
     * Adds a predicate to the current join instance.
     *
     * @param predicate a predicate to add.
     * @return A newly created instance with the provided predicate.
     */
    default OnJoin on(Predicate predicate) {
        return new Impl(right(), kind(), predicate);
    }

    /**
     * Creates USING join with the provided list of columns. {@code USING (col1, col2, ...);}
     *
     * @param usingColumns a list of columns to be used in USING statement.
     * @return A newly created instance of the USING join with the provided list of columns.
     */
    default UsingJoin using(String... usingColumns) {
        return UsingJoin.of(right(), kind(), usingColumns);
    }

    /**
     * Creates USING join with the provided list of columns. {@code USING (col1, col2, ...);}
     *
     * @param usingColumns a list of columns to be used in USING statement.
     * @return A newly created instance of the USING join with the provided list of columns.
     */
    default UsingJoin using(List<String> usingColumns) {
        return UsingJoin.of(right(), kind(), usingColumns);
    }

    /**
     * Adds a kind to the current join instance.
     *
     * @param kind a kind to add.
     * @return A newly created instance with the provided kind.
     */
    default OnJoin ofKind(JoinKind kind) {
        return new Impl(right(), kind, on());
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
        return v.visitOnJoin(this);
    }

    /**
     * Implements a regular join: INNER/LEFT/RIGHT/FULL.
     *
     * @param right the table to join.
     * @param kind  the join type: INNER/LEFT/RIGHT/FULL
     * @param on    the predicate to use on the join.
     */
    record Impl(TableRef right, JoinKind kind, Predicate on) implements OnJoin {
    }
}
