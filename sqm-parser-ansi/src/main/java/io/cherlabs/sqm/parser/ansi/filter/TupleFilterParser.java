package io.cherlabs.sqm.parser.ansi.filter;

import io.cherlabs.sqm.core.Column;
import io.cherlabs.sqm.core.TupleFilter;
import io.cherlabs.sqm.core.Values;
import io.cherlabs.sqm.parser.core.Cursor;
import io.cherlabs.sqm.parser.core.ParserException;
import io.cherlabs.sqm.parser.core.TokenType;
import io.cherlabs.sqm.parser.spi.ParseContext;
import io.cherlabs.sqm.parser.spi.ParseResult;
import io.cherlabs.sqm.parser.spi.Parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TupleFilterParser implements Parser<TupleFilter> {

    @Override
    public ParseResult<TupleFilter> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected (", TokenType.LPAREN);

        final Set<TokenType> terminates = Set.of(TokenType.COMMA, TokenType.RPAREN);
        final List<Column> columns = new ArrayList<>();

        do {
            var column = ctx.parse(Column.class, cur.advance(cur.find(terminates)));
            if (column.isError()) {
                return error(column);
            }
            columns.add(column.value());
        } while (cur.consumeIf(TokenType.COMMA));

        cur.expect("Expected )", TokenType.RPAREN);

        var op = parseOperator(cur);

        var values = ctx.parse(Values.class, cur);
        if (values.isError()) {
            return error(values);
        }

        return ok(new TupleFilter(columns, op, values.value()));
    }

    @Override
    public Class<TupleFilter> targetType() {
        return TupleFilter.class;
    }

    private TupleFilter.Operator parseOperator(Cursor cur) {
        var isNot = cur.consumeIf(TokenType.NOT);
        if (cur.consumeIf(TokenType.IN)) {
            return isNot ? TupleFilter.Operator.NotIn : TupleFilter.Operator.In;
        }
        throw new ParserException("Unsupported operator: " + cur.peek().lexeme(), cur.fullPos());
    }
}
