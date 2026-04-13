package io.sqm.parser.mysql;

import io.sqm.core.Identifier;
import io.sqm.core.Table;
import io.sqm.core.TableHint;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.Token;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.ArrayList;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * MySQL table parser with support for table index hints.
 */
public class TableParser extends io.sqm.parser.ansi.TableParser {

    /**
     * Creates a MySQL table parser.
     */
    public TableParser() {
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

        var hints = new ArrayList<TableHint>();
        Identifier alias = null;

        while (true) {
            if (cur.matchAny(TokenType.USE, TokenType.IGNORE, TokenType.FORCE)) {
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
        return cur.match(TokenType.AS) || cur.match(TokenType.IDENT);
    }

    private ParseResult<TableHint> parseIndexHint(Cursor cur) {
        var typeToken = readIndexHintType(cur);
        if (typeToken == null) {
            return error("Expected USE, IGNORE or FORCE", cur.fullPos());
        }

        cur.expect("Expected INDEX or KEY in table index hint", TokenType.KEY, TokenType.INDEX);

        String scope = "";

        int forPos = cur.fullPos();
        if (cur.consumeIf(TokenType.FOR)) {
            if (cur.consumeIf(TokenType.JOIN)) {
                scope = "JOIN";
            }
            else if (cur.consumeIf(TokenType.ORDER)) {
                cur.expect("Expected BY after ORDER", TokenType.BY);
                scope = "ORDER_BY";
            }
            else if (cur.consumeIf(TokenType.GROUP)) {
                cur.expect("Expected BY after GROUP", TokenType.BY);
                scope = "GROUP_BY";
            }
            else {
                return error("Expected JOIN, ORDER BY or GROUP BY after FOR", forPos);
            }
        }

        cur.expect("Expected ( after INDEX hint", TokenType.LPAREN);
        var indexes = parseIdentifierItems(cur, "Expected index identifier");
        cur.expect("Expected ) after INDEX hint", TokenType.RPAREN);

        return ok(TableHint.of(
            MySqlHintParserSupport.indexHintName(typeToken.type().name(), scope),
            indexes.toArray()
        ));
    }

    private Token readIndexHintType(Cursor cur) {
        if (cur.matchAny(TokenType.USE, TokenType.IGNORE, TokenType.FORCE)) {
            return cur.advance();
        }
        return null;
    }
}
