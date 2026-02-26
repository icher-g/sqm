package io.sqm.parser.ansi;

import io.sqm.core.Expression;
import io.sqm.core.RowExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses row value expressions.
 */
public class RowExprParser implements MatchableParser<RowExpr> {
    /**
     * Creates a row-expression parser.
     */
    public RowExprParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<RowExpr> parse(Cursor cur, ParseContext ctx) {
        final List<Expression> list = new ArrayList<>();
        cur.expect("Expected (", TokenType.LPAREN);

        do {
            var vr = ctx.parse(Expression.class, cur);
            if (vr.isError()) {
                return error(vr);
            }
            list.add(vr.value());
        } while (cur.consumeIf(TokenType.COMMA));

        cur.expect("Expected )", TokenType.RPAREN);
        return ok(RowExpr.of(list));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<RowExpr> targetType() {
        return RowExpr.class;
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
        // match (1, or ('a', --> (1,2) OR (1)
        if (!cur.match(TokenType.LPAREN)) {
            return false;
        }
        return cur.find(Set.of(TokenType.COMMA), Set.of(TokenType.RPAREN), 1) < cur.size();
    }
}
