package io.sqm.parser.spi;

import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.Lookahead;

/**
 * Represents a set of lookups to be used for nodes recognitions.
 */
public interface Lookups {
    /**
     * Determines whether the upcoming tokens represent any kind of SQL expression.
     *
     * @param cur the cursor positioned at the current token stream location
     * @return {@code true} if the next tokens appear to form an expression, {@code false} otherwise
     */
    default boolean looksLikeExpression(Cursor cur) {
        return looksLikeExpression(cur, Lookahead.initial());
    }

    /**
     * Determines whether the upcoming tokens represent any kind of SQL expression.
     *
     * @param cur the cursor positioned at the current token stream location
     * @param pos the current lookahead position.
     * @return {@code true} if the next tokens appear to form an expression, {@code false} otherwise
     */
    boolean looksLikeExpression(Cursor cur, Lookahead pos);

    /**
     * Checks if the upcoming tokens form the beginning of a {@code CASE} expression.
     *
     * @param cur the current token cursor
     * @return {@code true} if the next token is {@code CASE}, {@code false} otherwise
     */
    default boolean looksLikeCaseExpr(Cursor cur) {
        return looksLikeCaseExpr(cur, Lookahead.initial());
    }

    /**
     * Checks if the upcoming tokens form the beginning of a {@code CASE} expression.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if the next token is {@code CASE}, {@code false} otherwise
     */
    boolean looksLikeCaseExpr(Cursor cur, Lookahead pos);

    /**
     * Checks whether the next tokens represent a column reference, such as {@code col} or {@code t.col}.
     *
     * @param cur the current token cursor
     * @return {@code true} if a column reference appears ahead, {@code false} otherwise
     */
    default boolean looksLikeColumnRef(Cursor cur) {
        return looksLikeColumnRef(cur, Lookahead.initial());
    }

    /**
     * Checks whether the next tokens represent a column reference, such as {@code col} or {@code t.col}.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a column reference appears ahead, {@code false} otherwise
     */
    boolean looksLikeColumnRef(Cursor cur, Lookahead pos);

    /**
     * Determines if the upcoming tokens indicate a function call, such as {@code COUNT(...)} or {@code ABS(...)}.
     *
     * @param cur the current token cursor
     * @return {@code true} if a function call is likely, {@code false} otherwise
     */
    default boolean looksLikeFunctionCall(Cursor cur) {
        return looksLikeFunctionCall(cur, Lookahead.initial());
    }

    /**
     * Determines if the upcoming tokens indicate a function call, such as {@code COUNT(...)} or {@code ABS(...)}.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a function call is likely, {@code false} otherwise
     */
    boolean looksLikeFunctionCall(Cursor cur, Lookahead pos);

    /**
     * Checks if the next tokens represent a predicate (a boolean condition).
     *
     * @param cur the current token cursor
     * @return {@code true} if a predicate structure is detected, {@code false} otherwise
     */
    default boolean looksLikePredicate(Cursor cur) {
        return looksLikePredicate(cur, Lookahead.initial());
    }

    /**
     * Checks if the next tokens represent a predicate (a boolean condition).
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a predicate structure is detected, {@code false} otherwise
     */
    boolean looksLikePredicate(Cursor cur, Lookahead pos);

    /**
     * Checks if the next tokens represent a unary predicate (a boolean expression: TRUE, FALSE or a boolean column).
     *
     * @param cur the current token cursor
     * @return {@code true} if a predicate structure is detected, {@code false} otherwise
     */
    default boolean looksLikeUnaryPredicate(Cursor cur) {
        return looksLikeUnaryPredicate(cur, Lookahead.initial());
    }

    /**
     * Checks if the next tokens represent a unary predicate (a boolean expression: TRUE, FALSE or a boolean column).
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a predicate structure is detected, {@code false} otherwise
     */
    boolean looksLikeUnaryPredicate(Cursor cur, Lookahead pos);

    /**
     * Determines if the next tokens represent an {@code ANY} or {@code ALL} quantified predicate.
     *
     * @param cur the current token cursor
     * @return {@code true} if an {@code ANY} or {@code ALL} predicate appears ahead, {@code false} otherwise
     */
    default boolean looksLikeAnyAllPredicate(Cursor cur) {
        return looksLikeAnyAllPredicate(cur, Lookahead.initial());
    }

