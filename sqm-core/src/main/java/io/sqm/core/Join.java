package io.sqm.core;

import io.sqm.core.internal.NaturalJoinImpl;
import io.sqm.core.internal.OnJoinImpl;

import java.util.List;
import java.util.Optional;

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
        return CrossJoin.of(right);
    }

    /**
     * Creates cross join with the provided table name.
     *
     * @param table the name of the table. This is not qualified name.
     * @return A newly created instance of the table.
     */
    static CrossJoin cross(String table) {
        return CrossJoin.of(table);
    }

    /**
     * Creates cross join with the provided table schema and name.
     *
     * @param schema a table schema.
     * @param table  the name of the table. This is not qualified name.
     * @return A newly created instance of the table.
     */
    static CrossJoin cross(String schema, String table) {
        return CrossJoin.of(schema, table);
    }

    /**
     * Creates a cross join with the provided table.
     *
     * @param right        a table to join.
     * @param usingColumns a list of columns to be used for joining.
     * @return A newly created instance of CROSS JOIN with the provided table.
     */
    static UsingJoin using(TableRef right, String... usingColumns) {
        return UsingJoin.of(right, usingColumns);
    }

    /**
     * Creates a using join with the provided table.
     *
     * @param right        a table to join.
     * @param usingColumns a list of columns to be used for joining.
     * @return A newly created instance of USING JOIN with the provided table and a list of columns.
     */
    static UsingJoin using(TableRef right, List<String> usingColumns) {
        return UsingJoin.of(right, usingColumns);
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
     * Creates cross join with the provided table name.
     *
     * @param table the name of the table. This is not qualified name.
     * @return A newly created instance of the table.
     */
    static NaturalJoin natural(String table) {
        return new NaturalJoinImpl(TableRef.table(table));
    }

    /**
     * Creates cross join with the provided table schema and name.
     *
     * @param schema a table schema.
     * @param table  the name of the table. This is not qualified name.
     * @return A newly created instance of the table.
     */
    static NaturalJoin natural(String schema, String table) {
        return new NaturalJoinImpl(TableRef.table(schema, table));
    }

    /**
     * Casts this to {@link OnJoin} if possible.
     *
     * @return an {@link Optional}<{@link OnJoin}>.
     */
    default Optional<OnJoin> asOn() {
        return this instanceof OnJoin j ? Optional.of(j) : Optional.empty();
    }

    /**
     * Casts this to {@link CrossJoin} if possible.
     *
     * @return an {@link Optional}<{@link CrossJoin}>.
     */
    default Optional<CrossJoin> asCross() {
        return this instanceof CrossJoin j ? Optional.of(j) : Optional.empty();
    }

    /**
     * Casts this to {@link UsingJoin} if possible.
     *
     * @return an {@link Optional}<{@link UsingJoin}>.
     */
    default Optional<UsingJoin> asUsing() {
        return this instanceof UsingJoin j ? Optional.of(j) : Optional.empty();
    }

    /**
     * Casts this to {@link NaturalJoin} if possible.
     *
     * @return an {@link Optional}<{@link NaturalJoin}>.
     */
    default Optional<NaturalJoin> asNatural() {
        return this instanceof NaturalJoin j ? Optional.of(j) : Optional.empty();
    }

    TableRef right();
}
