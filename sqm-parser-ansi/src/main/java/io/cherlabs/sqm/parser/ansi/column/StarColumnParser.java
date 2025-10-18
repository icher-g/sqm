package io.cherlabs.sqm.parser.ansi.column;

import io.cherlabs.sqm.core.StarColumn;
import io.cherlabs.sqm.parser.spi.ParseContext;
import io.cherlabs.sqm.parser.spi.ParseResult;
import io.cherlabs.sqm.parser.spi.Parser;
import io.cherlabs.sqm.parser.core.Cursor;
import io.cherlabs.sqm.parser.core.TokenType;

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