    /**
     * Determines if the next tokens represent an {@code ANY} or {@code ALL} quantified predicate.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if an {@code ANY} or {@code ALL} predicate appears ahead, {@code false} otherwise
     */
    boolean looksLikeAnyAllPredicate(Cursor cur, Lookahead pos);

    /**
     * Checks if the upcoming tokens start a {@code BETWEEN} predicate.
     *
     * @param cur the current token cursor
     * @return {@code true} if a {@code BETWEEN} clause appears next, {@code false} otherwise
     */
    default boolean looksLikeBetweenPredicate(Cursor cur) {
        return looksLikeBetweenPredicate(cur, Lookahead.initial());
    }

    /**
     * Checks if the upcoming tokens start a {@code BETWEEN} predicate.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a {@code BETWEEN} clause appears next, {@code false} otherwise
     */
    boolean looksLikeBetweenPredicate(Cursor cur, Lookahead pos);

    /**
     * Determines whether the tokens represent a comparison predicate such as {@code a = b} or {@code x > 10}.
     *
     * @param cur the current token cursor
     * @return {@code true} if a comparison predicate seems to begin here, {@code false} otherwise
     */
    default boolean looksLikeComparisonPredicate(Cursor cur) {
        return looksLikeComparisonPredicate(cur, Lookahead.initial());
    }

    /**
     * Determines whether the tokens represent a comparison predicate such as {@code a = b} or {@code x > 10}.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a comparison predicate seems to begin here, {@code false} otherwise
     */
    boolean looksLikeComparisonPredicate(Cursor cur, Lookahead pos);

    /**
     * Checks whether the next tokens indicate an {@code EXISTS} predicate.
     *
     * @param cur the current token cursor
     * @return {@code true} if an {@code EXISTS} clause is detected, {@code false} otherwise
     */
    default boolean looksLikeExistsPredicate(Cursor cur) {
        return looksLikeExistsPredicate(cur, Lookahead.initial());
    }

    /**
     * Checks whether the next tokens indicate an {@code EXISTS} predicate.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if an {@code EXISTS} clause is detected, {@code false} otherwise
     */
    boolean looksLikeExistsPredicate(Cursor cur, Lookahead pos);

    /**
     * Determines if the next tokens form an {@code IN (...)} predicate.
     *
     * @param cur the current token cursor
     * @return {@code true} if an {@code IN} predicate appears ahead, {@code false} otherwise
     */
    default boolean looksLikeInPredicate(Cursor cur) {
        return looksLikeInPredicate(cur, Lookahead.initial());
    }

    /**
     * Determines if the next tokens form an {@code IN (...)} predicate.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if an {@code IN} predicate appears ahead, {@code false} otherwise
     */
    boolean looksLikeInPredicate(Cursor cur, Lookahead pos);

    /**
     * Checks if the next tokens form an {@code IS [NOT] NULL} predicate.
     *
     * @param cur the current token cursor
     * @return {@code true} if an {@code IS NULL} or {@code IS NOT NULL} expression appears ahead, {@code false} otherwise
     */
    default boolean looksLikeIsNullPredicate(Cursor cur) {
        return looksLikeIsNullPredicate(cur, Lookahead.initial());
    }

    /**
     * Checks if the next tokens form an {@code IS [NOT] NULL} predicate.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if an {@code IS NULL} or {@code IS NOT NULL} expression appears ahead, {@code false} otherwise
     */
    boolean looksLikeIsNullPredicate(Cursor cur, Lookahead pos);

    /**
     * Determines if the next tokens form a {@code LIKE} or {@code NOT LIKE} predicate.
     *
     * @param cur the current token cursor
     * @return {@code true} if a {@code LIKE} clause appears ahead, {@code false} otherwise
     */
    default boolean looksLikeLikePredicate(Cursor cur) {
        return looksLikeLikePredicate(cur, Lookahead.initial());
    }

