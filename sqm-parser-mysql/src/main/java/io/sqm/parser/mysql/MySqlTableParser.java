package io.sqm.parser.mysql;

import io.sqm.core.Identifier;
import io.sqm.core.Table;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.ansi.TableParser;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.ArrayList;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * MySQL table parser with support for table index hints.
 */
public class MySqlTableParser extends TableParser {

    /**
     * Creates a MySQL table parser.
     */
    public MySqlTableParser() {
    }

    /**
     * Parses MySQL-specific table suffixes such as index hints.
     *
     * @param cur         token cursor.
     * @param ctx         parse context.
     * @param schema      resolved table schema, or {@code null}.
     * @param name        resolved table name.
     * @param inheritance resolved inheritance mode.
     * @return parsed table result.
     */
    @Override
    protected ParseResult<Table> parseAfterQualifiedName(
        Cursor cur,
        ParseContext ctx,
        Identifier schema,
        Identifier name,
        Table.Inheritance inheritance) {
        var hints = new ArrayList<Table.IndexHint>();
        Identifier alias = null;

        while (true) {
            if (isIndexHintKeyword(cur)) {
                if (!ctx.capabilities().supports(SqlFeature.TABLE_INDEX_HINT)) {
                    return error("Table index hints are not supported by this dialect", cur.fullPos());
                }
                var hint = parseIndexHint(cur);
                if (hint.isError()) {
                    return error(hint);
                }
                hints.add(hint.value());
                continue;
            }
            if (alias == null && looksLikeAliasStart(cur)) {
                alias = parseAliasIdentifier(cur);
                continue;
            }
            break;
        }

        return ok(Table.of(schema, name, alias, inheritance, hints));
    }

    private boolean looksLikeAliasStart(Cursor cur) {
        return cur.match(TokenType.AS) || (cur.match(TokenType.IDENT) && !isIndexHintKeyword(cur));
    }

    private boolean isIndexHintKeyword(Cursor cur) {
        return cur.match(TokenType.IDENT)
            && ("USE".equalsIgnoreCase(cur.peek().lexeme())
            || "IGNORE".equalsIgnoreCase(cur.peek().lexeme())
            || "FORCE".equalsIgnoreCase(cur.peek().lexeme()));
    }

    private ParseResult<Table.IndexHint> parseIndexHint(Cursor cur) {
        var typeToken = cur.expect("Expected index hint type", TokenType.IDENT);
        Table.IndexHintType type;
        if ("USE".equalsIgnoreCase(typeToken.lexeme())) {
            type = Table.IndexHintType.USE;
        }
        else if ("IGNORE".equalsIgnoreCase(typeToken.lexeme())) {
            type = Table.IndexHintType.IGNORE;
        }
        else if ("FORCE".equalsIgnoreCase(typeToken.lexeme())) {
            type = Table.IndexHintType.FORCE;
        }
        else {
            return error("Expected USE, IGNORE or FORCE", cur.fullPos());
        }

        if (!(cur.match(TokenType.IDENT) && "INDEX".equalsIgnoreCase(cur.peek().lexeme())
            || cur.match(TokenType.KEY))) {
            return error("Expected INDEX or KEY in table index hint", cur.fullPos());
        }
        cur.advance();

        Table.IndexHintScope scope = Table.IndexHintScope.DEFAULT;
        if (cur.consumeIf(TokenType.FOR)) {
            if (cur.consumeIf(TokenType.JOIN)) {
                scope = Table.IndexHintScope.JOIN;
            }
            else if (cur.consumeIf(TokenType.ORDER)) {
                cur.expect("Expected BY after ORDER", TokenType.BY);
                scope = Table.IndexHintScope.ORDER_BY;
            }
            else if (cur.consumeIf(TokenType.GROUP)) {
                cur.expect("Expected BY after GROUP", TokenType.BY);
                scope = Table.IndexHintScope.GROUP_BY;
            }
            else {
                return error("Expected JOIN, ORDER BY or GROUP BY after FOR", cur.fullPos());
            }
        }

        cur.expect("Expected ( after INDEX hint", TokenType.LPAREN);
        var indexes = new ArrayList<Identifier>();
        indexes.add(toIdentifier(cur.expect("Expected index identifier", TokenType.IDENT)));
        while (cur.consumeIf(TokenType.COMMA)) {
            indexes.add(toIdentifier(cur.expect("Expected index identifier", TokenType.IDENT)));
        }
        cur.expect("Expected ) after INDEX hint", TokenType.RPAREN);

        return ok(new Table.IndexHint(type, scope, indexes));
    }
}
