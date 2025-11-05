package io.sqm.parser.ansi;

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

        // Positional GROUP BY: "1", "2", ...
        // SQL allows positive 1-based ordinals.
        if (isPositiveInteger(cur.peek().lexeme())) {
            int pos = Integer.parseInt(cur.peek().lexeme());
            if (pos <= 0) {
                return ParseResult.error("GROUP BY position must be a positive integer", pos);
            }
            ordinal = pos;
        }
        else {
            // Otherwise: delegate to the column parser
            var result = ctx.parse(Expression.class, cur);
            if (result.isError()) {
                return ParseResult.error(result);
            }
            expr = result.value();
        }

        // 3) Parse optional modifiers in any order from the remaining tokens
        Direction direction = null;
        Nulls nulls = null;
        String collate = null;

        var tokens = Set.of(TokenType.ASC, TokenType.DESC, TokenType.NULLS, TokenType.COLLATE);

        while (cur.matchAny(tokens)) {
            if (cur.consumeIf(TokenType.ASC)) {
                if (direction != null) {
                    return error("Direction specified more than once", cur.fullPos());
                }
                direction = Direction.ASC;
                continue;
            }
            if (cur.consumeIf(TokenType.DESC)) {
                if (direction != null) {
                    return error("Direction specified more than once", cur.fullPos());
                }
                direction = Direction.DESC;
                continue;
            }
            if (cur.consumeIf(TokenType.NULLS)) {
                if (nulls != null) return ParseResult.error("NULLS specified more than once", cur.fullPos());
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
            }
        }

        if (expr != null) {
            return finalize(cur, ctx, OrderItem.of(expr, direction, nulls, collate));
        }
        return finalize(cur, ctx, OrderItem.of(ordinal, direction, nulls, collate));
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
