package io.cherlabs.sqm.parser;

import io.cherlabs.sqm.core.Filter;
import io.cherlabs.sqm.core.Join;
import io.cherlabs.sqm.core.NamedTable;
import io.cherlabs.sqm.core.Table;
import io.cherlabs.sqm.parser.core.Cursor;
import io.cherlabs.sqm.parser.core.TokenType;

/**
 * Parses a JOIN clause into a {@link Join}.
 * Grammar (case-insensitive):
 * [INNER|LEFT [OUTER]|RIGHT [OUTER]|FULL [OUTER]|CROSS] JOIN table [AS alias] [ON <boolean-expr>]
 * Examples:
 * JOIN products p ON p.category_id = c.id
 * LEFT OUTER JOIN warehouses AS w ON w.product_id = p.id AND w.stock > 0
 * CROSS JOIN regions r
 */
public final class JoinSpecParser implements SpecParser<Join> {

    private static Join.JoinType mapJoinType(TokenType tt) {
        return switch (tt) {
            case LEFT -> Join.JoinType.Left;
            case RIGHT -> Join.JoinType.Right;
            case FULL -> Join.JoinType.Full;
            case CROSS -> Join.JoinType.Cross;
            default -> Join.JoinType.Inner;
        };
    }

    /* ---------------- helpers ---------------- */

    private static Join buildJoin(Join.JoinType type, Table table, Filter on) {
        return switch (type) {
            case Inner -> (on != null) ? Join.inner(table).on(on) : Join.inner(table);
            case Left -> (on != null) ? Join.left(table).on(on) : Join.left(table);
            case Right -> (on != null) ? Join.right(table).on(on) : Join.right(table);
            case Full -> (on != null) ? Join.full(table).on(on) : Join.full(table);
            case Cross -> Join.cross(table);
        };
    }

    /**
     * Gets the {@link Join} type.
     *
     * @return {@link Join} type.
     */
    @Override
    public Class<Join> targetType() {
        return Join.class;
    }

    /**
     * Parses the join specification.
     *
     * @param cur the {@link Cursor} class containing the tokens.
     * @return a parser result.
     */
    @Override
    public ParseResult<Join> parse(Cursor cur) {
        // Optional join type
        Join.JoinType type = Join.JoinType.Inner;

        if (cur.matchAny(TokenType.INNER, TokenType.LEFT, TokenType.RIGHT, TokenType.FULL, TokenType.CROSS)) {
            type = mapJoinType(cur.advance().type());
            cur.consumeIf(TokenType.OUTER); // LEFT/RIGHT/FULL OUTER
        }

        // JOIN keyword
        cur.expect(TokenType.JOIN);

        // Table (possibly qualified via dots)
        var t = cur.expect("Expected table name after JOIN", TokenType.IDENT);

        String table = t.lexeme();
        String schema = null;

        while (cur.consumeIf(TokenType.DOT)) {
            t = cur.expect("Expected identifier after '.' in table", TokenType.IDENT);
            schema = table;
            table = t.lexeme();
        }

        // Optional alias: AS identifier | bare identifier (not a keyword)
        String alias = null;
        if (cur.consumeIf(TokenType.AS)) {
            t = cur.expect("Expected alias after AS", TokenType.IDENT);
            alias = t.lexeme();
        } else if (cur.match(TokenType.IDENT)) {
            alias = cur.advance().lexeme();
        }

        // Optional ON <expr> (not for CROSS)
        Filter on = null;
        if (type != Join.JoinType.Cross && cur.consumeIf(TokenType.ON)) {
            var subCur = cur.sliceUntil(cur.size());
            var fr = new FilterSpecParser().parse(subCur);
            if (!fr.ok()) return ParseResult.error(fr);
            on = fr.value();
        }

        // 6) Build the Join
        return ParseResult.ok(buildJoin(type, new NamedTable(table, alias, schema), on));
    }
}
