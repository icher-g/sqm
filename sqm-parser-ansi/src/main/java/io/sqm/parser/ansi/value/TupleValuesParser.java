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

public class TupleValuesParser implements Parser<Values.Tuples> {
    @Override
    public ParseResult<Values.Tuples> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected (", TokenType.LPAREN);

        final Set<TokenType> terminates = Set.of(TokenType.COMMA, TokenType.RPAREN);
        final List<List<?>> values = new ArrayList<>();

        do {
            var val = ctx.parse(Values.ListValues.class, cur.advance(cur.find(terminates)));
            if (val.isError()) {
                return error(val);
            }
            values.add(val.value().items());
        } while (cur.consumeIf(TokenType.COMMA));

        cur.expect("Expected )", TokenType.RPAREN);
        return ok(new Values.Tuples(values));
    }

    @Override
    public Class<Values.Tuples> targetType() {
        return Values.Tuples.class;
    }
}
