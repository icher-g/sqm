package io.sqm.parser.ansi;

import io.sqm.core.BitStringLiteralExpr;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses ANSI bit string literals (e.g., {@code B'1010'}).
 */
public class BitStringLiteralExprParser implements MatchableParser<BitStringLiteralExpr> {
    /**
     * Creates a bit string literal parser.
     */
    public BitStringLiteralExprParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<BitStringLiteralExpr> parse(Cursor cur, ParseContext ctx) {
        if (!ctx.capabilities().supports(SqlFeature.BIT_STRING_LITERAL)) {
            return error("Bit string literals are not supported by this dialect", cur.fullPos());
        }
        var token = cur.expect("Expected bit string literal", TokenType.BIT_STRING);
        return ok(BitStringLiteralExpr.of(token.lexeme()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<BitStringLiteralExpr> targetType() {
        return BitStringLiteralExpr.class;
    }

    /**
     * Performs a look-ahead test to determine whether this parser is applicable
     * at the current cursor position.
     *
     * @param cur the current cursor pointing to the next token to be parsed
     * @param ctx the parsing context providing configuration, helpers and nested parsing
     * @return {@code true} if this parser should be used to parse the upcoming
     * construct, {@code false} otherwise
     */
    @Override
    public boolean match(Cursor cur, ParseContext ctx) {
        return cur.match(TokenType.BIT_STRING);
    }
}
