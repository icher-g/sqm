package io.sqm.parser.ansi;

import io.sqm.core.Identifier;
import io.sqm.core.InsertSource;
import io.sqm.core.InsertStatement;
import io.sqm.core.Query;
import io.sqm.core.RowExpr;
import io.sqm.core.RowListExpr;
import io.sqm.core.RowValues;
import io.sqm.core.SelectItem;
import io.sqm.core.Table;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses baseline ANSI {@code INSERT} statements.
 */
public class InsertStatementParser implements Parser<InsertStatement> {

    /**
     * Creates an insert-statement parser.
     */
    public InsertStatementParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<? extends InsertStatement> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected INSERT", TokenType.INSERT);
        cur.expect("Expected INTO after INSERT", TokenType.INTO);

        var table = ctx.parse(Table.class, cur);
        if (table.isError()) {
            return error(table);
        }

        List<Identifier> columns = List.of();

        if (cur.consumeIf(TokenType.LPAREN)) {
            var parsedColumns = parseTargetColumns(cur);
            if (parsedColumns.isError()) {
                return error(parsedColumns);
            }
            cur.expect("Expected ) after target columns", TokenType.RPAREN);
            columns = parsedColumns.value();
        }

        var source = parseInsertSource(cur, ctx);
        if (source.isError()) {
            return error(source);
        }

        var returning = parseReturning(cur, ctx);
        if (returning.isError()) {
            return error(returning);
        }

        return ok(InsertStatement.of(table.value(), columns, source.value(), returning.value()));
    }

    /**
     * Parses optional {@code RETURNING} clause.
     *
     * @param cur token cursor
     * @param ctx parse context
     * @return returning projection list, or empty list when omitted
     */
    protected ParseResult<List<SelectItem>> parseReturning(Cursor cur, ParseContext ctx) {
        if (cur.match(TokenType.RETURNING)) {
            return error("INSERT ... RETURNING is not supported by this dialect", cur.fullPos());
        }
        return ok(List.of());
    }

    private ParseResult<List<Identifier>> parseTargetColumns(Cursor cur) {
        List<Identifier> columns = new ArrayList<>();
        do {
            columns.add(toIdentifier(cur.expect("Expected target column", TokenType.IDENT)));
        } while (cur.consumeIf(TokenType.COMMA));

        return ok(List.copyOf(columns));
    }

    private ParseResult<? extends InsertSource> parseInsertSource(Cursor cur, ParseContext ctx) {
        if (cur.consumeIf(TokenType.VALUES)) {
            return parseValuesSource(cur, ctx);
        }

        var query = ctx.parse(Query.class, cur);
        if (query.isError()) {
            return error(query);
        }
        return ok(query.value());
    }

    private ParseResult<? extends InsertSource> parseValuesSource(Cursor cur, ParseContext ctx) {
        var rows = parseItems(RowExpr.class, cur, ctx);
        if (rows.isError()) {
            return error(rows);
        }

        RowValues values = rows.value().size() == 1 ? rows.value().getFirst() : RowListExpr.of(rows.value());
        return ok(values);
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<InsertStatement> targetType() {
        return InsertStatement.class;
    }
}
