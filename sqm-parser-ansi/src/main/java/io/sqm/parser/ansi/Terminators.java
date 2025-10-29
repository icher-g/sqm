package io.sqm.parser.ansi;

import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;

import java.util.EnumSet;
import java.util.Set;

public abstract class Terminators {
    /**
     * A set of tokens used to check if the {@link Cursor} contains operators used in composite query.
     */
    public static final Set<TokenType> COMPOSITE_QUERY = EnumSet.of(
        TokenType.UNION, TokenType.INTERSECT, TokenType.EXCEPT, TokenType.RPAREN
    );

    /**
     * A set of tokens used to find the end of the SELECT statement.
     */
    public static final Set<TokenType> SELECT = EnumSet.of(
        TokenType.FROM, TokenType.WHERE, TokenType.GROUP, TokenType.HAVING, TokenType.ORDER, TokenType.LIMIT, TokenType.OFFSET, TokenType.EOF, TokenType.RPAREN
    );

    /**
     * A set of tokens used to find the end of the JOIN statement.
     */
    public static final Set<TokenType> FROM_OR_JOIN = EnumSet.of(
        TokenType.JOIN, TokenType.INNER, TokenType.LEFT, TokenType.RIGHT, TokenType.FULL, TokenType.CROSS, TokenType.USING, TokenType.NATURAL, TokenType.WHERE, TokenType.GROUP, TokenType.HAVING, TokenType.ORDER, TokenType.LIMIT, TokenType.OFFSET, TokenType.EOF
    );

    /**
     * A set of tokens used to find the end of the WHERE statement.
     */
    public static final Set<TokenType> WHERE = EnumSet.of(
        TokenType.GROUP, TokenType.HAVING, TokenType.ORDER, TokenType.LIMIT, TokenType.OFFSET, TokenType.EOF
    );

    /**
     * A set of tokens used to find the end of the GROUP BY statement.
     */
    public static final Set<TokenType> GROUP_BY = EnumSet.of(
        TokenType.HAVING, TokenType.ORDER, TokenType.LIMIT, TokenType.OFFSET, TokenType.EOF
    );

    /**
     * A set of tokens used to find the end of the HAVING statement.
     */
    public static final Set<TokenType> HAVING = EnumSet.of(
        TokenType.ORDER, TokenType.LIMIT, TokenType.OFFSET, TokenType.EOF
    );

    /**
     * A set of tokens used to find the end of the ORDER BY statement.
     */
    public static final Set<TokenType> ORDER_BY = EnumSet.of(
        TokenType.LIMIT, TokenType.OFFSET, TokenType.EOF, TokenType.COMMA
    );

    /**
     * A set of tokens used to find the end of the ITEM in the list.
     */
    public static final Set<TokenType> ITEM = EnumSet.of(
        TokenType.COMMA, TokenType.EOF
    );

    /**
     * A set of tokens used to find the end of the column definition in the predicate.
     */
    public static final Set<TokenType> COLUMN_IN_PREDICATE = EnumSet.of(
        TokenType.IN, TokenType.NOT, TokenType.BETWEEN, TokenType.EQ, TokenType.NEQ1, TokenType.NEQ2, TokenType.LT, TokenType.LTE, TokenType.GT, TokenType.GTE, TokenType.LIKE, TokenType.IS, TokenType.EOF
    );
}
