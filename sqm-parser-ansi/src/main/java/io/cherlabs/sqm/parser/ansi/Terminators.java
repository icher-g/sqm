package io.cherlabs.sqm.parser.ansi;

import io.cherlabs.sqm.parser.core.TokenType;

import java.util.EnumSet;
import java.util.Set;

public abstract class Terminators {

    /**
     * A set of tokens used to find the end of the SELECT statement.
     */
    public static final Set<TokenType> SELECT_TERMINATORS = EnumSet.of(
        TokenType.FROM, TokenType.WHERE, TokenType.GROUP, TokenType.HAVING, TokenType.ORDER, TokenType.LIMIT, TokenType.OFFSET, TokenType.EOF
    );

    /**
     * A set of tokens used to find the end of the JOIN statement.
     */
    public static final Set<TokenType> FROM_OR_JOIN_TERMINATORS = EnumSet.of(
        TokenType.JOIN, TokenType.INNER, TokenType.LEFT, TokenType.RIGHT, TokenType.FULL, TokenType.CROSS, TokenType.WHERE, TokenType.GROUP, TokenType.HAVING, TokenType.ORDER, TokenType.LIMIT, TokenType.OFFSET, TokenType.EOF
    );

    /**
     * A set of tokens used to find the end of the WHERE statement.
     */
    public static final Set<TokenType> WHERE_TERMINATORS = EnumSet.of(
        TokenType.GROUP, TokenType.HAVING, TokenType.ORDER, TokenType.LIMIT, TokenType.OFFSET, TokenType.EOF
    );

    /**
     * A set of tokens used to find the end of the GROUP BY statement.
     */
    public static final Set<TokenType> GROUP_TERMINATORS = EnumSet.of(
        TokenType.HAVING, TokenType.ORDER, TokenType.LIMIT, TokenType.OFFSET, TokenType.EOF
    );

    /**
     * A set of tokens used to find the end of the HAVING statement.
     */
    public static final Set<TokenType> HAVING_TERMINATORS = EnumSet.of(
        TokenType.ORDER, TokenType.LIMIT, TokenType.OFFSET, TokenType.EOF
    );

    /**
     * A set of tokens used to find the end of the ORDER BY statement.
     */
    public static final Set<TokenType> ORDER_TERMINATORS = EnumSet.of(
        TokenType.LIMIT, TokenType.OFFSET, TokenType.EOF
    );

    /**
     * A set of tokens used to find the end of the ITEM in the list.
     */
    public static final Set<TokenType> ITEM_TERMINATORS = EnumSet.of(
        TokenType.COMMA, TokenType.EOF
    );

    /**
     * A set of tokens used to find the end of the column definition in the filter.
     */
    public static final Set<TokenType> COLUMN_IN_FILTER_TERMINATORS = EnumSet.of(
        TokenType.IN, TokenType.NOT, TokenType.BETWEEN, TokenType.EQ, TokenType.NEQ1, TokenType.NEQ2, TokenType.LT, TokenType.LTE, TokenType.GT, TokenType.GTE, TokenType.LIKE, TokenType.IS, TokenType.EOF
    );
}
