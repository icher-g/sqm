package io.sqm.parser.ansi;

import io.sqm.core.ArrayExpr;
import io.sqm.core.Expression;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.MatchableParser;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses ANSI array literal expressions.
 */
public class ArrayExprParser implements MatchableParser<ArrayExpr> {
    /**
     * Creates an array expression parser.
     */
    public ArrayExprParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<? extends ArrayExpr> parse(Cursor cur, ParseContext ctx) {
        if (!ctx.capabilities().supports(SqlFeature.ARRAY_LITERAL)) {
            return error("ARRAY literals are not supported by this dialect", cur.fullPos());
        }
        cur.expect("Expected ARRAY", TokenType.ARRAY);
        return parseArrayExpr(cur, ctx);
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

    private ParseResult<ArrayExpr> parseArrayExpr(Cursor cur, ParseContext ctx) {
        cur.expect("Expected [", TokenType.LBRACKET);

        // Multidimensional form: ARRAY[[...], [...], ...]
        if (cur.match(TokenType.LBRACKET)) {
            List<Expression> items = new ArrayList<>();
            do {
                var nested = parseArrayExpr(cur, ctx);
                if (nested.isError()) {
                    return error(nested);
                }
                items.add(nested.value());
            }
            while (cur.consumeIf(TokenType.COMMA));

            cur.expect("Expected ]", TokenType.RBRACKET);
            return ok(ArrayExpr.of(items));
        }

        // 1D form: ARRAY[expr, expr, ...]
        List<Expression> elements;
        if (!cur.match(TokenType.RBRACKET)) {
            var items = parseItems(Expression.class, cur, ctx);
            if (items.isError()) {
                return error(items);
            }
            elements = items.value();
        }
        else {
            elements = List.of();
        }

        cur.expect("Expected ]", TokenType.RBRACKET);
        return ok(ArrayExpr.of(elements));
    }
}
