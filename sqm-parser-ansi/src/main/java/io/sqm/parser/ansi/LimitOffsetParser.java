package io.sqm.parser.ansi;

import io.sqm.core.Expression;
import io.sqm.core.LimitOffset;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

public class LimitOffsetParser implements Parser<LimitOffset> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<LimitOffset> parse(Cursor cur, ParseContext ctx) {

        Expression limit = null;
        boolean limitAll = false;

        // LIMIT (optional)
        if (cur.consumeIf(TokenType.LIMIT)) {
            if (cur.consumeIf(TokenType.ALL)) {
                limitAll = true;
            }
            else {
                var lr = ctx.parse(Expression.class, cur);
                if (lr.isError()) {
                    return error(lr);
                }
                limit = lr.value();
            }
        }

        // ANSI FETCH (can appear with or without OFFSET)
        if (cur.consumeIf(TokenType.FETCH)) {
            var fr = parseOptionalFetchClause(cur, ctx, limitAll);
            if (fr.isError()) {
                return error(fr);
            }
            limit = fr.value();
        }

        // OFFSET (optional)
        Expression offset = null;
        if (cur.consumeIf(TokenType.OFFSET)) {
            var or = ctx.parse(Expression.class, cur);
            if (or.isError()) {
                return error(or);
            }
            offset = or.value();

            // Optional ROW / ROWS
            if (cur.match(TokenType.ROW) || cur.match(TokenType.ROWS)) {
                cur.advance();
            }

            // Optional ANSI FETCH after OFFSET
            if (cur.consumeIf(TokenType.FETCH)) {
                var fr = parseOptionalFetchClause(cur, ctx, limitAll);
                if (fr.isError()) {
                    return error(fr);
                }
                limit = fr.value();
            }
        }

        return ok(LimitOffset.of(limit, offset, limitAll));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<LimitOffset> targetType() {
        return LimitOffset.class;
    }

    private ParseResult<Expression> parseOptionalFetchClause(Cursor cur, ParseContext ctx, boolean limitAll) {
        if (limitAll) {
            return error("FETCH cannot be used with LIMIT ALL", cur.fullPos());
        }

        // FETCH requires FIRST or NEXT
        if (!(cur.consumeIf(TokenType.FIRST) || cur.consumeIf(TokenType.NEXT))) {
            return error("Expected FIRST or NEXT after FETCH", cur.fullPos());
        }

        var lr = ctx.parse(Expression.class, cur);
        if (lr.isError()) {
            return error(lr);
        }

        // Optional ROW / ROWS
        if (cur.match(TokenType.ROW) || cur.match(TokenType.ROWS)) {
            cur.advance();
        }

        if (!cur.consumeIf(TokenType.ONLY)) {
            return error("Expected ONLY at the end of FETCH clause", cur.fullPos());
        }
        return ok(lr.value());
    }
}
