package io.sqm.parser.postgresql;

import io.sqm.core.ResultClause;
import io.sqm.core.ResultItem;
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
 * Parses PostgreSQL {@code UPDATE} statements, including optional {@code FROM} sources.
 */
public class UpdateStatementParser extends io.sqm.parser.ansi.UpdateStatementParser {

    /**
     * Creates a PostgreSQL update-statement parser.
     */
    public UpdateStatementParser() {
    }

    /**
     * Parses optional PostgreSQL {@code FROM} sources.
     *
     * @param cur token cursor
     * @param ctx parse context
     * @return from source list, or empty list when omitted
     */
    @Override
    protected ParseResult<List<TableRef>> parseFrom(Cursor cur, ParseContext ctx) {
        if (!cur.consumeIf(TokenType.FROM)) {
            return ok(List.of());
        }

        if (!ctx.capabilities().supports(SqlFeature.UPDATE_FROM)) {
            return error("UPDATE ... FROM is not supported by this dialect", cur.fullPos());
        }

        var fromResult = parseItems(TableRef.class, cur, ctx);
        if (fromResult.isError()) {
            return error(fromResult);
        }
        return ok(List.copyOf(fromResult.value()));
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
            return error("UPDATE ... RETURNING is not supported by this dialect", cur.fullPos());
        }

        var returningResult = parseItems(ResultItem.class, cur, ctx);
        if (returningResult.isError()) {
            return error(returningResult);
        }
        return ok(ResultClause.of(returningResult.value()));
    }
}
