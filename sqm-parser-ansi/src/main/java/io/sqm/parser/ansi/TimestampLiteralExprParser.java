package io.sqm.parser.ansi;

import io.sqm.core.TimeZoneSpec;
import io.sqm.core.TimestampLiteralExpr;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses ANSI {@code TIMESTAMP ... '...'} typed literals.
 */
public class TimestampLiteralExprParser implements MatchableParser<TimestampLiteralExpr> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<TimestampLiteralExpr> parse(Cursor cur, ParseContext ctx) {
        if (!ctx.capabilities().supports(SqlFeature.TIMESTAMP_TYPED_LITERAL)) {
            return error("TIMESTAMP literals are not supported by this dialect", cur.fullPos());
        }
        var keyword = cur.expect("Expected TIMESTAMP literal", TokenType.IDENT);
        if (!keyword.lexeme().equalsIgnoreCase("timestamp")) {
            return error("Expected TIMESTAMP literal but found '" + keyword.lexeme() + "'", cur.fullPos());
        }
        TimeZoneSpec timeZoneSpec = TemporalLiteralParsingSupport.parseTimeZoneSpec(cur);
        var literal = cur.expect("Expected string literal after TIMESTAMP", TokenType.STRING);
        return ok(TimestampLiteralExpr.of(literal.lexeme(), timeZoneSpec));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<TimestampLiteralExpr> targetType() {
        return TimestampLiteralExpr.class;
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
        if (!cur.match(TokenType.IDENT) || !cur.peek().lexeme().equalsIgnoreCase("timestamp")) {
            return false;
        }
        int p = TemporalLiteralParsingSupport.skipTimeZoneSpec(cur, 1);
        if (p == -1) {
            return false;
        }
        return cur.match(TokenType.STRING, p);
    }
}
