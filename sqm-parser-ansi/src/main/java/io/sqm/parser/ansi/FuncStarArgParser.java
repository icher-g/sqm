package io.sqm.parser.ansi;

import io.sqm.core.Expression;
import io.sqm.core.FunctionExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class FuncStarArgParser implements Parser<FunctionExpr.Arg.Star> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<FunctionExpr.Arg.Star> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expect '*'", TokenType.STAR);
        return ok(Expression.starArg());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<FunctionExpr.Arg.Star> targetType() {
        return FunctionExpr.Arg.Star.class;
    }
}
