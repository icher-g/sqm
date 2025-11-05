package io.sqm.parser.ansi;

import io.sqm.core.ExprSelectItem;
import io.sqm.core.Expression;
import io.sqm.core.SelectItem;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class ExprSelectItemParser implements Parser<ExprSelectItem> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<ExprSelectItem> parse(Cursor cur, ParseContext ctx) {
        var expr = ctx.parse(Expression.class, cur);
        if (expr.isError()) {
            return error(expr);
        }
        var alias = parseAlias(cur);
        return finalize(cur, ctx, SelectItem.expr(expr.value()).as(alias));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<ExprSelectItem> targetType() {
        return ExprSelectItem.class;
    }
}
