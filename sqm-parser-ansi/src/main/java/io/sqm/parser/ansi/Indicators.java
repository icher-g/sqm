package io.sqm.parser.ansi;

import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;

import java.util.EnumSet;
import java.util.Set;

public abstract class Indicators {
    /**
     * A set of tokens used to check if the {@link Cursor} contains operators used in composite query.
     */
    public static final Set<TokenType> COMPOSITE_QUERY = EnumSet.of(
        TokenType.UNION, TokenType.INTERSECT, TokenType.EXCEPT
    );

    /**
     * A set of tokens used to check if the {@link Cursor} contains operators used in composite predicate.
     */
    public static final Set<TokenType> COMPOSITE_PREDICATE = EnumSet.of(
        TokenType.AND, TokenType.OR, TokenType.NOT
    );

    /**
     * A set of tokens used to check if the {@link Cursor} is on a JOIN.
     */
    public static final Set<TokenType> JOIN = EnumSet.of(
        TokenType.JOIN, TokenType.INNER, TokenType.LEFT, TokenType.RIGHT, TokenType.FULL, TokenType.CROSS, TokenType.USING, TokenType.NATURAL
    );

    public static final Set<TokenType> COMPARISON_OPERATOR = EnumSet.of(
        TokenType.EQ, TokenType.NEQ1, TokenType.NEQ2, TokenType.LT, TokenType.LTE, TokenType.GT, TokenType.GTE
    );
}
