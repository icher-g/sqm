package io.sqm.parser.sqlserver;

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
 * Parses SQL Server OFFSET/FETCH pagination clauses.
 */
public class LimitOffsetParser implements Parser<LimitOffset> {

    /**
     * Creates a SQL Server limit/offset parser.
     */
    public LimitOffsetParser() {
    }

    /**
     * Parses SQL Server pagination forms:
     * <ul>
     *     <li>{@code OFFSET <offset> ROWS}</li>
     *     <li>{@code OFFSET <offset> ROWS FETCH NEXT <count> ROWS ONLY}</li>
     *     <li>{@code OFFSET <offset> ROWS FETCH FIRST <count> ROWS ONLY}</li>
     * </ul>
     *
     * @param cur a cursor containing tokens.
     * @param ctx parser context.
     * @return parsing result.
     */
    @Override
    public ParseResult<LimitOffset> parse(Cursor cur, ParseContext ctx) {
        if (cur.match(TokenType.LIMIT)) {
            return error("LIMIT pagination syntax is not supported by SQL Server", cur.fullPos());
        }

        if (cur.match(TokenType.FETCH)) {
            return error("FETCH requires OFFSET in SQL Server", cur.fullPos());
        }

        if (!cur.consumeIf(TokenType.OFFSET)) {
            return ok(LimitOffset.of(null, null, false));
        }

        var offsetExpr = ctx.parse(Expression.class, cur);
        if (offsetExpr.isError()) {
            return error(offsetExpr);
        }

        cur.expect("Expected ROW or ROWS after OFFSET expression", TokenType.ROW, TokenType.ROWS);

        Expression limit = null;
        if (cur.consumeIf(TokenType.FETCH)) {
            if (!(cur.consumeIf(TokenType.FIRST) || cur.consumeIf(TokenType.NEXT))) {
                return error("Expected FIRST or NEXT after FETCH", cur.fullPos());
            }

            var limitExpr = ctx.parse(Expression.class, cur);
            if (limitExpr.isError()) {
                return error(limitExpr);
            }
            limit = limitExpr.value();

            cur.expect("Expected ROW or ROWS after FETCH expression", TokenType.ROW, TokenType.ROWS);

            cur.expect("Expected ONLY at the end of FETCH clause", TokenType.ONLY);
        }

        return ok(LimitOffset.of(limit, offsetExpr.value()));
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
