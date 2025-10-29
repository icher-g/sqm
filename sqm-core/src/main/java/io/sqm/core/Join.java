package io.sqm.core;

import io.sqm.core.internal.CrossJoinImpl;
import io.sqm.core.internal.NaturalJoinImpl;
import io.sqm.core.internal.OnJoinImpl;
import io.sqm.core.internal.UsingJoinImpl;

import java.util.List;

/**
 * A single JOIN step that attaches a right-side TableRef to the current FROM-chain.
 * For CROSS/NATURAL joins, on() may be null. For USING, use usingColumns().
 */
public sealed interface Join extends Node permits CrossJoin, NaturalJoin, OnJoin, UsingJoin {

    /**
     * Creates an inner join with the provided table.
     *
     * @param right a table to join.
     * @return A newly created instance of INNER JOIN with the provided table.
     */
    static OnJoin join(TableRef right) {
        return new OnJoinImpl(right, JoinKind.INNER, null);
    }

    /**
     * Creates a left join with the provided table.
     *
     * @param right a table to join.
     * @return A newly created instance of LEFT JOIN with the provided table.
     */
    static OnJoin left(TableRef right) {
        return new OnJoinImpl(right, JoinKind.LEFT, null);
    }

    /**
     * Creates a right join with the provided table.
     *
     * @param right a table to join.
     * @return A newly created instance of RIGHT JOIN with the provided table.
     */
    static OnJoin right(TableRef right) {
        return new OnJoinImpl(right, JoinKind.RIGHT, null);
    }

    /**
     * Creates a full join with the provided table.
     *
     * @param right a table to join.
     * @return A newly created instance of FULL JOIN with the provided table.
     */
    static OnJoin full(TableRef right) {
        return new OnJoinImpl(right, JoinKind.FULL, null);
    }

    /**
     * Creates a cross join with the provided table.
     *
     * @param right a table to join.
     * @return A newly created instance of CROSS JOIN with the provided table.
     */
    static CrossJoin cross(TableRef right) {
        return new CrossJoinImpl(right);
    }

    /**
     * Creates a using join with the provided table.
     *
     * @param right        a table to join.
     * @param usingColumns a list of columns to be used for joining.
     * @return A newly created instance of USING JOIN with the provided table and a list of columns.
     */
    static UsingJoin using(TableRef right, List<String> usingColumns) {
        return new UsingJoinImpl(right, usingColumns);
    }

    /**
     * Creates a natural join with the provided table.
     *
     * @param right a table to join.
     * @return A newly created instance of NATURAL JOIN with the provided table.
     */
    static NaturalJoin natural(TableRef right) {
        return new NaturalJoinImpl(right);
    }

    /**
     * Casts this to {@link OnJoin}.
     *
     * @return {@link OnJoin}.
     */
    default OnJoin asOn() {
        return (OnJoin) this;
    }

    /**
     * Casts this to {@link CrossJoin}.
     *
     * @return {@link CrossJoin}.
     */
    default CrossJoin asCross() {
        return (CrossJoin) this;
    }

    /**
     * Casts this to {@link UsingJoin}.
     *
     * @return {@link UsingJoin}.
     */
    default UsingJoin asUsing() {
        return (UsingJoin) this;
    }

    /**
     * Casts this to {@link NaturalJoin}.
     *
     * @return {@link NaturalJoin}.
     */
    default NaturalJoin asNatural() {
        return (NaturalJoin) this;
    }

    TableRef right();
}
