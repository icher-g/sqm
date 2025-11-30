package io.sqm.parser.ansi;

import io.sqm.core.Expression;
import io.sqm.core.FunctionExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

public class FuncExprArgParser implements Parser<FunctionExpr.Arg.ExprArg> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<FunctionExpr.Arg.ExprArg> parse(Cursor cur, ParseContext ctx) {
        var arg = ctx.parse(Expression.class, cur);
        if (arg.isError()) {
            return error(arg); // <— no alias, no EOF check here
        }
        return ok(Expression.funcArg(arg.value()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<FunctionExpr.Arg.ExprArg> targetType() {
        return FunctionExpr.Arg.ExprArg.class;
    }
}
