package io.cherlabs.sqm.parser.spi;

import io.cherlabs.sqm.parser.core.Cursor;

/**
 * Represents a set of lookups to be used for entities recognitions.
 */
public interface Lookups {

    /**
     * Checks whether {@link Cursor} is currently on a '*' or not.
     *
     * @param cur the cursor.
     * @return True if the {@link Cursor} is currently on a '*' or False otherwise.
     */
    boolean looksLikeStar(Cursor cur);

    /**
     * Checks whether {@link Cursor} is currently on a CASE statement or not.
     *
     * @param cur the cursor.
     * @return True if the {@link Cursor} is currently on a CASE statement or False otherwise.
     */
    boolean looksLikeCase(Cursor cur);

    /**
     * Checks whether {@link Cursor} is currently on a column statement or not.
     *
     * @param cur the cursor.
     * @return True if the {@link Cursor} is currently on a column statement or False otherwise.
     */
    boolean looksLikeColumn(Cursor cur);

    /**
     * Checks whether {@link Cursor} is currently on a value or not.
     *
     * @param cur the cursor.
     * @return True if the {@link Cursor} is currently on a value or False otherwise.
     */
    boolean looksLikeValue(Cursor cur);

    /**
     * Checks whether {@link Cursor} is currently on a function call or not.
     *
     * @param cur the cursor.
     * @return True if the {@link Cursor} is currently on a function call or False otherwise.
     */
    boolean looksLikeFunction(Cursor cur);

    /**
     * Checks whether {@link Cursor} is currently on a WITH query statement or not.
     *
     * @param cur the cursor.
     * @return True if the {@link Cursor} is currently on a WITH query statement or False otherwise.
     */
    boolean looksLikeWithQuery(Cursor cur);

    /**
     * Checks whether {@link Cursor} contains one of the composite operators or not.
     *
     * @param cur the cursor.
     * @return True if the {@link Cursor} contains one of the composite operators or False otherwise.
     */
    boolean looksLikeCompositeQuery(Cursor cur);

    /**
     * Checks whether {@link Cursor} is currently on a sub query statement or not.
     *
     * @param cur the cursor.
     * @return True if the {@link Cursor} is currently on a sub query statement or False otherwise.
     */
    boolean looksLikeSubquery(Cursor cur);

    /**
     * Checks whether {@link Cursor} is currently on a composite filter or not.
     *
     * @param cur the cursor.
     * @return True if the {@link Cursor} is currently on a composite filter or False otherwise.
     */
    boolean looksLikeCompositeFilter(Cursor cur);

    /**
     * Checks whether {@link Cursor} is currently on a tuple filter or not.
     *
     * @param cur the cursor.
     * @return True if the {@link Cursor} is currently on a tuple filter or False otherwise.
     */
    boolean looksLikeTupleFilter(Cursor cur);

    /**
     * Checks whether {@link Cursor} is currently on a list of values or not.
     *
     * @param cur the cursor.
     * @return True if the {@link Cursor} is currently on a list of values or False otherwise.
     */
    boolean looksLikeListValues(Cursor cur);

    /**
     * Checks whether {@link Cursor} is currently on a list of tuple values or not.
     *
     * @param cur the cursor.
     * @return True if the {@link Cursor} is currently on a list of tuple values or False otherwise.
     */
    boolean looksLikeTupleValues(Cursor cur);

    /**
     * Checks whether {@link Cursor} is currently on a BETWEEN statement or not.
     *
     * @param cur the cursor.
     * @return True if the {@link Cursor} is currently on a BETWEEN statement or False otherwise.
     */
    boolean looksLikeRangeValues(Cursor cur);
}
