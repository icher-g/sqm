package io.sqm.parser.postgresql;

import io.sqm.core.*;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses PostgreSQL {@code INSERT} statements, including optional {@code ON CONFLICT} and {@code RETURNING}.
 */
public class InsertStatementParser extends io.sqm.parser.ansi.InsertStatementParser {

    /**
     * Creates a PostgreSQL insert-statement parser.
     */
    public InsertStatementParser() {
    }

    /**
     * Parses optional PostgreSQL {@code ON CONFLICT} clause.
     *
     * @param cur token cursor
     * @param ctx parse context
     * @return parsed conflict-clause details, or no-conflict details when omitted
     */
    @Override
    protected ParseResult<OnConflictClause> parseOnConflict(Cursor cur, ParseContext ctx) {
        if (!cur.consumeIf(TokenType.ON)) {
            return ok(noOnConflictClause());
        }
        cur.expect("Expected CONFLICT after ON", TokenType.CONFLICT);

        if (!ctx.capabilities().supports(SqlFeature.INSERT_ON_CONFLICT)) {
            return error("INSERT ... ON CONFLICT is not supported by this dialect", cur.fullPos());
        }

        List<Identifier> target = List.of();

        if (cur.consumeIf(TokenType.LPAREN)) {
            target = parseIdentifierItems(cur, "Expected conflict target column");
            cur.expect("Expected ) after conflict target", TokenType.RPAREN);
        }

        cur.expect("Expected DO after ON CONFLICT", TokenType.DO);

        if (cur.consumeIf(TokenType.NOTHING)) {
            return ok(onConflictDoNothingClause(target));
        }

        cur.expect("Expected NOTHING or UPDATE after DO", TokenType.UPDATE);
        cur.expect("Expected SET after DO UPDATE", TokenType.SET);

        var assignments = parseItems(Assignment.class, cur, ctx);
        if (assignments.isError()) {
            return error(assignments);
        }

        Predicate where = null;

        if (cur.consumeIf(TokenType.WHERE)) {
            var whereResult = ctx.parse(Predicate.class, cur);
            if (whereResult.isError()) {
                return error(whereResult);
            }
            where = whereResult.value();
        }

        return ok(onConflictDoUpdateClause(target, assignments.value(), where));
    }

    /**
     * Parses optional PostgreSQL {@code RETURNING} clause.
     *
     * @param cur token cursor
     * @param ctx parse context
     * @return returning projection list, or empty list when omitted
     */
    @Override
    protected ParseResult<ResultClause> parseReturning(Cursor cur, ParseContext ctx) {
        if (!cur.consumeIf(TokenType.RETURNING)) {
            return ok(null);
        }

        if (!ctx.capabilities().supports(SqlFeature.DML_RESULT_CLAUSE)) {
            return error("INSERT ... RETURNING is not supported by this dialect", cur.fullPos());
        }

        var returningResult = parseItems(ResultItem.class, cur, ctx);
        if (returningResult.isError()) {
            return error(returningResult);
        }
        return ok(ResultClause.of(returningResult.value()));
    }
}
