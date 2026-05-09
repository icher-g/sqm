package io.sqm.parser.oracle;

import io.sqm.core.HierarchicalQueryClause;
import io.sqm.core.OrderBy;
import io.sqm.core.OrderItem;
import io.sqm.core.Predicate;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Oracle parser for hierarchical query clauses.
 */
public class HierarchicalQueryClauseParser extends io.sqm.parser.ansi.HierarchicalQueryClauseParser {
    /**
     * Creates an Oracle hierarchical-query-clause parser.
     */
    public HierarchicalQueryClauseParser() {
    }

    /**
     * Parses an Oracle hierarchical query clause.
     *
     * @param cur token cursor
     * @param ctx parse context
     * @return parsed hierarchical query clause
     */
    @Override
    public ParseResult<? extends HierarchicalQueryClause> parse(Cursor cur, ParseContext ctx) {
        if (cur.match(TokenType.ORDER) && cur.match(TokenType.SIBLINGS, 1)) {
            return error("ORDER SIBLINGS BY requires CONNECT BY", cur.fullPos());
        }

        Predicate startWith = null;
        Predicate connectBy = null;
        boolean noCycle = false;

        if (cur.match(TokenType.START)) {
            var parsedStartWith = parseStartWith(cur, ctx);
            if (parsedStartWith.isError()) {
                return error(parsedStartWith);
            }
            startWith = parsedStartWith.value();
        }

        if (cur.match(TokenType.CONNECT)) {
            cur.expect("Expected CONNECT", TokenType.CONNECT);
            cur.expect("Expected BY after CONNECT", TokenType.BY);
            noCycle = cur.consumeIf(TokenType.NOCYCLE);
            var parsedConnectBy = ctx.parse(Predicate.class, cur);
            if (parsedConnectBy.isError()) {
                return error(parsedConnectBy);
            }
            connectBy = parsedConnectBy.value();
        }

        if (cur.match(TokenType.START)) {
            if (startWith != null) {
                return error("Duplicate START WITH clause", cur.fullPos());
            }
            var parsedStartWith = parseStartWith(cur, ctx);
            if (parsedStartWith.isError()) {
                return error(parsedStartWith);
            }
            startWith = parsedStartWith.value();
        }

        if (connectBy == null) {
            return error("Expected CONNECT BY in hierarchical query clause", cur.fullPos());
        }

        OrderBy orderSiblingsBy = null;
        if (cur.match(TokenType.ORDER) && cur.match(TokenType.SIBLINGS, 1)) {
            var parsedOrder = parseOrderSiblingsBy(cur, ctx);
            if (parsedOrder.isError()) {
                return error(parsedOrder);
            }
            orderSiblingsBy = parsedOrder.value();
        }

        return ok(HierarchicalQueryClause.of(startWith, connectBy, noCycle, orderSiblingsBy));
    }

    private ParseResult<Predicate> parseStartWith(Cursor cur, ParseContext ctx) {
        cur.expect("Expected START", TokenType.START);
        cur.expect("Expected WITH after START", TokenType.WITH);
        var predicate = ctx.parse(Predicate.class, cur);
        if (predicate.isError()) {
            return error(predicate);
        }
        return ok(predicate.value());
    }

    private ParseResult<OrderBy> parseOrderSiblingsBy(Cursor cur, ParseContext ctx) {
        cur.expect("Expected ORDER", TokenType.ORDER);
        cur.expect("Expected SIBLINGS after ORDER", TokenType.SIBLINGS);
        cur.expect("Expected BY after ORDER SIBLINGS", TokenType.BY);
        List<OrderItem> items = new ArrayList<>();
        do {
            var item = ctx.parse(OrderItem.class, cur);
            if (item.isError()) {
                return error(item);
            }
            items.add(item.value());
        } while (cur.consumeIf(TokenType.COMMA));
        return ok(OrderBy.of(items));
    }
}
