package io.sqm.parser.postgresql;

import io.sqm.core.DollarStringLiteralExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses PostgreSQL dollar-quoted string literals ({@code $$...$$} or {@code $tag$...$tag$}).
 */
public class DollarStringLiteralExprParser implements MatchableParser<DollarStringLiteralExpr> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<DollarStringLiteralExpr> parse(Cursor cur, ParseContext ctx) {
        var token = cur.expect("Expected dollar-quoted string literal", TokenType.DOLLAR_STRING);
        var parsed = parseToken(token.lexeme());
        if (parsed == null) {
            return error("Invalid dollar-quoted string literal", cur.fullPos());
        }
        return ok(DollarStringLiteralExpr.of(parsed.tag(), parsed.value()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<DollarStringLiteralExpr> targetType() {
        return DollarStringLiteralExpr.class;
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
        return cur.match(TokenType.DOLLAR_STRING);
    }

    private static DollarParts parseToken(String raw) {
        if (raw == null || raw.length() < 4 || raw.charAt(0) != '$') {
            return null;
        }
        int second = raw.indexOf('$', 1);
        if (second < 0) {
            return null;
        }
        String tag = raw.substring(1, second);
        String delim = "$" + tag + "$";
        if (!raw.endsWith(delim)) {
            return null;
        }
        int contentStart = second + 1;
        int contentEnd = raw.length() - delim.length();
        String value = raw.substring(contentStart, contentEnd);
        return new DollarParts(tag, value);
    }

    private record DollarParts(String tag, String value) {
    }
}
