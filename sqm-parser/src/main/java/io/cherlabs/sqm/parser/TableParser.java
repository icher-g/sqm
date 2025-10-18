package io.cherlabs.sqm.parser;

import io.cherlabs.sqm.core.NamedTable;
import io.cherlabs.sqm.core.QueryTable;
import io.cherlabs.sqm.core.Table;
import io.cherlabs.sqm.parser.core.Cursor;
import io.cherlabs.sqm.parser.spi.ParseContext;
import io.cherlabs.sqm.parser.spi.ParseResult;
import io.cherlabs.sqm.parser.spi.Parser;

/**
 * Parses table specs like:
 * <pre>
 * {@code
 * products
 * sales.products p
 * sales.products AS p
 * server.db.schema.table t  (schema becomes "server.db.schema")
 * }
 * </pre>
 */
public final class TableParser implements Parser<Table> {

    /**
     * Gets the {@link Table} type.
     *
     * @return {@link Table} type.
     */
    @Override
    public Class<Table> targetType() {
        return Table.class;
    }

    /**
     * Parses the table specification.
     *
     * @param cur the {@link Cursor} class containing the tokens.
     * @return a parser result.
     */
    @Override
    public ParseResult<Table> parse(Cursor cur, ParseContext ctx) {
        if (ctx.lookups().looksLikeSubquery(cur)) {
            var sr = ctx.parse(QueryTable.class, cur);
            return finalize(cur, ctx, sr);
        }

        var tr = ctx.parse(NamedTable.class, cur);
        if (tr.isError()) {
            return error(tr);
        }
        return finalize(cur, ctx, tr);
    }
}
