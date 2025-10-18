package io.sqm.parser;

import io.sqm.core.*;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

/**
 * A spec parser for column specifications.
 * <p>Example:</p>
 * <pre>
 *     {@code
 *     "u.user_name", "o.status", "count(*) AS cnt"
 *     }
 * </pre>
 */
public class ColumnParser implements Parser<Column> {

    /**
     * Gets the {@link Column} type.
     *
     * @return {@link Column} type.
     */
    @Override
    public Class<Column> targetType() {
        return Column.class;
    }

    /**
     * Parses the column specification.
     *
     * @param cur the {@link Cursor} class containing the tokens.
     * @return a parser result.
     */
    @Override
    public ParseResult<Column> parse(Cursor cur, ParseContext ctx) {
        // The column spec might be inside the brackets ().
        cur = cur.removeBrackets();

        // Check if this is a *.
        if (ctx.lookups().looksLikeStar(cur)) {
            var sr = ctx.parse(StarColumn.class, cur);
            return finalize(cur, ctx, sr);
        }

        // Try value: SELECT 1
        if (ctx.lookups().looksLikeValue(cur)) {
            var vr = ctx.parse(ValueColumn.class, cur);
            return finalize(cur, ctx, vr);
        }

        // Try CASE
        if (ctx.lookups().looksLikeCase(cur)) {
            var cr = ctx.parse(CaseColumn.class, cur);
            return finalize(cur, ctx, cr);
        }

        // Try function column: IDENT ('.' IDENT)* '(' ...
        if (ctx.lookups().looksLikeFunction(cur)) {
            var fr = ctx.parse(FunctionColumn.class, cur);
            return finalize(cur, ctx, fr);
        }

        // Try sub query.
        if (ctx.lookups().looksLikeSubquery(cur)) {
            var sqr = ctx.parse(QueryColumn.class, cur);
            return finalize(cur, ctx, sqr);
        }

        // Simple expr like: t.c [AS a] | c [AS a]
        var nr = ctx.parse(NamedColumn.class, cur);
        return finalize(cur, ctx, nr);
    }
}