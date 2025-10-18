package io.cherlabs.sqm.parser;

import io.cherlabs.sqm.core.Values;
import io.cherlabs.sqm.parser.core.Cursor;
import io.cherlabs.sqm.parser.spi.ParseContext;
import io.cherlabs.sqm.parser.spi.ParseResult;
import io.cherlabs.sqm.parser.spi.Parser;

/**
 * Parses values in filters.
 * <p>Example:</p>
 * <pre>
 *     {@code
 *     "o.status IN ('A','B') AND d.id= 3"
 *     }
 * </pre>
 *
 */
public class ValuesParser implements Parser<Values> {

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parsing context.
     * @return valid or invalid {@link ParseResult} depending on the parsing success.
     */
    @Override
    public ParseResult<Values> parse(Cursor cur, ParseContext ctx) {
        if (ctx.lookups().looksLikeTupleValues(cur)) {
            var vals = ctx.parse(Values.Tuples.class, cur);
            return finalize(cur, ctx, vals);
        }

        if (ctx.lookups().looksLikeListValues(cur)) {
            var vals = ctx.parse(Values.ListValues.class, cur);
            return finalize(cur, ctx, vals);
        }

        if (ctx.lookups().looksLikeRangeValues(cur)) {
            var vals = ctx.parse(Values.Range.class, cur);
            return finalize(cur, ctx, vals);
        }

        if (ctx.lookups().looksLikeSubquery(cur)) {
            var vals = ctx.parse(Values.Subquery.class, cur);
            return finalize(cur, ctx, vals);
        }

        if (ctx.lookups().looksLikeColumn(cur)) {
            var vals = ctx.parse(Values.Column.class, cur);
            return finalize(cur, ctx, vals);
        }

        var vals = ctx.parse(Values.Single.class, cur);
        return finalize(cur, ctx, vals);
    }

    /**
     * Gets the {@link Values} type.
     *
     * @return {@link Values} type.
     */
    @Override
    public Class<Values> targetType() {
        return Values.class;
    }
}
