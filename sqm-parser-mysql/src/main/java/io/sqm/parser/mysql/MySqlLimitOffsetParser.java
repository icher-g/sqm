package io.sqm.parser.mysql;

import io.sqm.core.Expression;
import io.sqm.core.LimitOffset;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses MySQL LIMIT/OFFSET clauses.
 */
public class MySqlLimitOffsetParser implements Parser<LimitOffset> {

    /**
     * Creates a MySQL limit/offset parser.
     */
    public MySqlLimitOffsetParser() {
    }

    /**
     * Parses MySQL pagination forms:
     * <ul>
     *     <li>{@code LIMIT <count>}</li>
     *     <li>{@code LIMIT <count> OFFSET <offset>}</li>
     *     <li>{@code LIMIT <offset>, <count>}</li>
     * </ul>
     *
     * @param cur a cursor containing tokens.
     * @param ctx parser context.
     * @return parsing result.
     */
    @Override
    public ParseResult<LimitOffset> parse(Cursor cur, ParseContext ctx) {
        Expression limit;
        Expression offset = null;

        if (cur.consumeIf(TokenType.LIMIT)) {
            if (cur.consumeIf(TokenType.ALL)) {
                return error("LIMIT ALL is not supported by MySQL", cur.fullPos());
            }

            var firstExpr = ctx.parse(Expression.class, cur);
            if (firstExpr.isError()) {
                return error(firstExpr);
            }

            if (cur.consumeIf(TokenType.COMMA)) {
                // MySQL comma form: LIMIT <offset>, <count>
                offset = firstExpr.value();
                var countExpr = ctx.parse(Expression.class, cur);
                if (countExpr.isError()) {
                    return error(countExpr);
                }
                limit = countExpr.value();
                if (cur.match(TokenType.OFFSET) || cur.match(TokenType.FETCH)) {
                    return error("Cannot combine LIMIT <offset>, <count> with OFFSET/FETCH", cur.fullPos());
                }
                return ok(LimitOffset.of(limit, offset));
            }

            limit = firstExpr.value();

            if (cur.consumeIf(TokenType.OFFSET)) {
                var offsetExpr = ctx.parse(Expression.class, cur);
                if (offsetExpr.isError()) {
                    return error(offsetExpr);
                }
                offset = offsetExpr.value();
            }

            if (cur.match(TokenType.FETCH)) {
                return error("FETCH pagination syntax is not supported by MySQL", cur.fullPos());
            }

            return ok(LimitOffset.of(limit, offset));
        }

        if (cur.consumeIf(TokenType.OFFSET)) {
            return error("OFFSET without LIMIT is not supported by MySQL", cur.fullPos());
        }

        if (cur.match(TokenType.FETCH)) {
            return error("FETCH pagination syntax is not supported by MySQL", cur.fullPos());
        }

        return ok(LimitOffset.of(null, null, false));
    }

    /**
     * Gets the target type this parser handles.
     *
     * @return target node type.
     */
    @Override
    public Class<LimitOffset> targetType() {
        return LimitOffset.class;
    }
}
