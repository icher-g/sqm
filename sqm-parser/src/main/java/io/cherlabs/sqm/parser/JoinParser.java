package io.cherlabs.sqm.parser;

import io.cherlabs.sqm.core.Join;
import io.cherlabs.sqm.core.TableJoin;
import io.cherlabs.sqm.parser.core.Cursor;
import io.cherlabs.sqm.parser.spi.ParseContext;
import io.cherlabs.sqm.parser.spi.ParseResult;
import io.cherlabs.sqm.parser.spi.Parser;

/**
 * Parses a JOIN clause into a {@link Join}.
 * Grammar (case-insensitive):
 * [INNER|LEFT [OUTER]|RIGHT [OUTER]|FULL [OUTER]|CROSS] JOIN table [AS alias] [ON <boolean-expr>]
 * Examples:
 * JOIN products p ON p.category_id = c.id
 * LEFT OUTER JOIN warehouses AS w ON w.product_id = p.id AND w.stock > 0
 * CROSS JOIN regions r
 */
public final class JoinParser implements Parser<Join> {


    /**
     * Gets the {@link Join} type.
     *
     * @return {@link Join} type.
     */
    @Override
    public Class<Join> targetType() {
        return Join.class;
    }

    /**
     * Parses the join specification.
     *
     * @param cur the {@link Cursor} class containing the tokens.
     * @return a parser result.
     */
    @Override
    public ParseResult<Join> parse(Cursor cur, ParseContext ctx) {
        var join = ctx.parse(TableJoin.class, cur);
        if (join.isError()) {
            return error(join);
        }
        return finalize(cur, ctx, join);
    }
}