    /**
     * Determines if the next tokens form a {@code LIKE} or {@code NOT LIKE} predicate.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a {@code LIKE} clause appears ahead, {@code false} otherwise
     */
    boolean looksLikeLikePredicate(Cursor cur, Lookahead pos);

    /**
     * Checks if the upcoming tokens start with the {@code NOT} keyword.
     *
     * @param cur the current token cursor
     * @return {@code true} if a {@code NOT} operator appears ahead, {@code false} otherwise
     */
    default boolean looksLikeNotPredicate(Cursor cur) {
        return looksLikeNotPredicate(cur, Lookahead.initial());
    }

    /**
     * Checks if the upcoming tokens start with the {@code NOT} keyword.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a {@code NOT} operator appears ahead, {@code false} otherwise
     */
    boolean looksLikeNotPredicate(Cursor cur, Lookahead pos);

    /**
     * Determines whether the tokens form a logical {@code AND} predicate.
     *
     * @param cur the current token cursor
     * @return {@code true} if an {@code AND} clause is found, {@code false} otherwise
     */
    default boolean looksLikeAndPredicate(Cursor cur) {
        return looksLikeAndPredicate(cur, Lookahead.initial());
    }

    /**
     * Determines whether the tokens form a logical {@code AND} predicate.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if an {@code AND} clause is found, {@code false} otherwise
     */
    boolean looksLikeAndPredicate(Cursor cur, Lookahead pos);

    /**
     * Determines whether the tokens form a logical {@code OR} predicate.
     *
     * @param cur the current token cursor
     * @return {@code true} if an {@code OR} clause is found, {@code false} otherwise
     */
    default boolean looksLikeOrPredicate(Cursor cur) {
        return looksLikeOrPredicate(cur, Lookahead.initial());
    }

    /**
     * Determines whether the tokens form a logical {@code OR} predicate.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if an {@code OR} clause is found, {@code false} otherwise
     */
    boolean looksLikeOrPredicate(Cursor cur, Lookahead pos);

    /**
     * Checks whether the next tokens represent a value set — for example, a list or row constructor.
     *
     * @param cur the current token cursor
     * @return {@code true} if a value set appears ahead, {@code false} otherwise
     */
    default boolean looksLikeValueSet(Cursor cur) {
        return looksLikeValueSet(cur, Lookahead.initial());
    }

    /**
     * Checks whether the next tokens represent a value set — for example, a list or row constructor.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a value set appears ahead, {@code false} otherwise
     */
    boolean looksLikeValueSet(Cursor cur, Lookahead pos);

    /**
     * Determines if the tokens start a subquery expression, such as {@code (SELECT ...)}.
     *
     * @param cur the current token cursor
     * @return {@code true} if a subquery expression is likely, {@code false} otherwise
     */
    default boolean looksLikeQueryExpr(Cursor cur) {
        return looksLikeQueryExpr(cur, Lookahead.initial());
    }

    /**
     * Determines if the tokens start a subquery expression, such as {@code (SELECT ...)}.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a subquery expression is likely, {@code false} otherwise
     */
    boolean looksLikeQueryExpr(Cursor cur, Lookahead pos);

    /**
     * Checks whether the next tokens represent a row constructor, such as {@code (a, b)}.
     *
     * @param cur the current token cursor
     * @return {@code true} if a row expression appears ahead, {@code false} otherwise
     */
    default boolean looksLikeRowExpr(Cursor cur) {
        return looksLikeRowExpr(cur, Lookahead.initial());
    }

    /**
     * Checks whether the next tokens represent a row constructor, such as {@code (a, b)}.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a row expression appears ahead, {@code false} otherwise
     */
    boolean looksLikeRowExpr(Cursor cur, Lookahead pos);

    /**
     * Determines if the upcoming tokens form a list of row expressions, for example {@code ((1, 2), (3, 4))}.
     *
     * @param cur the current token cursor
     * @return {@code true} if a row list is detected, {@code false} otherwise
     */
    default boolean looksLikeRowListExpr(Cursor cur) {
        return looksLikeRowListExpr(cur, Lookahead.initial());
    }

    /**
     * Determines if the upcoming tokens form a list of row expressions, for example {@code ((1, 2), (3, 4))}.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a row list is detected, {@code false} otherwise
     */
    boolean looksLikeRowListExpr(Cursor cur, Lookahead pos);

