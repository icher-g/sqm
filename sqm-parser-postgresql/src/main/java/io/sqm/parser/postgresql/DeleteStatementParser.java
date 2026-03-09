package io.sqm.parser.postgresql;

import io.sqm.core.SelectItem;
import io.sqm.core.TableRef;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses PostgreSQL {@code DELETE} statements, including optional {@code USING} sources.
 */
public class DeleteStatementParser extends io.sqm.parser.ansi.DeleteStatementParser {

    /**
     * Creates a PostgreSQL delete-statement parser.
     */
    public DeleteStatementParser() {
    }

    /**
     * Parses optional PostgreSQL {@code USING} sources.
     *
     * @param cur token cursor
     * @param ctx parse context
     * @return using source list, or empty list when omitted
     */
    @Override
    protected ParseResult<List<TableRef>> parseUsing(Cursor cur, ParseContext ctx) {
        if (!cur.consumeIf(TokenType.USING)) {
            return ok(List.of());
        }

        if (!ctx.capabilities().supports(SqlFeature.DELETE_USING)) {
            return error("DELETE ... USING is not supported by this dialect", cur.fullPos());
        }

        var usingResult = parseItems(TableRef.class, cur, ctx);
        if (usingResult.isError()) {
            return error(usingResult);
        }
        return ok(List.copyOf(usingResult.value()));
    }

    /**
     * Parses optional PostgreSQL {@code RETURNING} clause.
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
