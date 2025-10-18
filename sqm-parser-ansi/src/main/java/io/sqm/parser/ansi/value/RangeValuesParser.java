package io.sqm.parser.ansi.value;

import io.sqm.core.Values;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class RangeValuesParser implements Parser<Values.Range> {
    @Override
    public ParseResult<Values.Range> parse(Cursor cur, ParseContext ctx) {
        var min = ctx.parse(Values.Single.class, cur);
        if (min.isError()) {
            return error(min);
        }

        cur.expect("Expected AND after a first value in BETWEEN", TokenType.AND);

        var max = ctx.parse(Values.Single.class, cur);
        if (max.isError()) {
            return error(max);
        }
        return ok(new Values.Range(min.value().value(), max.value().value()));
    }

    @Override
    public Class<Values.Range> targetType() {
        return Values.Range.class;
    }
}
