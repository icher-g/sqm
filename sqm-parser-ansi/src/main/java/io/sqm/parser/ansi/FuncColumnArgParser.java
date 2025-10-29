package io.sqm.parser.ansi;

import io.sqm.core.ColumnExpr;
import io.sqm.core.Expression;
import io.sqm.core.FunctionExpr;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class FuncColumnArgParser implements Parser<FunctionExpr.Arg.Column> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<FunctionExpr.Arg.Column> parse(Cursor cur, ParseContext ctx) {
        var column = ctx.parse(ColumnExpr.class, cur);
        if (column.isError()) {
            return error(column);
        }
        return ok(Expression.funcArg(column.value()));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<FunctionExpr.Arg.Column> targetType() {
        return FunctionExpr.Arg.Column.class;
    }
}
