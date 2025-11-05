package io.sqm.parser.ansi;

import io.sqm.core.CaseExpr;
import io.sqm.core.Expression;
import io.sqm.core.WhenThen;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.ArrayList;

public class CaseExprParser implements Parser<CaseExpr> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<CaseExpr> parse(Cursor cur, ParseContext ctx) {
        // CASE
        cur.expect("Expected CASE but found: " + cur.peek(), TokenType.CASE);

        // One or more WHEN ... THEN ...
        var whens = new ArrayList<WhenThen>();
        while (cur.match(TokenType.WHEN)) {
            var whenThen = ctx.parse(WhenThen.class, cur);
            whens.add(whenThen.value());
        }

        if (whens.isEmpty()) {
            return error("CASE must have at least one WHEN ... THEN arm", cur.fullPos());
        }

        // Optional ELSE <result>
        Expression elseValue = null;

        if (cur.consumeIf(TokenType.ELSE)) {
            var elseResult = ctx.parse(Expression.class, cur);
            if (elseResult.isError()) {
                return error(elseResult);
            }
            elseValue = elseResult.value();
        }

        // END
        cur.expect("Expected END to close CASE", TokenType.END);

        return finalize(cur, ctx, CaseExpr.of(whens, elseValue));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<CaseExpr> targetType() {
        return CaseExpr.class;
    }
}
