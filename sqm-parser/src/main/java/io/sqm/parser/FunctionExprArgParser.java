package io.sqm.parser;

import io.sqm.core.FunctionExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class FunctionExprArgParser implements Parser<FunctionExpr.Arg> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<FunctionExpr.Arg> parse(Cursor cur, ParseContext ctx) {
        if (ctx.lookups().looksLikeStar(cur)) {
            var res = ctx.parse(FunctionExpr.Arg.StarArg.class, cur);
            return finalize(cur, ctx, res);
        }

        var res = ctx.parse(FunctionExpr.Arg.ExprArg.class, cur);
        return finalize(cur, ctx, res);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<FunctionExpr.Arg> targetType() {
        return FunctionExpr.Arg.class;
    }
}
