package io.cherlabs.sqm.parser.ansi.value;

import io.cherlabs.sqm.core.Values;
import io.cherlabs.sqm.parser.core.Cursor;
import io.cherlabs.sqm.parser.core.ParserException;
import io.cherlabs.sqm.parser.spi.ParseContext;
import io.cherlabs.sqm.parser.spi.ParseResult;
import io.cherlabs.sqm.parser.spi.Parser;

public class SingleValueParser implements Parser<Values.Single> {
    @Override
    public ParseResult<Values.Single> parse(Cursor cur, ParseContext ctx) {
        var t = cur.advance();
        switch (t.type()) {
            case NUMBER -> {
                return ok(new Values.Single(parseNumber(t.lexeme())));
            }
            case STRING -> {
                return ok(new Values.Single(t.lexeme()));
            }
            case TRUE -> {
                return ok(new Values.Single(true));
            }
            case FALSE -> {
                return ok(new Values.Single(false));
            }
            case NULL -> {
                return ok(new Values.Single(null));
            }
        }
        throw new ParserException("Unsupported value type: " + t.type(), cur.fullPos());
    }

    @Override
    public Class<Values.Single> targetType() {
        return Values.Single.class;
    }
}