    /**
     * Checks whether the next token sequence represents a literal value (string, number, boolean, etc.).
     *
     * @param cur the current token cursor
     * @return {@code true} if a literal expression appears ahead, {@code false} otherwise
     */
    default boolean looksLikeLiteralExpr(Cursor cur) {
        return looksLikeLiteralExpr(cur, Lookahead.initial());
    }

    /**
     * Checks whether the next token sequence represents a literal value (string, number, boolean, etc.).
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a literal expression appears ahead, {@code false} otherwise
     */
    boolean looksLikeLiteralExpr(Cursor cur, Lookahead pos);

    /**
     * Determines whether the tokens represent a selectable item, such as a column, expression, or {@code *}.
     *
     * @param cur the current token cursor
     * @return {@code true} if a select item is detected, {@code false} otherwise
     */
    default boolean looksLikeSelectItem(Cursor cur) {
        return looksLikeSelectItem(cur, Lookahead.initial());
    }

    /**
     * Determines whether the tokens represent a selectable item, such as a column, expression, or {@code *}.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a select item is detected, {@code false} otherwise
     */
    boolean looksLikeSelectItem(Cursor cur, Lookahead pos);

    /**
     * Checks if the next token is an asterisk ({@code *}), representing all columns.
     *
     * @param cur the current token cursor
     * @return {@code true} if a star appears ahead, {@code false} otherwise
     */
    default boolean looksLikeStar(Cursor cur) {
        return looksLikeStar(cur, Lookahead.initial());
    }

    /**
     * Checks if the next token is an asterisk ({@code *}), representing all columns.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a star appears ahead, {@code false} otherwise
     */
    boolean looksLikeStar(Cursor cur, Lookahead pos);

    /**
     * Determines whether the tokens represent a qualified star, such as {@code t.*}.
     *
     * @param cur the current token cursor
     * @return {@code true} if a qualified star appears ahead, {@code false} otherwise
     */
    default boolean looksLikeQualifiedStar(Cursor cur) {
        return looksLikeQualifiedStar(cur, Lookahead.initial());
    }

    /**
     * Determines whether the tokens represent a qualified star, such as {@code t.*}.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a qualified star appears ahead, {@code false} otherwise
     */
    boolean looksLikeQualifiedStar(Cursor cur, Lookahead pos);

    /**
     * Checks whether the upcoming tokens represent a full SQL query, possibly including {@code SELECT}, {@code WITH}, or set operations.
     *
     * @param cur the current token cursor
     * @return {@code true} if a query structure appears ahead, {@code false} otherwise
     */
    default boolean looksLikeQuery(Cursor cur) {
        return looksLikeQuery(cur, Lookahead.initial());
    }

    /**
     * Checks whether the upcoming tokens represent a full SQL query, possibly including {@code SELECT}, {@code WITH}, or set operations.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a query structure appears ahead, {@code false} otherwise
     */
    boolean looksLikeQuery(Cursor cur, Lookahead pos);

    /**
     * Checks whether the upcoming tokens represent a {@code SELECT}.
     *
     * @param cur the current token cursor
     * @return {@code true} if a query structure appears ahead, {@code false} otherwise
     */
    default boolean looksLikeSelectQuery(Cursor cur) {
        return looksLikeSelectQuery(cur, Lookahead.initial());
    }

    /**
     * Checks whether the upcoming tokens represent a {@code SELECT}.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a query structure appears ahead, {@code false} otherwise
     */
    boolean looksLikeSelectQuery(Cursor cur, Lookahead pos);

    /**
     * Checks if the next tokens represent a {@code WITH} query (Common Table Expression).
     *
     * @param cur the current token cursor
     * @return {@code true} if a {@code WITH} clause appears ahead, {@code false} otherwise
     */
    default boolean looksLikeWithQuery(Cursor cur) {
        return looksLikeWithQuery(cur, Lookahead.initial());
    }

    /**
     * Checks if the next tokens represent a {@code WITH} query (Common Table Expression).
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a {@code WITH} clause appears ahead, {@code false} otherwise
     */
    boolean looksLikeWithQuery(Cursor cur, Lookahead pos);

