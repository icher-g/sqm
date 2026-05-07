package io.sqm.parser.oracle;

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
 * Parses Oracle row-limiting clauses.
 */
public class LimitOffsetParser implements Parser<LimitOffset> {
    /**
     * Creates an Oracle limit/offset parser.
     */
    public LimitOffsetParser() {
    }

    /**
     * Parses Oracle row-limiting forms:
     * <ul>
     *     <li>{@code FETCH FIRST <count> ROWS ONLY}</li>
     *     <li>{@code FETCH NEXT <count> ROWS ONLY}</li>
     *     <li>{@code OFFSET <offset> ROWS}</li>
     *     <li>{@code OFFSET <offset> ROWS FETCH NEXT <count> ROWS ONLY}</li>
     * </ul>
     *
     * @param cur token cursor.
     * @param ctx parse context.
     * @return parsed limit/offset node.
     */
    @Override
    public ParseResult<LimitOffset> parse(Cursor cur, ParseContext ctx) {
        if (cur.match(TokenType.LIMIT)) {
            return error("LIMIT pagination syntax is not supported by Oracle", cur.fullPos());
        }

        Expression offset = null;
        if (cur.consumeIf(TokenType.OFFSET)) {
            var offsetResult = ctx.parse(Expression.class, cur);
            if (offsetResult.isError()) {
                return error(offsetResult);
            }
            offset = offsetResult.value();
            cur.expect("Expected ROW or ROWS after OFFSET expression", TokenType.ROW, TokenType.ROWS);
        }

        Expression limit = null;
        if (cur.consumeIf(TokenType.FETCH)) {
            if (!(cur.consumeIf(TokenType.FIRST) || cur.consumeIf(TokenType.NEXT))) {
                return error("Expected FIRST or NEXT after FETCH", cur.fullPos());
            }

            var limitResult = ctx.parse(Expression.class, cur);
            if (limitResult.isError()) {
                return error(limitResult);
            }
            limit = limitResult.value();

            cur.expect("Expected ROW or ROWS after FETCH expression", TokenType.ROW, TokenType.ROWS);
            cur.expect("Expected ONLY at the end of FETCH clause", TokenType.ONLY);
        }

        return ok(LimitOffset.of(limit, offset));
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
