package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.parser.AtomicExprParser;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.core.OperatorTokens.*;
import static io.sqm.parser.spi.ParseResult.error;

public class MultiplicativeArithmeticExprParser implements Parser<Expression> {

    private final AtomicExprParser atomicExprParser;

    public MultiplicativeArithmeticExprParser(AtomicExprParser atomicExprParser) {
        this.atomicExprParser = atomicExprParser;
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<? extends Expression> parse(Cursor cur, ParseContext ctx) {
        var lhs = atomicExprParser.parse(cur, ctx);
        if (lhs.isError()) {
            return error(lhs);
        }
        while (isStar(cur.peek()) || isSlash(cur.peek()) || isPercent(cur.peek())) {
            if (isStar(cur.peek())) {
                lhs = ctx.parse(MulArithmeticExpr.class, lhs.value(), cur);
            }
            else
                if (isSlash(cur.peek())) {
                    lhs = ctx.parse(DivArithmeticExpr.class, lhs.value(), cur);
                }
                else {
                    lhs = ctx.parse(ModArithmeticExpr.class, lhs.value(), cur);
                }
            if (lhs.isError()) {
                return error(lhs);
            }
        }
        return lhs;
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<? extends Expression> targetType() {
        return MultiplicativeArithmeticExpr.class;
    }
}
