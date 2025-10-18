package io.sqm.parser.ansi.column;

import io.sqm.core.ValueColumn;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

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
