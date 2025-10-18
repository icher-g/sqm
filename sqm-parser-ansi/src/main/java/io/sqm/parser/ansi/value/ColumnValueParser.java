package io.sqm.parser.ansi.value;

import io.sqm.core.Column;
import io.sqm.core.Values;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

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
