package io.sqm.parser;

import io.sqm.core.FunctionColumn;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class FunctionArgParser implements Parser<FunctionColumn.Arg> {
    /**
     * Parses a function argument.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parse context.
     * @return {@link FunctionColumn.Arg}.
     */
    @Override
    public ParseResult<FunctionColumn.Arg> parse(Cursor cur, ParseContext ctx) {
        if (ctx.lookups().looksLikeStar(cur)) { // '*'
            var ar = ctx.parse(FunctionColumn.Arg.Star.class, cur);
            return finalize(cur, ctx, ar);
        }

        // Nested function: IDENT ('.' IDENT)* '(' ...
        if (ctx.lookups().looksLikeFunction(cur)) {
            var fr = ctx.parse(FunctionColumn.Arg.Function.class, cur);
            return finalize(cur, ctx, fr);
        }

        // Check for column.
        if (ctx.lookups().looksLikeColumn(cur)) {
            var cr = ctx.parse(FunctionColumn.Arg.Column.class, cur);
            return finalize(cur, ctx, cr);
        }

        // Must be literal.
        var lr = ctx.parse(FunctionColumn.Arg.Literal.class, cur);
        return finalize(cur, ctx, lr);
    }

    /**
     * Gets {@link FunctionColumn.Arg} as a target type.
     *
     * @return {@link FunctionColumn.Arg}
     */
    @Override
    public Class<FunctionColumn.Arg> targetType() {
        return FunctionColumn.Arg.class;
    }
}
