package io.sqm.parser.ansi;

import io.sqm.core.Expression;
import io.sqm.core.FunctionExpr;
import io.sqm.core.LiteralExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class FuncLiteralArgParser implements Parser<FunctionExpr.Arg.Literal> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<FunctionExpr.Arg.Literal> parse(Cursor cur, ParseContext ctx) {
        var literal = ctx.parse(LiteralExpr.class, cur);
        if (literal.isError()) {
            return error(literal);
        }
        return ok(Expression.funcArg(literal.value().value()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<FunctionExpr.Arg.Literal> targetType() {
        return FunctionExpr.Arg.Literal.class;
    }
}
