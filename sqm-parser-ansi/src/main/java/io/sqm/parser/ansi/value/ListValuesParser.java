package io.sqm.parser.ansi.value;

import io.sqm.core.Values;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ListValuesParser implements Parser<Values.ListValues> {

    @Override
    public ParseResult<Values.ListValues> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected (", TokenType.LPAREN);

        final Set<TokenType> terminates = Set.of(TokenType.RPAREN, TokenType.COMMA);
        final List<Object> values = new ArrayList<>();

        do {
            var vr = ctx.parse(Values.Single.class, cur.advance(cur.find(terminates)));
            if (vr.isError()) {
                return error(vr);
            }
            values.add(vr.value().value());
        } while (cur.consumeIf(TokenType.COMMA));

        cur.expect("Expected )", TokenType.RPAREN);
        return ok(new Values.ListValues(values));
    }

    @Override
    public Class<Values.ListValues> targetType() {
        return Values.ListValues.class;
    }
}