    /**
     * Determines whether the upcoming tokens indicate a composite query formed with {@code UNION}, {@code INTERSECT}, or {@code EXCEPT}.
     *
     * @param cur the current token cursor
     * @return {@code true} if a composite query structure is detected, {@code false} otherwise
     */
    default boolean looksLikeCompositeQuery(Cursor cur) {
        return looksLikeCompositeQuery(cur, Lookahead.initial());
    }

    /**
     * Determines whether the upcoming tokens indicate a composite query formed with {@code UNION}, {@code INTERSECT}, or {@code EXCEPT}.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a composite query structure is detected, {@code false} otherwise
     */
    boolean looksLikeCompositeQuery(Cursor cur, Lookahead pos);

    /**
     * Determines if the tokens form a table reference (base table, alias, join, or subquery).
     *
     * @param cur the current token cursor
     * @return {@code true} if a table reference appears ahead, {@code false} otherwise
     */
    default boolean looksLikeTableRef(Cursor cur) {
        return looksLikeTableRef(cur, Lookahead.initial());
    }

    /**
     * Determines if the tokens form a table reference (base table, alias, join, or subquery).
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a table reference appears ahead, {@code false} otherwise
     */
    boolean looksLikeTableRef(Cursor cur, Lookahead pos);

    /**
     * Checks whether the next tokens indicate a subquery table (a query used as a table source).
     *
     * @param cur the current token cursor
     * @return {@code true} if a subquery table appears ahead, {@code false} otherwise
     */
    default boolean looksLikeQueryTable(Cursor cur) {
        return looksLikeQueryTable(cur, Lookahead.initial());
    }

    /**
     * Checks whether the next tokens indicate a subquery table (a query used as a table source).
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a subquery table appears ahead, {@code false} otherwise
     */
    boolean looksLikeQueryTable(Cursor cur, Lookahead pos);

    /**
     * Determines if the tokens form a {@code VALUES (...)} table constructor.
     *
     * @param cur the current token cursor
     * @return {@code true} if a {@code VALUES} table appears ahead, {@code false} otherwise
     */
    default boolean looksLikeValuesTable(Cursor cur) {
        return looksLikeValuesTable(cur, Lookahead.initial());
    }

    /**
     * Determines if the tokens form a {@code VALUES (...)} table constructor.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a {@code VALUES} table appears ahead, {@code false} otherwise
     */
    boolean looksLikeValuesTable(Cursor cur, Lookahead pos);

    /**
     * Checks whether the next tokens represent a base table name or identifier.
     *
     * @param cur the current token cursor
     * @return {@code true} if a simple table reference appears ahead, {@code false} otherwise
     */
    default boolean looksLikeTable(Cursor cur) {
        return looksLikeTable(cur, Lookahead.initial());
    }

    /**
     * Checks whether the next tokens represent a base table name or identifier.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a simple table reference appears ahead, {@code false} otherwise
     */
    boolean looksLikeTable(Cursor cur, Lookahead pos);

    /**
     * Determines if the tokens form a {@code JOIN} clause of any kind.
     *
     * @param cur the current token cursor
     * @return {@code true} if a join clause appears ahead, {@code false} otherwise
     */
    default boolean looksLikeJoin(Cursor cur) {
        return looksLikeJoin(cur, Lookahead.initial());
    }

    /**
     * Determines if the tokens form a {@code JOIN} clause of any kind.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a join clause appears ahead, {@code false} otherwise
     */
    boolean looksLikeJoin(Cursor cur, Lookahead pos);

    /**
     * Checks whether the next tokens form a {@code CROSS JOIN}.
     *
     * @param cur the current token cursor
     * @return {@code true} if a {@code CROSS JOIN} appears ahead, {@code false} otherwise
     */
    default boolean looksLikeCrossJoin(Cursor cur) {
        return looksLikeCrossJoin(cur, Lookahead.initial());
    }

    /**
     * Checks whether the next tokens form a {@code CROSS JOIN}.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a {@code CROSS JOIN} appears ahead, {@code false} otherwise
     */
    boolean looksLikeCrossJoin(Cursor cur, Lookahead pos);

