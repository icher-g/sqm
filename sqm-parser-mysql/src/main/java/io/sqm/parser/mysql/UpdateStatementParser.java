package io.sqm.parser.mysql;

import io.sqm.core.Join;
import io.sqm.core.ResultClause;
import io.sqm.core.ResultItem;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.ansi.Indicators;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.ArrayList;
import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses MySQL {@code UPDATE} statements, including joined target forms.
 */
public class UpdateStatementParser extends io.sqm.parser.ansi.UpdateStatementParser {

    /**
     * Creates a MySQL update parser.
     */
    public UpdateStatementParser() {
    }

    /**
     * Parses tokens that may appear after {@code UPDATE} and before the target table.
     *
     * @param cur token cursor
     * @param ctx parse context
     * @return parsed optimizer hints or an empty list when omitted
     */
    @Override
    protected ParseResult<List<String>> parseAfterUpdateKeyword(Cursor cur, ParseContext ctx) {
        var hints = new ArrayList<String>();
        while (cur.match(TokenType.COMMENT_HINT)) {
            if (!ctx.capabilities().supports(SqlFeature.OPTIMIZER_HINT_COMMENT)) {
                return error("Optimizer hint comments are not supported by this dialect", cur.fullPos());
            }
            hints.add(cur.advance().lexeme());
        }
        return ok(List.copyOf(hints));
    }

    /**
     * Parses optional joined sources attached to the target table.
     *
     * @param cur token cursor
     * @param ctx parse context
     * @return joined sources or empty list when omitted
     */
    @Override
    protected ParseResult<List<Join>> parseJoins(Cursor cur, ParseContext ctx) {
        if (!cur.matchAny(Indicators.JOIN)) {
            return ok(List.of());
        }
        if (!ctx.capabilities().supports(SqlFeature.UPDATE_JOIN)) {
            return error("UPDATE ... JOIN is not supported by this dialect", cur.fullPos());
        }

        var joins = new ArrayList<Join>();
        while (cur.matchAny(Indicators.JOIN)) {
            var join = ctx.parse(Join.class, cur);
            if (join.isError()) {
                return error(join);
            }
            joins.add(join.value());
        }
        return ok(List.copyOf(joins));
    }

    /**
     * Parses optional MySQL {@code RETURNING} clause.
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
            return error("UPDATE ... RETURNING is not supported by this dialect", cur.fullPos());
        }
        var returningResult = parseItems(ResultItem.class, cur, ctx);
        if (returningResult.isError()) {
            return error(returningResult);
        }
        return ok(ResultClause.of(returningResult.value()));
    }
}
