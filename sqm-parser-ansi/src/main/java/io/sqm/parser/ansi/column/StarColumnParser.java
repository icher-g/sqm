package io.sqm.parser.ansi.column;

import io.sqm.core.StarColumn;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;

public class StarColumnParser implements Parser<StarColumn> {

    @Override
    public ParseResult<StarColumn> parse(Cursor cur, ParseContext ctx) {
        if (cur.consumeIf(TokenType.STAR)) {
            return ok(new StarColumn());
        }
        return error("Expected *", cur.fullPos());
    }

    @Override
    public Class<StarColumn> targetType() {
        return StarColumn.class;
    }
}
