package io.sqm.parser.ansi;

import io.sqm.core.DeleteStatement;
import io.sqm.core.Join;
import io.sqm.core.Predicate;
import io.sqm.core.SelectItem;
import io.sqm.core.Table;
import io.sqm.core.TableRef;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;
import io.sqm.parser.spi.Parser;

import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses baseline ANSI {@code DELETE} statements.
 */
public class DeleteStatementParser implements Parser<DeleteStatement> {

    /**
     * Creates a delete-statement parser.
     */
    public DeleteStatementParser() {
    }

    /**
     * Parses the spec represented by the {@link Cursor} instance.
     *
     * @param cur a Cursor instance that contains a list of tokens representing the spec to be parsed.
     * @param ctx a parser context containing parsers and lookups.
     * @return a parsing result.
     */
    @Override
    public ParseResult<? extends DeleteStatement> parse(Cursor cur, ParseContext ctx) {
        cur.expect("Expected DELETE", TokenType.DELETE);

        var optimizerHintsResult = parseAfterDeleteKeyword(cur, ctx);
        if (optimizerHintsResult.isError()) {
            return error(optimizerHintsResult);
        }

        cur.expect("Expected FROM after DELETE", TokenType.FROM);

        var tableResult = ctx.parse(Table.class, cur);
        if (tableResult.isError()) {
            return error(tableResult);
        }

        var usingResult = parseUsing(cur, ctx);
        if (usingResult.isError()) {
            return error(usingResult);
        }

        var joinsResult = parseJoins(cur, ctx, !usingResult.value().isEmpty());
        if (joinsResult.isError()) {
            return error(joinsResult);
        }

        Predicate where = null;

        if (cur.consumeIf(TokenType.WHERE)) {
            var whereResult = ctx.parse(Predicate.class, cur);
            if (whereResult.isError()) {
                return error(whereResult);
            }
            where = whereResult.value();
        }

        var returningResult = parseReturning(cur, ctx);
        if (returningResult.isError()) {
            return error(returningResult);
        }

        return ok(DeleteStatement.of(
            tableResult.value(),
            usingResult.value(),
            joinsResult.value(),
            where,
            null,
            returningResult.value(),
            optimizerHintsResult.value()
        ));
    }

    /**
     * Parses tokens that may appear after {@code DELETE} and before {@code FROM}.
     *
     * @param cur token cursor
     * @param ctx parse context
     * @return parsed optimizer hints or an empty list when omitted
     */
    protected ParseResult<List<String>> parseAfterDeleteKeyword(Cursor cur, ParseContext ctx) {
        return ok(List.of());
    }

    /**
     * Parses optional {@code USING} sources.
     *
     * @param cur token cursor
     * @param ctx parse context
     * @return parsed USING sources or empty list when omitted
     */
    protected ParseResult<List<TableRef>> parseUsing(Cursor cur, ParseContext ctx) {
        if (cur.match(TokenType.USING)) {
            return error("DELETE ... USING is not supported by this dialect", cur.fullPos());
        }
        return ok(List.of());
    }

    /**
     * Parses optional joined sources attached to the {@code USING} clause.
     *
     * @param cur token cursor
     * @param ctx parse context
     * @param hasUsing whether a {@code USING} clause was already parsed
     * @return parsed joins or empty list when omitted
     */
    protected ParseResult<List<Join>> parseJoins(Cursor cur, ParseContext ctx, boolean hasUsing) {
        if (cur.matchAny(Indicators.JOIN)) {
            return error("DELETE ... JOIN is not supported by this dialect", cur.fullPos());
        }
        return ok(List.of());
    }

    /**
     * Parses optional {@code RETURNING} projection items.
     *
     * @param cur token cursor
     * @param ctx parse context
     * @return parsed RETURNING items or empty list when omitted
     */
    protected ParseResult<List<SelectItem>> parseReturning(Cursor cur, ParseContext ctx) {
        if (cur.match(TokenType.RETURNING)) {
            return error("DELETE ... RETURNING is not supported by this dialect", cur.fullPos());
        }
        return ok(List.of());
    }

    /**
     * Gets the target type this handler can handle.
     *
     * @return an entity type to be handled by the handler.
     */
    @Override
    public Class<DeleteStatement> targetType() {
        return DeleteStatement.class;
    }
}
