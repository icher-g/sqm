package io.sqm.parser.mysql;

import io.sqm.core.Assignment;
import io.sqm.core.InsertStatement.InsertMode;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses MySQL {@code INSERT} statements, including {@code INSERT IGNORE},
 * {@code REPLACE INTO}, and {@code ON DUPLICATE KEY UPDATE}.
 */
public class MySqlInsertStatementParser extends io.sqm.parser.ansi.InsertStatementParser {

    /**
     * Creates a MySQL insert parser.
     */
    public MySqlInsertStatementParser() {
    }

    /**
     * Parses the leading MySQL insert mode.
     *
     * @param cur token cursor
     * @param ctx parse context
     * @return parsed insert mode
     */
    @Override
    protected ParseResult<InsertMode> parseInsertMode(Cursor cur, ParseContext ctx) {
        if (cur.consumeIf(TokenType.REPLACE)) {
            if (!ctx.capabilities().supports(SqlFeature.REPLACE_INTO)) {
                return error("REPLACE INTO is not supported by this dialect", cur.fullPos());
            }
            return ok(InsertMode.REPLACE);
        }
        if (!cur.consumeIf(TokenType.INSERT)) {
            return error("Expected INSERT or REPLACE", cur.fullPos());
        }
        if (!cur.consumeIf(TokenType.IGNORE)) {
            return ok(InsertMode.STANDARD);
        }
        if (!ctx.capabilities().supports(SqlFeature.INSERT_IGNORE)) {
            return error("INSERT IGNORE is not supported by this dialect", cur.fullPos());
        }
        return ok(InsertMode.IGNORE);
    }

    /**
     * Parses optional MySQL {@code ON DUPLICATE KEY UPDATE} clause.
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
        if (cur.match(TokenType.CONFLICT)) {
            return error("INSERT ... ON CONFLICT is not supported by this dialect", cur.fullPos());
        }
        if (!cur.consumeIf(TokenType.DUPLICATE)) {
            return error("Expected DUPLICATE after ON", cur.fullPos());
        }
        if (!ctx.capabilities().supports(SqlFeature.INSERT_ON_DUPLICATE_KEY_UPDATE)) {
            return error("INSERT ... ON DUPLICATE KEY UPDATE is not supported by this dialect", cur.fullPos());
        }
        cur.expect("Expected KEY after DUPLICATE", TokenType.KEY);
        cur.expect("Expected UPDATE after DUPLICATE KEY", TokenType.UPDATE);

        var assignments = parseItems(Assignment.class, cur, ctx);
        if (assignments.isError()) {
            return error(assignments);
        }
        return ok(onConflictDoUpdateClause(List.of(), assignments.value(), null));
    }
}
