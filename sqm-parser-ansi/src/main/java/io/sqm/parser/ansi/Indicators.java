package io.sqm.parser.ansi;

import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;

import java.util.EnumSet;
import java.util.Set;

/**
 * Token indicator sets used by ANSI parser lookahead checks.
 */
public abstract class Indicators {
    /**
     * Creates an indicator constants holder.
     */
    protected Indicators() {
    }

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
}
