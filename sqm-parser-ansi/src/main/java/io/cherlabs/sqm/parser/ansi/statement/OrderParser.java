package io.cherlabs.sqm.parser.ansi.statement;

import io.cherlabs.sqm.core.Column;
import io.cherlabs.sqm.core.Direction;
import io.cherlabs.sqm.core.Nulls;
import io.cherlabs.sqm.core.Order;
import io.cherlabs.sqm.parser.core.Cursor;
import io.cherlabs.sqm.parser.core.TokenType;
import io.cherlabs.sqm.parser.spi.ParseContext;
import io.cherlabs.sqm.parser.spi.ParseResult;
import io.cherlabs.sqm.parser.spi.Parser;

/**
 * A spec parser for order by specifications.
 * <p>Example:</p>
 * <pre>
 *     {@code
 *     "o.status ASC", "u.name DESC"
 *     }
 * </pre>
 */
public class OrderParser implements Parser<Order> {

    private static String unquoteIfQuoted(String s) {
        int n = s.length();
        if (n >= 2 && s.charAt(0) == '"' && s.charAt(n - 1) == '"') {
            // ANSI: doubled quotes inside are escapes
            return s.substring(1, n - 1).replace("\"\"", "\"");
        }
        return s;
    }

    /**
     * Gets the {@link Order} type.
     *
     * @return {@link Order} type.
     */
    @Override
    public Class<Order> targetType() {
        return Order.class;
    }

    /**
     * Parses the order by specification.
     *
     * @param cur the {@link Cursor} class containing the tokens.
     * @return a parser result.
     */
    @Override
    public ParseResult<Order> parse(Cursor cur, ParseContext ctx) {
        // 1) Find end of the column-spec portion (before ASC|DESC|NULLS|COLLATE outside parens)
        final int colEnd = cur.find(TokenType.ASC, TokenType.DESC, TokenType.NULLS, TokenType.COLLATE, TokenType.EOF);
        if (colEnd == cur.size()) {
            return error("Missing column in ORDER BY item", cur.fullPos());
        }

        // 2) Parse the column via ColumnSpecParser on the exact substring slice
        var cr = ctx.parse(Column.class, cur.advance(colEnd));
        if (cr.isError()) {
            return error(cr);
        }
        final Column column = cr.value();

        // 3) Parse optional modifiers in any order from the remaining tokens
        Direction direction = null;
        Nulls nulls = null;
        String collate = null;

        while (!cur.isEof()) {
            if (cur.consumeIf(TokenType.ASC)) {
                if (direction != null) {
                    return error("Direction specified more than once", cur.fullPos());
                }
                direction = Direction.Asc;
                continue;
            }
            if (cur.consumeIf(TokenType.DESC)) {
                if (direction != null) {
                    return error("Direction specified more than once", cur.fullPos());
                }
                direction = Direction.Desc;
                continue;
            }
            if (cur.consumeIf(TokenType.NULLS)) {
                if (nulls != null) return ParseResult.error("NULLS specified more than once", cur.fullPos());
                var t = cur.expect("Expected FIRST | LAST | DEFAULT after NULLS", TokenType.FIRST, TokenType.LAST, TokenType.DEFAULT);
                if (t.type() == TokenType.FIRST) nulls = Nulls.First;
                else if (t.type() == TokenType.LAST) nulls = Nulls.Last;
                else if (t.type() == TokenType.DEFAULT) nulls = Nulls.Default;
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

            // If anything non-whitespace / non-EOF remains -> error.
            return error("Unexpected token in ORDER BY item: " + cur.peek().lexeme(), cur.fullPos());
        }

        return ok(new Order(column, direction, nulls, collate));
    }
}
