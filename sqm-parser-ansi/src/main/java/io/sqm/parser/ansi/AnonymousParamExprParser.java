package io.sqm.parser.ansi;

import io.sqm.core.AnonymousParamExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.ok;

public class AnonymousParamExprParser implements MatchableParser<AnonymousParamExpr> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<AnonymousParamExpr> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected ?", TokenType.QMARK);
        return ok(AnonymousParamExpr.of());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<AnonymousParamExpr> targetType() {
        return AnonymousParamExpr.class;
    }

    /**
     * Performs a look-ahead test to determine whether this parser is applicable
     * at the current cursor position.
     * <p>
     * The method must <strong>not</strong> advance the cursor or modify any parsing
     * context state. Its sole responsibility is to check whether the upcoming
     * tokens syntactically correspond to the construct handled by this parser.
     *
     * @param cur the current cursor pointing to the next token to be parsed
     * @param ctx the parsing context providing configuration, helpers and nested parsing
     * @return {@code true} if this parser should be used to parse the upcoming
     * construct, {@code false} otherwise
     */
    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return cur.match(TokenType.QMARK);
    }
}
