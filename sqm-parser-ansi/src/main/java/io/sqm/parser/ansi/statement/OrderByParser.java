package io.sqm.parser.ansi.statement;

import io.sqm.core.Order;
import io.sqm.core.OrderBy;
import io.sqm.parser.ansi.Terminators;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.ArrayList;
import java.util.List;

public class OrderByParser implements Parser<OrderBy> {

    @Override
    public ParseResult<OrderBy> parse(Cursor cur, ParseContext ctx) {
        List<Order> items = new ArrayList<>();
        if (cur.consumeIf(TokenType.ORDER)) {
            cur.expect("Expected BY after ORDER", TokenType.BY);

            var orderCur = cur.advance(cur.find(Terminators.ORDER_TERMINATORS));
            while (!orderCur.isEof()) {
                Cursor itemCur = orderCur.advance(orderCur.find(Terminators.ITEM_TERMINATORS));
                var or = ctx.parse(Order.class, itemCur);
                if (or.isError()) {
                    return error(or);
                }
                items.add(or.value());
                orderCur.consumeIf(TokenType.COMMA);
            }
        }
        return ParseResult.ok(new OrderBy(items));
    }

    @Override
    public Class<OrderBy> targetType() {
        return OrderBy.class;
    }
}
