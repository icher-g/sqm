package io.sqm.parser.postgresql;

import io.sqm.core.*;
import io.sqm.core.dialect.SqlFeature;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import java.util.List;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * Parses PostgreSQL CTE definitions including writable CTE bodies.
 */
public class CteDefParser extends io.sqm.parser.ansi.CteDefParser {

    /**
     * Creates a PostgreSQL CTE parser.
     */
    public CteDefParser() {
    }

    private static boolean isEmpty(ResultClause result) {
        return result == null || result.items().isEmpty();
    }

    /**
     * Parses CTE body as a generic statement for PostgreSQL.
     *
     * @param cur token cursor positioned at CTE body start.
     * @param ctx parse context.
     * @return parsed statement body.
     */
    @Override
    protected ParseResult<? extends Statement> parseBody(Cursor cur, ParseContext ctx) {
        return ctx.parse(Statement.class, cur);
    }

    /**
     * Creates PostgreSQL CTE node and validates writable CTE constraints.
     *
     * @param name            CTE name.
     * @param body            parsed body statement.
     * @param aliases         column aliases.
     * @param materialization materialization hint.
     * @param ctx             parse context.
     * @param cur             token cursor.
     * @return parsed CTE definition.
     */
    @Override
    protected ParseResult<CteDef> createCte(Identifier name,
        Statement body,
        List<Identifier> aliases,
        CteDef.Materialization materialization,
        ParseContext ctx,
        Cursor cur) {
        if (body instanceof Query query) {
            return ok(Query.cte(name, query, aliases, materialization));
        }

        if (!ctx.capabilities().supports(SqlFeature.DML_RESULT_CLAUSE)) {
            return error("Writable CTE DML RETURNING is not supported by this dialect", cur.fullPos());
        }

        if (body instanceof InsertStatement insert) {
            if (isEmpty(insert.result())) {
                return error("Writable CTE INSERT requires RETURNING", cur.fullPos());
            }
            return ok(Query.cte(name, insert, aliases, materialization));
        }

        if (body instanceof UpdateStatement update) {
            if (isEmpty(update.result())) {
                return error("Writable CTE UPDATE requires RETURNING", cur.fullPos());
            }
            return ok(Query.cte(name, update, aliases, materialization));
        }

        if (body instanceof DeleteStatement delete) {
            if (isEmpty(delete.result())) {
                return error("Writable CTE DELETE requires RETURNING", cur.fullPos());
            }
            return ok(Query.cte(name, delete, aliases, materialization));
        }

        return error("Writable CTE supports only INSERT, UPDATE, or DELETE with RETURNING in this dialect", cur.fullPos());
    }
}
