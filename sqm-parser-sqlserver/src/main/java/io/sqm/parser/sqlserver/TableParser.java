package io.sqm.parser.sqlserver;

import io.sqm.core.Identifier;
import io.sqm.core.Table;
import io.sqm.core.TableHint;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * SQL Server table parser with support for {@code WITH (...)} table hints.
 */
public class TableParser extends io.sqm.parser.ansi.TableParser {

    /**
     * Creates a SQL Server table parser.
     */
    public TableParser() {
    }

    /**
     * Parses SQL Server-specific table suffixes such as aliases and table hints.
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

        Identifier alias = null;
        if (looksLikeAliasStart(cur)) {
            alias = parseAliasIdentifier(cur);
        }

        var hints = new ArrayList<TableHint>();
        if (cur.consumeIf(TokenType.WITH)) {
            if (!ctx.capabilities().supports(SqlFeature.TABLE_LOCK_HINT)) {
                return error("SQL Server table hints are not supported by this dialect", cur.fullPos());
            }
            cur.expect("Expected ( after WITH in SQL Server table hint clause", TokenType.LPAREN);
            var parsedHints = parseHintList(cur);
            if (parsedHints.isError()) {
                return error(parsedHints);
            }
            hints.addAll(parsedHints.value());
            cur.expect("Expected ) after SQL Server table hints", TokenType.RPAREN);
        }

        return ok(Table.of(schema, name, alias, inheritance, hints));
    }

    private boolean looksLikeAliasStart(Cursor cur) {
        return cur.match(TokenType.AS) || cur.match(TokenType.IDENT);
    }

    private ParseResult<List<TableHint>> parseHintList(Cursor cur) {
        var hints = new ArrayList<TableHint>();
        var seen = new HashSet<String>();

        do {
            var hint = parseHint(cur);
            if (hint == null) {
                return error("Expected SQL Server table hint", cur.fullPos());
            }

            if (!seen.add(hint.name().value())) {
                return error("Duplicate SQL Server table hint " + hint.name().value(), cur.fullPos());
            }

            hints.add(hint);

        } while (cur.consumeIf(TokenType.COMMA));

        if (seen.contains("NOLOCK") && (seen.contains("UPDLOCK") || seen.contains("HOLDLOCK"))) {
            return error("SQL Server NOLOCK cannot be combined with UPDLOCK or HOLDLOCK", cur.fullPos());
        }

        return ok(List.copyOf(hints));
    }

    private TableHint parseHint(Cursor cur) {
        if (cur.consumeIf(TokenType.NOLOCK)) {
            return TableHint.of("NOLOCK");
        }
        if (cur.consumeIf(TokenType.UPDLOCK)) {
            return TableHint.of("UPDLOCK");
        }
        if (cur.consumeIf(TokenType.HOLDLOCK)) {
            return TableHint.of("HOLDLOCK");
        }
        return null;
    }
}
