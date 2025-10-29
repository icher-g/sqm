package io.sqm.parser;

import io.sqm.core.QueryTable;
import io.sqm.core.Table;
import io.sqm.core.TableRef;
import io.sqm.core.ValuesTable;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class TableRefParser implements Parser<TableRef> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<TableRef> parse(Cursor cur, ParseContext ctx) {
        if (ctx.lookups().looksLikeQueryTable(cur)) {
            var res = ctx.parse(QueryTable.class, cur);
            return finalize(cur, ctx, res);
        }

        if (ctx.lookups().looksLikeValuesTable(cur)) {
            var res = ctx.parse(ValuesTable.class, cur);
            return finalize(cur, ctx, res);
        }

        var res = ctx.parse(Table.class, cur);
        return finalize(cur, ctx, res);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<TableRef> targetType() {
        return TableRef.class;
    }
}
