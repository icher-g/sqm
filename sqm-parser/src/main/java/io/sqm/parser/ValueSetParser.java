package io.sqm.parser;

import io.sqm.core.QueryExpr;
import io.sqm.core.RowExpr;
import io.sqm.core.RowListExpr;
import io.sqm.core.ValueSet;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class ValueSetParser implements Parser<ValueSet> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<ValueSet> parse(Cursor cur, ParseContext ctx) {
        if (ctx.lookups().looksLikeQueryExpr(cur)) {
            var res = ctx.parse(QueryExpr.class, cur);
            return finalize(cur, ctx, res);
        }

        if (ctx.lookups().looksLikeRowExpr(cur)) {
            var res = ctx.parse(RowExpr.class, cur);
            return finalize(cur, ctx, res);
        }

        if (ctx.lookups().looksLikeRowListExpr(cur)) {
            var res = ctx.parse(RowListExpr.class, cur);
            return finalize(cur, ctx, res);
        }

        return error("Unsupported value set token: " + cur.peek().lexeme(), cur.fullPos());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<ValueSet> targetType() {
        return ValueSet.class;
    }
}