    /**
     * Determines whether the upcoming tokens represent a {@code NATURAL JOIN}.
     *
     * @param cur the current token cursor
     * @return {@code true} if a {@code NATURAL JOIN} appears ahead, {@code false} otherwise
     */
    default boolean looksLikeNaturalJoin(Cursor cur) {
        return looksLikeNaturalJoin(cur, Lookahead.initial());
    }

    /**
     * Determines whether the upcoming tokens represent a {@code NATURAL JOIN}.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a {@code NATURAL JOIN} appears ahead, {@code false} otherwise
     */
    boolean looksLikeNaturalJoin(Cursor cur, Lookahead pos);

    /**
     * Checks if the next tokens indicate a {@code USING} join clause.
     *
     * @param cur the current token cursor
     * @return {@code true} if a {@code USING} join appears ahead, {@code false} otherwise
     */
    default boolean looksLikeUsingJoin(Cursor cur) {
        return looksLikeUsingJoin(cur, Lookahead.initial());
    }

    /**
     * Checks if the next tokens indicate a {@code USING} join clause.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a {@code USING} join appears ahead, {@code false} otherwise
     */
    boolean looksLikeUsingJoin(Cursor cur, Lookahead pos);

    /**
     * Determines whether the tokens form an {@code ON} join predicate.
     *
     * @param cur the current token cursor
     * @return {@code true} if an {@code ON} clause appears ahead, {@code false} otherwise
     */
    default boolean looksLikeOnJoin(Cursor cur) {
        return looksLikeOnJoin(cur, Lookahead.initial());
    }

    /**
     * Determines whether the tokens form an {@code ON} join predicate.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if an {@code ON} clause appears ahead, {@code false} otherwise
     */
    boolean looksLikeOnJoin(Cursor cur, Lookahead pos);

    /**
     * Determines whether the tokens indicate a query parameter: {@code $1, :name, ?}.
     *
     * @param cur the current token cursor
     * @return {@code true} if a query parameter appears ahead, {@code false} otherwise
     */
    default boolean looksLikeParam(Cursor cur) {
        return looksLikeParam(cur, Lookahead.initial());
    }

    /**
     * Determines whether the tokens indicate a query parameter: {@code $1, :name, ?}.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a query parameter appears ahead, {@code false} otherwise
     */
    boolean looksLikeParam(Cursor cur, Lookahead pos);

    /**
     * Determines whether the tokens indicate a query parameter: {@code ?}.
     *
     * @param cur the current token cursor
     * @return {@code true} if a query parameter appears ahead, {@code false} otherwise
     */
    default boolean looksLikeAnonymousParam(Cursor cur) {
        return looksLikeAnonymousParam(cur, Lookahead.initial());
    }

    /**
     * Determines whether the tokens indicate a query parameter: {@code ?}.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a query parameter appears ahead, {@code false} otherwise
     */
    boolean looksLikeAnonymousParam(Cursor cur, Lookahead pos);

    /**
     * Determines whether the tokens indicate a query parameter: {@code :name}.
     *
     * @param cur the current token cursor
     * @return {@code true} if a query parameter appears ahead, {@code false} otherwise
     */
    default boolean looksLikeNamedParam(Cursor cur) {
        return looksLikeNamedParam(cur, Lookahead.initial());
    }

    /**
     * Determines whether the tokens indicate a query parameter: {@code :name}.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a query parameter appears ahead, {@code false} otherwise
     */
    boolean looksLikeNamedParam(Cursor cur, Lookahead pos);

    /**
     * Determines whether the tokens indicate a query parameter: {@code $1}.
     *
     * @param cur the current token cursor
     * @return {@code true} if a query parameter appears ahead, {@code false} otherwise
     */
    default boolean looksLikeOrdinalParam(Cursor cur) {
        return looksLikeOrdinalParam(cur, Lookahead.initial());
    }

    /**
     * Determines whether the tokens indicate a query parameter: {@code $1}.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if a query parameter appears ahead, {@code false} otherwise
     */
    boolean looksLikeOrdinalParam(Cursor cur, Lookahead pos);

