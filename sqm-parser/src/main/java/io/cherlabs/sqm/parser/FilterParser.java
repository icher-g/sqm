package io.cherlabs.sqm.parser;

import io.cherlabs.sqm.core.ColumnFilter;
import io.cherlabs.sqm.core.CompositeFilter;
import io.cherlabs.sqm.core.Filter;
import io.cherlabs.sqm.core.TupleFilter;
import io.cherlabs.sqm.parser.core.Cursor;
import io.cherlabs.sqm.parser.spi.ParseContext;
import io.cherlabs.sqm.parser.spi.ParseResult;
import io.cherlabs.sqm.parser.spi.Parser;

/**
 * A spec parser for filter specifications.
 * <p>Example:</p>
 * <pre>
 *     {@code
 *     "o.status IN ('A','B') AND d.id= 3"
 *     }
 * </pre>
 */
public class FilterParser implements Parser<Filter> {

    /**
     * Gets the {@link Filter} type.
     *
     * @return {@link Filter} type.
     */
    @Override
    public Class<Filter> targetType() {
        return Filter.class;
    }

    /**
     * Parses the filter specification.
     *
     * @param cur the {@link Cursor} class containing the tokens.
     * @return a parser result.
     */
    @Override
    public ParseResult<Filter> parse(Cursor cur, ParseContext ctx) {
        cur = cur.removeBrackets();

        if (ctx.lookups().looksLikeCompositeFilter(cur)) {
            var cfr = ctx.parse(CompositeFilter.class, cur);
            return finalize(cur, ctx, cfr);
        }

        if (ctx.lookups().looksLikeTupleFilter(cur)) {
            var tfr = ctx.parse(TupleFilter.class, cur);
            return finalize(cur, ctx, tfr);
        }

        var cfr = ctx.parse(ColumnFilter.class, cur);
        return finalize(cur, ctx, cfr);
    }
}
