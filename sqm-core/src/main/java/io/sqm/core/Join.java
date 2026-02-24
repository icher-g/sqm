package io.sqm.core;

import io.sqm.core.match.JoinMatch;

/**
 * A single JOIN step that attaches a right-side TableRef to the current FROM-chain.
 * For CROSS/NATURAL joins, on() may be null. For USING, use usingColumns().
 */
public sealed interface Join extends FromItem permits CrossJoin, DialectJoin, NaturalJoin, OnJoin, UsingJoin {

    /**
     * Creates an inner join with the provided table.
     *
     * @param right a table to join.
     * @return A newly created instance of INNER JOIN with the provided table.
     */
    static OnJoin inner(TableRef right) {
        return OnJoin.of(right, JoinKind.INNER, null);
    }

    /**
     * Creates a left join with the provided table.
     *
     * @param right a table to join.
     * @return A newly created instance of LEFT JOIN with the provided table.
     */
    static OnJoin left(TableRef right) {
        return OnJoin.of(right, JoinKind.LEFT, null);
    }

    /**
     * Creates a right join with the provided table.
     *
     * @param right a table to join.
     * @return A newly created instance of RIGHT JOIN with the provided table.
     */
    static OnJoin right(TableRef right) {
        return OnJoin.of(right, JoinKind.RIGHT, null);
    }

    /**
     * Creates a full join with the provided table.
     *
     * @param right a table to join.
     * @return A newly created instance of FULL JOIN with the provided table.
     */
    static OnJoin full(TableRef right) {
        return OnJoin.of(right, JoinKind.FULL, null);
    }

    /**
     * Creates a cross join with the provided table.
     *
     * @param right a table to join.
     * @return A newly created instance of CROSS JOIN with the provided table.
     */
    static CrossJoin cross(TableRef right) {
        return CrossJoin.of(right);
    }

    /**
     * Creates a natural join with the provided table.
     *
     * @param right a table to join.
     * @return A newly created instance of NATURAL JOIN with the provided table.
     */
    static NaturalJoin natural(TableRef right) {
        return NaturalJoin.of(right);
    }

    /**
     * Creates a new matcher for the current {@link Join}.
     *
     * @param <R> the result type
     * @return a new {@code JoinMatch}.
     */
    default <R> JoinMatch<R> matchJoin() {
        return JoinMatch.match(this);
    }

    /**
     * Gets the right side of the join. A table like expression.
     *
     * @return a table like expression.
     */
    TableRef right();
}
