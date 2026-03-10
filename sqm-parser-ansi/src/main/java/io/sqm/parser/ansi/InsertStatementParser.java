package io.sqm.parser.ansi;

import io.sqm.core.*;
import io.sqm.core.InsertStatement.InsertMode;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

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
        var insertMode = parseInsertMode(cur, ctx);
        if (insertMode.isError()) {
            return error(insertMode);
        }

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

        var conflict = parseOnConflict(cur, ctx);
        if (conflict.isError()) {
            return error(conflict);
        }

        var returning = parseReturning(cur, ctx);
        if (returning.isError()) {
            return error(returning);
        }

        return ok(InsertStatement.of(
            insertMode.value(),
            table.value(),
            columns,
            source.value(),
            conflict.value().target(),
            conflict.value().action(),
            conflict.value().assignments(),
            conflict.value().where(),
            returning.value()));
    }

    /**
     * Parses the leading insert mode.
     *
     * @param cur token cursor
     * @param ctx parse context
     * @return parsed insert mode
     */
    protected ParseResult<InsertMode> parseInsertMode(Cursor cur, ParseContext ctx) {
        cur.expect("Expected INSERT", TokenType.INSERT);
        return ok(InsertMode.STANDARD);
    }

    /**
     * Parses optional {@code ON CONFLICT} clause.
     *
     * @param cur token cursor
     * @param ctx parse context
     * @return conflict-clause details, or no-conflict details when omitted
     */
    protected ParseResult<OnConflictClause> parseOnConflict(Cursor cur, ParseContext ctx) {
        if (cur.match(TokenType.ON)) {
            return error("INSERT ... ON CONFLICT is not supported by this dialect", cur.fullPos());
        }
        return ok(noOnConflictClause());
    }

    /**
     * Creates no-conflict clause details.
     *
     * @return no-conflict clause
     */
    protected final OnConflictClause noOnConflictClause() {
        return OnConflictClause.none();
    }

    /**
     * Creates {@code ON CONFLICT ... DO NOTHING} clause details.
     *
     * @param target conflict target
     * @return parsed clause details
     */
    protected final OnConflictClause onConflictDoNothingClause(List<Identifier> target) {
        return new OnConflictClause(List.copyOf(target), InsertStatement.OnConflictAction.DO_NOTHING, List.of(), null);
    }

    /**
     * Creates {@code ON CONFLICT ... DO UPDATE SET ...} clause details.
     *
     * @param target      conflict target
     * @param assignments update assignments
     * @param where       optional conflict-update predicate
     * @return parsed clause details
     */
    protected final OnConflictClause onConflictDoUpdateClause(List<Identifier> target,
        List<Assignment> assignments,
        Predicate where) {
        return new OnConflictClause(List.copyOf(target), InsertStatement.OnConflictAction.DO_UPDATE, List.copyOf(assignments), where);
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

    /**
     * Parses insert target columns.
     *
     * @param cur token cursor
     * @return parsed target columns
     */
    protected final ParseResult<List<Identifier>> parseTargetColumns(Cursor cur) {
        return ok(parseIdentifierItems(cur, "Expected target column"));
    }

    /**
     * Parses the insert source.
     *
     * @param cur token cursor
     * @param ctx parse context
     * @return parsed insert source
     */
    protected final ParseResult<? extends InsertSource> parseInsertSource(Cursor cur, ParseContext ctx) {
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

    /**
     * Parsed optional ON CONFLICT clause details.
     *
     * @param target      conflict target
     * @param action      an {@code ON CONFLICT} action.
     * @param assignments update assignments
     * @param where       optional conflict-update predicate
     */
    protected record OnConflictClause(List<Identifier> target,
                                      InsertStatement.OnConflictAction action,
                                      List<Assignment> assignments,
                                      Predicate where) {
        private static OnConflictClause none() {
            return new OnConflictClause(List.of(), InsertStatement.OnConflictAction.NONE, List.of(), null);
        }
    }
}
