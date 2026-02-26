package io.sqm.parser;

import io.sqm.core.FunctionExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.MatchResult;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * Parser for function-call arguments.
 */
public class FunctionExprArgParser implements Parser<FunctionExpr.Arg> {
    /**
     * Creates a function-argument parser.
     */
    public FunctionExprArgParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<? extends FunctionExpr.Arg> parse(Cursor cur, ParseContext ctx) {
        MatchResult<? extends FunctionExpr.Arg> matched = ctx.parseIfMatch(FunctionExpr.Arg.StarArg.class, cur);
        if (matched.match()) {
            return matched.result();
        }
        // if this is not a star try tp parse expression.
        return ctx.parse(FunctionExpr.Arg.ExprArg.class, cur);
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
