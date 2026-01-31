package io.sqm.parser.postgresql;

import io.sqm.core.Direction;
import io.sqm.core.Expression;
import io.sqm.core.Nulls;
import io.sqm.core.OrderItem;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.Set;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * PostgreSQL-specific ORDER BY item parser supporting {@code USING <operator>}.
 */
public class OrderItemParser implements Parser<OrderItem> {

    private static String unquoteIfQuoted(String s) {
        int n = s.length();
        if (n >= 2 && s.charAt(0) == '"' && s.charAt(n - 1) == '"') {
            // ANSI: doubled quotes inside are escapes
            return s.substring(1, n - 1).replace("\"\"", "\"");
        }
        return s;
    }

    private static boolean isPositiveInteger(String s) {
        // Fast path: all digits, no sign, no decimal.
        for (int i = 0, n = s.length(); i < n; i++) {
            char ch = s.charAt(i);
            if (ch < '0' || ch > '9') return false;
        }
        return !s.isEmpty();
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<OrderItem> parse(Cursor cur, ParseContext ctx) {
        Integer ordinal = null;
        Expression expr = null;

        // Positional ORDER BY: "1", "2", ...
        if (isPositiveInteger(cur.peek().lexeme())) {
            int pos = Integer.parseInt(cur.peek().lexeme());
            if (pos <= 0) {
                return error("ORDER BY position must be a positive integer", pos);
            }
            ordinal = pos;
            cur.advance();
        }
        else {
            // Otherwise: delegate to the expression parser
            var result = ctx.parse(Expression.class, cur);
            if (result.isError()) {
                return error(result);
            }
            expr = result.value();
        }

        Direction direction = null;
        Nulls nulls = null;
        String collate = null;
        String usingOperator = null;

        var tokens = Set.of(TokenType.ASC, TokenType.DESC, TokenType.NULLS, TokenType.COLLATE, TokenType.USING);

        while (cur.matchAny(tokens)) {
            if (cur.consumeIf(TokenType.ASC)) {
                if (direction != null) {
                    return error("Direction specified more than once", cur.fullPos());
                }
                if (usingOperator != null) {
                    return error("USING operator cannot be combined with ASC/DESC", cur.fullPos());
                }
                direction = Direction.ASC;
                continue;
            }
            if (cur.consumeIf(TokenType.DESC)) {
                if (direction != null) {
                    return error("Direction specified more than once", cur.fullPos());
                }
                if (usingOperator != null) {
                    return error("USING operator cannot be combined with ASC/DESC", cur.fullPos());
                }
                direction = Direction.DESC;
                continue;
            }
            if (cur.consumeIf(TokenType.NULLS)) {
                if (nulls != null) return error("NULLS specified more than once", cur.fullPos());
                var t = cur.expect("Expected FIRST | LAST | DEFAULT after NULLS", TokenType.FIRST, TokenType.LAST, TokenType.DEFAULT);
                if (t.type() == TokenType.FIRST) nulls = Nulls.FIRST;
                else if (t.type() == TokenType.LAST) nulls = Nulls.LAST;
                else if (t.type() == TokenType.DEFAULT) nulls = Nulls.DEFAULT;
                else return error("Expected FIRST | LAST | DEFAULT after NULLS", cur.fullPos());
                continue;
            }
            if (cur.consumeIf(TokenType.COLLATE)) {
                if (collate != null) {
                    return error("COLLATE specified more than once", cur.fullPos());
                }
                var t = cur.expect("Expected collation name after COLLATE", TokenType.IDENT);
                collate = unquoteIfQuoted(t.lexeme());
                continue;
            }
            if (cur.consumeIf(TokenType.USING)) {
                if (usingOperator != null) {
                    return error("USING specified more than once", cur.fullPos());
                }
                if (direction != null) {
                    return error("USING operator cannot be combined with ASC/DESC", cur.fullPos());
                }
                if (cur.match(TokenType.OPERATOR)) {
                    usingOperator = cur.advance().lexeme();
                }
                else if (cur.match(TokenType.QMARK)) {
                    usingOperator = cur.advance().lexeme();
                }
                else {
                    return error("Expected operator after USING", cur.fullPos());
                }
            }
        }

        if (expr != null) {
            return ok(OrderItem.of(expr, null, direction, nulls, collate, usingOperator));
        }
        return ok(OrderItem.of(null, ordinal, direction, nulls, collate, usingOperator));
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<OrderItem> targetType() {
        return OrderItem.class;
    }
}
