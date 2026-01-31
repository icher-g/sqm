package io.sqm.parser.postgresql;

import io.sqm.core.IntervalLiteralExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.ParserException;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses PostgreSQL {@code INTERVAL '...'} literals with optional qualifiers.
 */
public class IntervalLiteralExprParser implements MatchableParser<IntervalLiteralExpr> {
    private static final Set<String> UNITS = Set.of("year", "month", "day", "hour", "minute", "second");

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<IntervalLiteralExpr> parse(Cursor cur, ParseContext ctx) {
        var keyword = cur.expect("Expected INTERVAL literal", TokenType.IDENT);
        if (!keyword.lexeme().equalsIgnoreCase("interval")) {
            return error("Expected INTERVAL literal but found '" + keyword.lexeme() + "'", cur.fullPos());
        }
        var literal = cur.expect("Expected string literal after INTERVAL", TokenType.STRING);
        Optional<String> qualifier = parseQualifier(cur);
        return qualifier.map(s -> ok(IntervalLiteralExpr.of(literal.lexeme(), s))).orElseGet(() -> ok(IntervalLiteralExpr.of(literal.lexeme())));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<IntervalLiteralExpr> targetType() {
        return IntervalLiteralExpr.class;
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
        return cur.match(TokenType.IDENT)
            && cur.peek().lexeme().equalsIgnoreCase("interval")
            && cur.match(TokenType.STRING, 1);
    }

    private Optional<String> parseQualifier(Cursor cur) {
        if (!cur.match(TokenType.IDENT)) {
            return Optional.empty();
        }
        var first = cur.peek().lexeme();
        if (!isUnit(first)) {
            return Optional.empty();
        }
        cur.advance();
        if (cur.consumeIf(TokenType.TO)) {
            var second = cur.expect("Expected interval unit after TO", TokenType.IDENT);
            if (!isUnit(second.lexeme())) {
                throw new ParserException("Unsupported interval unit: " + second.lexeme(), cur.fullPos());
            }
            return Optional.of(first + " TO " + second.lexeme());
        }
        return Optional.of(first);
    }

    private boolean isUnit(String value) {
        return UNITS.contains(value.toLowerCase(Locale.ROOT));
    }
}
