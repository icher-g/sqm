package io.sqm.parser.postgresql;

import io.sqm.core.SelectItem;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses PostgreSQL {@code INSERT} statements, including optional {@code RETURNING}.
 */
public class InsertStatementParser extends io.sqm.parser.ansi.InsertStatementParser {

    /**
     * Creates a PostgreSQL insert-statement parser.
     */
    public InsertStatementParser() {
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
            return error("INSERT ... RETURNING is not supported by this dialect", cur.fullPos());
        }
        var returningResult = parseItems(SelectItem.class, cur, ctx);
        if (returningResult.isError()) {
            return error(returningResult);
        }
        return ok(List.copyOf(returningResult.value()));
    }
}
