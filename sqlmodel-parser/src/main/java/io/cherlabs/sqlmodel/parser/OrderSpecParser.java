package io.cherlabs.sqlmodel.parser;

import io.cherlabs.sqlmodel.core.Column;
import io.cherlabs.sqlmodel.core.Direction;
import io.cherlabs.sqlmodel.core.Nulls;
import io.cherlabs.sqlmodel.core.Order;
import io.cherlabs.sqlmodel.parser.core.Cursor;
import io.cherlabs.sqlmodel.parser.core.TokenType;

public class OrderSpecParser implements SpecParser<Order> {

    private static String unquoteIfQuoted(String s) {
        int n = s.length();
        if (n >= 2 && s.charAt(0) == '"' && s.charAt(n - 1) == '"') {
            // ANSI: doubled quotes inside are escapes
            return s.substring(1, n - 1).replace("\"\"", "\"");
        }
        return s;
    }

    @Override
    public Class<Order> targetType() {
        return Order.class;
    }

    @Override
    public ParseResult<Order> parse(Cursor cur) {
        // 1) Find end of the column-spec portion (before ASC|DESC|NULLS|COLLATE outside parens)
        final int colEnd = cur.find(TokenType.ASC, TokenType.DESC, TokenType.NULLS, TokenType.COLLATE, TokenType.EOF);
        if (colEnd <= 0) {
            return ParseResult.error("Missing column in ORDER BY item");
        }

        // 2) Parse the column via ColumnSpecParser on the exact substring slice
        var colCur = cur.sliceUntil(colEnd);
        final ParseResult<Column> colRes = new ColumnSpecParser().parse(colCur);
        if (!colRes.ok()) {
            return ParseResult.error(colRes);
        }
        cur.setPos(colEnd);
        final Column column = colRes.value();

        // 3) Parse optional modifiers in any order from the remaining tokens
        Direction direction = null;
        Nulls nulls = null;
        String collate = null;

        while (!cur.isEof()) {
            if (cur.consumeIf(TokenType.ASC)) {
                if (direction != null) return ParseResult.error("Direction specified more than once");
                direction = Direction.Asc;
                continue;
            }
            if (cur.consumeIf(TokenType.DESC)) {
                if (direction != null) return ParseResult.error("Direction specified more than once");
                direction = Direction.Desc;
                continue;
            }
            if (cur.consumeIf(TokenType.NULLS)) {
                if (nulls != null) return ParseResult.error("NULLS specified more than once");
                var t = cur.expect("Expected FIRST | LAST | DEFAULT after NULLS", TokenType.FIRST, TokenType.LAST, TokenType.DEFAULT);
                if (t.type() == TokenType.FIRST) nulls = Nulls.First;
                else if (t.type() == TokenType.LAST) nulls = Nulls.Last;
                else if (t.type() == TokenType.DEFAULT) nulls = Nulls.Default;
                else return ParseResult.error("Expected FIRST | LAST | DEFAULT after NULLS");
                continue;
            }
            if (cur.consumeIf(TokenType.COLLATE)) {
                if (collate != null) return ParseResult.error("COLLATE specified more than once");
                var t = cur.expect("Expected collation name after COLLATE", TokenType.IDENT);
                collate = unquoteIfQuoted(t.lexeme());
                continue;
            }

            // If anything non-whitespace / non-EOF remains -> error.
            return ParseResult.error("Unexpected token in ORDER BY item: " + cur.peek().lexeme());
        }

        return ParseResult.ok(new Order(column, direction, nulls, collate));
    }
}