    /**
     * Determines whether the tokens indicate an arithmetic operation: {@code +, -, *, /, %}.
     *
     * @param cur the current token cursor
     * @return {@code true} if an arithmetic operator appears ahead, {@code false} otherwise
     */
    default boolean looksLikeArithmeticOperation(Cursor cur) {
        return looksLikeArithmeticOperation(cur, Lookahead.initial());
    }

    /**
     * Determines whether the tokens indicate an arithmetic operation: {@code +, -, *, /, %}.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if an arithmetic operator appears ahead, {@code false} otherwise
     */
    boolean looksLikeArithmeticOperation(Cursor cur, Lookahead pos);

    /**
     * Determines whether the tokens indicate an arithmetic operator: {@code +}.
     *
     * @param cur the current token cursor
     * @return {@code true} if an arithmetic operator appears ahead, {@code false} otherwise
     */
    default boolean looksLikeAdd(Cursor cur) {
        return looksLikeAdd(cur, Lookahead.initial());
    }

    /**
     * Determines whether the tokens indicate an arithmetic operator: {@code +}.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if an arithmetic operator appears ahead, {@code false} otherwise
     */
    boolean looksLikeAdd(Cursor cur, Lookahead pos);

    /**
     * Determines whether the tokens indicate an arithmetic operator: {@code -}.
     *
     * @param cur the current token cursor
     * @return {@code true} if an arithmetic operator appears ahead, {@code false} otherwise
     */
    default boolean looksLikeSub(Cursor cur) {
        return looksLikeSub(cur, Lookahead.initial());
    }

    /**
     * Determines whether the tokens indicate an arithmetic operator: {@code -}.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if an arithmetic operator appears ahead, {@code false} otherwise
     */
    boolean looksLikeSub(Cursor cur, Lookahead pos);

    /**
     * Determines whether the tokens indicate an arithmetic operator: {@code *}.
     *
     * @param cur the current token cursor
     * @return {@code true} if an arithmetic operator appears ahead, {@code false} otherwise
     */
    default boolean looksLikeMul(Cursor cur) {
        return looksLikeMul(cur, Lookahead.initial());
    }

    /**
     * Determines whether the tokens indicate an arithmetic operator: {@code *}.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if an arithmetic operator appears ahead, {@code false} otherwise
     */
    boolean looksLikeMul(Cursor cur, Lookahead pos);

    /**
     * Determines whether the tokens indicate an arithmetic operator: {@code /}.
     *
     * @param cur the current token cursor
     * @return {@code true} if an arithmetic operator appears ahead, {@code false} otherwise
     */
    default boolean looksLikeDiv(Cursor cur) {
        return looksLikeDiv(cur, Lookahead.initial());
    }

    /**
     * Determines whether the tokens indicate an arithmetic operator: {@code /}.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if an arithmetic operator appears ahead, {@code false} otherwise
     */
    boolean looksLikeDiv(Cursor cur, Lookahead pos);

    /**
     * Determines whether the tokens indicate an arithmetic operator: {@code %}.
     *
     * @param cur the current token cursor
     * @return {@code true} if an arithmetic operator appears ahead, {@code false} otherwise
     */
    default boolean looksLikeMod(Cursor cur) {
        return looksLikeMod(cur, Lookahead.initial());
    }

    /**
     * Determines whether the tokens indicate an arithmetic operator: {@code %}.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if an arithmetic operator appears ahead, {@code false} otherwise
     */
    boolean looksLikeMod(Cursor cur, Lookahead pos);

    /**
     * Determines whether the tokens indicate an arithmetic operator: {@code -1}.
     *
     * @param cur the current token cursor
     * @return {@code true} if an arithmetic operator appears ahead, {@code false} otherwise
     */
    default boolean looksLikeNeg(Cursor cur) {
        return looksLikeNeg(cur, Lookahead.initial());
    }

    /**
     * Determines whether the tokens indicate an arithmetic operator: {@code -1}.
     *
     * @param cur the current token cursor
     * @param pos the current lookahead position.
     * @return {@code true} if an arithmetic operator appears ahead, {@code false} otherwise
     */
    boolean looksLikeNeg(Cursor cur, Lookahead pos);
}
