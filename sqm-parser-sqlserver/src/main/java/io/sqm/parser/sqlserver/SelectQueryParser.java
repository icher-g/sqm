package io.sqm.parser.sqlserver;

import io.sqm.core.SelectQueryBuilder;
import io.sqm.core.TopSpec;
import io.sqm.parser.core.Cursor;
import io.sqm.parser.core.TokenType;
import io.sqm.parser.spi.ParseContext;
import io.sqm.parser.spi.ParseResult;

import static io.sqm.parser.spi.ParseResult.error;
import static io.sqm.parser.spi.ParseResult.ok;

/**
 * SQL Server SELECT parser with support for DISTINCT + TOP ordering and
 * SQL Server pagination constraints.
 */
public class SelectQueryParser extends io.sqm.parser.ansi.SelectQueryParser {

    /**
     * Creates a SQL Server SELECT parser.
     */
    public SelectQueryParser() {
    }

    /**
     * Parses SQL Server tokens that may appear after DISTINCT and before the projection list.
     *
     * @param cur token cursor.
     * @param ctx parse context.
     * @param q   mutable query builder.
     * @return parsing result.
     */
    @Override
    protected ParseResult<Void> parseAfterDistinctClause(Cursor cur, ParseContext ctx, SelectQueryBuilder q) {
        var top = parseTopClause(cur, ctx);
        if (top.isError()) {
            return error(top);
        }

        if (top.value() != null) {
            q.top(top.value());
        }
        return ok(null);
    }

    /**
     * Validates SQL Server pagination rules that span multiple SELECT clauses.
     *
     * @param cur token cursor.
     * @param ctx parse context.
     * @param q   mutable query builder.
     * @return parsing result.
     */
    @Override
    protected ParseResult<Void> validateAfterPagination(Cursor cur, ParseContext ctx, SelectQueryBuilder q) {
        var limitOffset = q.currentLimitOffset();
        if (limitOffset == null || (limitOffset.limit() == null && limitOffset.offset() == null && !limitOffset.limitAll())) {
            return ok(null);
        }

        if (q.currentTopSpec() != null) {
            return error("Cannot combine TOP with OFFSET/FETCH in SQL Server baseline support", cur.fullPos());
        }

        if (q.currentOrderBy() == null) {
            return error("OFFSET/FETCH requires ORDER BY in SQL Server", cur.fullPos());
        }

        return ok(null);
    }

    /**
     * Validates completed SQL Server SELECT query state.
     *
     * @param cur token cursor.
     * @param ctx parse context.
     * @param q   mutable query builder.
     * @return parsing result.
     */
    @Override
    protected ParseResult<Void> validateCompletedQuery(Cursor cur, ParseContext ctx, SelectQueryBuilder q) {
        var topSpec = q.currentTopSpec();
        if (topSpec != null && topSpec.withTies() && q.currentOrderBy() == null) {
            return error("TOP WITH TIES requires ORDER BY in SQL Server", cur.fullPos());
        }
        return ok(null);
    }

    /**
     * Checks whether the current cursor position starts a SQL Server join,
     * including bare {@code OUTER APPLY}.
     *
     * @param cur token cursor.
     * @return {@code true} when the cursor is positioned at a SQL Server join-start token.
     */
    @Override
    protected boolean isJoinStart(Cursor cur) {
        return super.isJoinStart(cur) || cur.match(TokenType.OUTER);
    }

    private ParseResult<TopSpec> parseTopClause(Cursor cur, ParseContext ctx) {
        return SqlServerTopSpecParserSupport.parseTopClause(cur, ctx, true, "TOP WITH TIES requires ORDER BY in SQL Server");
    }
}
