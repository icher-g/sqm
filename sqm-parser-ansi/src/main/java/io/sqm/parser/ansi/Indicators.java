package io.sqm.parser.ansi;

import io.sqm.parser.core.TokenType;
import io.sqm.parser.core.Cursor;

import java.util.EnumSet;
import java.util.Set;

public abstract class Indicators {
    /**
     * A set of tokens used to check if the {@link Cursor} contains operators used in composite query.
     */
    public static final Set<TokenType> COMPOSITE_QUERY_INDICATORS = EnumSet.of(
        TokenType.UNION, TokenType.INTERSECT, TokenType.EXCEPT
    );

    /**
     * A set of tokens used to check if the {@link Cursor} contains operators used in composite filter.
     */
    public static final Set<TokenType> COMPOSITE_FILTER_INDICATORS = EnumSet.of(
        TokenType.AND, TokenType.OR, TokenType.NOT
    );
}
