package io.sqm.parser.ansi;

import io.sqm.core.RowExpr;
import io.sqm.core.RowListExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

public class RowListExprParser implements MatchableParser<RowListExpr> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<RowListExpr> parse(Cursor cur, ParseContext ctx) {
        final List<RowExpr> rows = new ArrayList<>();
        cur.expect("Expected (", TokenType.LPAREN);

        do {
            var row = ctx.parse(RowExpr.class, cur);
            if (row.isError()) {
                return error(row);
            }
            rows.add(row.value());

        } while (cur.consumeIf(TokenType.COMMA));

        cur.expect("Expected )", TokenType.RPAREN);
        return ok(RowListExpr.of(rows));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<RowListExpr> targetType() {
        return RowListExpr.class;
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
        // match ((1, or (('a', --> ((1,2),(3,4))
        if (!cur.match(TokenType.LPAREN)) {
            return false;
        }
        if (!cur.match(TokenType.LPAREN, 1)) {
            return false;
        }
        return cur.matchAny(2, TokenType.NUMBER, TokenType.STRING);
    }
}
