package io.sqm.parser.ansi;

import io.sqm.core.OrderBy;
import io.sqm.core.OrderItem;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses {@code ORDER BY} clauses.
 */
public class OrderByParser implements Parser<OrderBy> {
    /**
     * Creates an order-by parser.
     */
    public OrderByParser() {
    }

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
        var items = parseItems(OrderItem.class, cur, ctx);
        if (items.isError()) {
            return error(items);
        }
        return ok(OrderBy.of(items.value()));
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
