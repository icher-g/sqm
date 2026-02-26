package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.core.OperatorTokens.isMinus;
import static io.sqm.parser.core.OperatorTokens.isPlus;
import static io.sqm.parser.spi.ParseResult.error;

/**
 * Parses additive arithmetic expressions with {@code +} and {@code -} operators.
 */
public class AdditiveArithmeticExprParser implements Parser<Expression> {
    /**
     * Creates an additive arithmetic expression parser.
     */
    public AdditiveArithmeticExprParser() {
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
        ParseResult<? extends Expression> lhs = ctx.parse(MultiplicativeArithmeticExpr.class, cur);
        if (lhs.isError()) return error(lhs);
        while (isPlus(cur.peek()) || isMinus(cur.peek())) {
            if (isPlus(cur.peek())) {
                lhs = ctx.parse(AddArithmeticExpr.class, lhs.value(), cur);
            }
            else {
                lhs = ctx.parse(SubArithmeticExpr.class, lhs.value(), cur);
            }
            if (lhs.isError()) return error(lhs);
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
        return AdditiveArithmeticExpr.class;
    }
}
