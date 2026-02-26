package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses literal expressions.
 */
public class LiteralExprParser implements MatchableParser<LiteralExpr> {
    /**
     * Creates a literal expression parser.
     */
    public LiteralExprParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<LiteralExpr> parse(Cursor cur, ParseContext ctx) {
        if (cur.match(TokenType.ESCAPE_STRING)) {
            var result = ctx.parse(EscapeStringLiteralExpr.class, cur);
            return result.isError() ? error(result) : ok(result.value());
        }
        if (cur.match(TokenType.DOLLAR_STRING)) {
            var result = ctx.parse(DollarStringLiteralExpr.class, cur);
            return result.isError() ? error(result) : ok(result.value());
        }
        if (cur.match(TokenType.BIT_STRING)) {
            var result = ctx.parse(BitStringLiteralExpr.class, cur);
            return result.isError() ? error(result) : ok(result.value());
        }
        if (cur.match(TokenType.HEX_STRING)) {
            var result = ctx.parse(HexStringLiteralExpr.class, cur);
            return result.isError() ? error(result) : ok(result.value());
        }
        if (cur.match(TokenType.IDENT)) {
            var keyword = cur.peek().lexeme();
            if (keyword.equalsIgnoreCase("date")) {
                var result = ctx.parse(DateLiteralExpr.class, cur);
                return result.isError() ? error(result) : ok(result.value());
            }
            if (keyword.equalsIgnoreCase("time")) {
                var result = ctx.parse(TimeLiteralExpr.class, cur);
                return result.isError() ? error(result) : ok(result.value());
            }
            if (keyword.equalsIgnoreCase("timestamp")) {
                var result = ctx.parse(TimestampLiteralExpr.class, cur);
                return result.isError() ? error(result) : ok(result.value());
            }
            if (keyword.equalsIgnoreCase("interval")) {
                var result = ctx.parse(IntervalLiteralExpr.class, cur);
                return result.isError() ? error(result) : ok(result.value());
            }
        }
        if (cur.match(TokenType.STRING)) {
            return ok(LiteralExpr.of(cur.advance().lexeme()));
        }
        if (cur.match(TokenType.NUMBER)) {
            return ok(LiteralExpr.of(parseNumber(cur.advance().lexeme())));
        }
        if (cur.match(TokenType.NULL)) {
            cur.advance(); // skip the literal itself
            return ok(LiteralExpr.of(null));
        }
        if (cur.match(TokenType.TRUE)) {
            cur.advance(); // skip the literal itself
            return ok(LiteralExpr.of(Boolean.TRUE));
        }
        if (cur.match(TokenType.FALSE)) {
            cur.advance(); // skip the literal itself
            return ok(LiteralExpr.of(Boolean.FALSE));
        }
        return error("Unsupported literal token: " + cur.peek().lexeme(), cur.fullPos());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<LiteralExpr> targetType() {
        return LiteralExpr.class;
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
        return ctx.lookups().looksLikeLiteralExpr(cur);
    }
}
