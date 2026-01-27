package io.sqm.parser.ansi;

import io.sqm.core.ArrayExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;

public class ArrayExprParser implements MatchableParser<ArrayExpr> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<? extends ArrayExpr> parse(Cursor cur, ParseContext ctx) {
        return error("Array expressions are not supported by ANSI SQL parser", -1);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends ArrayExpr> targetType() {
        return ArrayExpr.class;
    }

    /**
     * Performs a look-ahead test to determine whether this parser is applicable
     * at the current cursor position.
     * <p>
     * Implementations must <strong>not</strong> advance the cursor or modify
     * the {@link ParseContext}. Their sole responsibility is to inspect the
     * upcoming tokens and decide if this parser is responsible for them.
     *
     * @param cur the cursor pointing at the current token
     * @param ctx the parsing context providing configuration and utilities
     * @return {@code true} if this parser should be used to parse the upcoming
     * input, {@code false} otherwise
     */
    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return cur.match(TokenType.ARRAY);
    }
}
