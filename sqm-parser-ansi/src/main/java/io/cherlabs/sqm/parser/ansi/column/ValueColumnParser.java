package io.cherlabs.sqm.parser.ansi.column;

import io.cherlabs.sqm.core.ValueColumn;
import io.cherlabs.sqm.parser.core.Cursor;
import io.cherlabs.sqm.parser.core.TokenType;
import io.cherlabs.sqm.parser.spi.ParseContext;
import io.cherlabs.sqm.parser.spi.ParseResult;
import io.cherlabs.sqm.parser.spi.Parser;

public class ValueColumnParser implements Parser<ValueColumn> {

    @Override
    public ParseResult<ValueColumn> parse(Cursor cur, ParseContext ctx) {
        var t = cur.advance();
        Object value = t.lexeme();
        if (t.type() == TokenType.NUMBER) {
            value = parseNumber(t.lexeme());
        }
        var alias = parseAlias(cur);
        return ok(new ValueColumn(value, alias));
    }

    @Override
    public Class<ValueColumn> targetType() {
        return ValueColumn.class;
    }
}
