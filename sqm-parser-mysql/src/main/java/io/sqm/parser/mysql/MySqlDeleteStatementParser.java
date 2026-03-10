package io.sqm.parser.mysql;

import io.sqm.core.Join;
import io.sqm.core.SelectItem;
import io.sqm.core.TableRef;
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
 * Parses MySQL {@code DELETE} statements, including canonical {@code DELETE FROM ... USING ... JOIN ...} forms.
 */
public class MySqlDeleteStatementParser extends io.sqm.parser.ansi.DeleteStatementParser {

    /**
     * Creates a MySQL delete parser.
     */
    public MySqlDeleteStatementParser() {
    }

    /**
     * Parses tokens that may appear after {@code DELETE} and before {@code FROM}.
     *
     * @param cur token cursor
     * @param ctx parse context
     * @return parsed optimizer hints or an empty list when omitted
     */
    @Override
    protected ParseResult<List<String>> parseAfterDeleteKeyword(Cursor cur, ParseContext ctx) {
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
     * Parses optional MySQL {@code USING} sources.
     *
     * @param cur token cursor
     * @param ctx parse context
     * @return parsed USING sources or empty list when omitted
     */
    @Override
    protected ParseResult<List<TableRef>> parseUsing(Cursor cur, ParseContext ctx) {
        if (!cur.consumeIf(TokenType.USING)) {
            return ok(List.of());
        }
        if (!ctx.capabilities().supports(SqlFeature.DELETE_USING_JOIN)) {
            return error("DELETE ... USING is not supported by this dialect", cur.fullPos());
        }
        var usingResult = parseItems(TableRef.class, cur, ctx);
        if (usingResult.isError()) {
            return error(usingResult);
        }
        return ok(List.copyOf(usingResult.value()));
    }

    /**
     * Parses optional joined sources attached to the {@code USING} clause.
     *
     * @param cur      token cursor
     * @param ctx      parse context
     * @param hasUsing whether a {@code USING} clause was already parsed
     * @return parsed joins or empty list when omitted
     */
    @Override
    protected ParseResult<List<Join>> parseJoins(Cursor cur, ParseContext ctx, boolean hasUsing) {
        if (!cur.matchAny(Indicators.JOIN)) {
            return ok(List.of());
        }
        if (!hasUsing) {
            return error("DELETE ... JOIN requires USING in this dialect", cur.fullPos());
        }
        if (!ctx.capabilities().supports(SqlFeature.DELETE_USING_JOIN)) {
            return error("DELETE ... USING ... JOIN is not supported by this dialect", cur.fullPos());
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
    protected ParseResult<List<SelectItem>> parseReturning(Cursor cur, ParseContext ctx) {
        if (!cur.consumeIf(TokenType.RETURNING)) {
            return ok(List.of());
        }
        if (!ctx.capabilities().supports(SqlFeature.DML_RETURNING)) {
            return error("DELETE ... RETURNING is not supported by this dialect", cur.fullPos());
        }
        var returningResult = parseItems(SelectItem.class, cur, ctx);
        if (returningResult.isError()) {
            return error(returningResult);
        }
        return ok(List.copyOf(returningResult.value()));
    }
}
