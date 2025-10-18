package io.sqm.parser.ansi.join;

import io.sqm.core.Filter;
import io.sqm.core.Join;
import io.sqm.core.Table;
import io.sqm.core.TableJoin;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

public class TableJoinParser implements Parser<TableJoin> {

    private static Join.JoinType mapJoinType(TokenType tt) {
        return switch (tt) {
            case LEFT -> Join.JoinType.Left;
            case RIGHT -> Join.JoinType.Right;
            case FULL -> Join.JoinType.Full;
            case CROSS -> Join.JoinType.Cross;
            default -> Join.JoinType.Inner;
        };
    }

    private static TableJoin buildJoin(Join.JoinType type, Table table, Filter on) {
        return switch (type) {
            case Inner -> (on != null) ? Join.inner(table).on(on) : Join.inner(table);
            case Left -> (on != null) ? Join.left(table).on(on) : Join.left(table);
            case Right -> (on != null) ? Join.right(table).on(on) : Join.right(table);
            case Full -> (on != null) ? Join.full(table).on(on) : Join.full(table);
            case Cross -> Join.cross(table);
        };
    }

    @Override
    public ParseResult<TableJoin> parse(Cursor cur, ParseContext ctx) {
        // Optional join type
        Join.JoinType type = Join.JoinType.Inner;

        if (cur.matchAny(TokenType.INNER, TokenType.LEFT, TokenType.RIGHT, TokenType.FULL, TokenType.CROSS)) {
            type = mapJoinType(cur.advance().type());
            cur.consumeIf(TokenType.OUTER); // LEFT/RIGHT/FULL OUTER
        }

        // JOIN keyword
        cur.expect("Expect JOIN", TokenType.JOIN);

        // Table
        var table = ctx.parse(Table.class, cur);
        if (table.isError()) {
            return error(table);
        }

        // Optional ON <expr> (not for CROSS)
        Filter on = null;
        if (type != Join.JoinType.Cross && cur.consumeIf(TokenType.ON)) {
            var fr = ctx.parse(Filter.class, cur);
            if (fr.isError()) {
                return error(fr);
            }
            on = fr.value();
        }

        // 6) Build the Join
        return ok(buildJoin(type, table.value(), on));
    }

    @Override
    public Class<TableJoin> targetType() {
        return TableJoin.class;
    }
}
