package io.sqm.parser.ansi;

import io.sqm.core.DateLiteralExpr;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses ANSI {@code DATE '...'} typed literals.
 */
public class DateLiteralExprParser implements MatchableParser<DateLiteralExpr> {
    /**
     * Creates a date literal parser.
     */
    public DateLiteralExprParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<DateLiteralExpr> parse(Cursor cur, ParseContext ctx) {
        if (!ctx.capabilities().supports(SqlFeature.DATE_TYPED_LITERAL)) {
            return error("DATE literals are not supported by this dialect", cur.fullPos());
        }
        var keyword = cur.expect("Expected DATE literal", TokenType.IDENT);
        if (!keyword.lexeme().equalsIgnoreCase("date")) {
            return error("Expected DATE literal but found '" + keyword.lexeme() + "'", cur.fullPos());
        }
        var literal = cur.expect("Expected string literal after DATE", TokenType.STRING);
        return ok(DateLiteralExpr.of(literal.lexeme()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<DateLiteralExpr> targetType() {
        return DateLiteralExpr.class;
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
            && cur.peek().lexeme().equalsIgnoreCase("date")
            && cur.match(TokenType.STRING, 1);
    }
}
