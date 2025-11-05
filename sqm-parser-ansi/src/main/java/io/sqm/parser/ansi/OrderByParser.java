package io.sqm.parser.ansi;

import io.sqm.core.OrderBy;
import io.sqm.core.OrderItem;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.ArrayList;
import java.util.List;

public class OrderByParser implements Parser<OrderBy> {
    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<OrderBy> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected ORDER", TokenType.ORDER);
        cur.expect("Expected BY after ORDER", TokenType.BY);
        List<OrderItem> items = new ArrayList<>();
        do {
            var or = ctx.parse(OrderItem.class, cur);
            if (or.isError()) {
                return error(or);
            }
            items.add(or.value());
        }
        while (cur.consumeIf(TokenType.COMMA));
        return finalize(cur, ctx, OrderBy.of(items));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<OrderBy> targetType() {
        return OrderBy.class;
    }
}
