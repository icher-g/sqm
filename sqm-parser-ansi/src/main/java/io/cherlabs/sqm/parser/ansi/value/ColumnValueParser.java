package io.cherlabs.sqm.parser.ansi.value;

import io.cherlabs.sqm.core.Column;
import io.cherlabs.sqm.core.Values;
import io.cherlabs.sqm.parser.core.Cursor;
import io.cherlabs.sqm.parser.spi.ParseContext;
import io.cherlabs.sqm.parser.spi.ParseResult;
import io.cherlabs.sqm.parser.spi.Parser;

public class ColumnValueParser implements Parser<Values.Column> {
    @Override
    public ParseResult<Values.Column> parse(Cursor cur, ParseContext ctx) {
        var column = ctx.parse(Column.class, cur);
        if (column.isError()) {
            return error(column);
        }
        return ok(new Values.Column(column.value()));
    }

    @Override
    public Class<Values.Column> targetType() {
        return Values.Column.class;
    }
}
