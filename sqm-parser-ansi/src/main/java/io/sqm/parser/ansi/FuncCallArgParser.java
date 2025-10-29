package io.sqm.parser.ansi;

import io.sqm.core.Expression;
import io.sqm.core.FunctionExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class FuncCallArgParser implements Parser<FunctionExpr.Arg.Function> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<FunctionExpr.Arg.Function> parse(Cursor cur, ParseContext ctx) {
        var funcCall = ctx.parse(FunctionExpr.class, cur);
        if (funcCall.isError()) {
            return error(funcCall); // <— no alias, no EOF check here
        }
        return ok(Expression.funcArg(funcCall.value()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<FunctionExpr.Arg.Function> targetType() {
        return FunctionExpr.Arg.Function.class;
    }
}
